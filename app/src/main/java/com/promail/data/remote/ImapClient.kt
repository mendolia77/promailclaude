package com.smartmail.data.remote

import com.smartmail.domain.models.Account
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.EmailAttachment
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMultipart
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm

class ImapClient(private val account: Account) {

    private var store: Store? = null
    private val gson = Gson()

    /**
     * Connette al server IMAP dell'account.
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.store.protocol", if (account.imapUseSsl) "imaps" else "imap")
                put("mail.imap.host", account.imapHost)
                put("mail.imap.port", account.imapPort.toString())
                put("mail.imap.ssl.enable", account.imapUseSsl.toString())
                put("mail.imap.starttls.enable", "true")
                // Timeout di connessione
                put("mail.imap.connectiontimeout", "15000")
                put("mail.imap.timeout", "30000")
                // Per Gmail: supporto OAuth2 (futuro) e IMAP IDLE
                put("mail.imap.auth.mechanisms", "XOAUTH2 PLAIN LOGIN")
            }

            val session = Session.getInstance(props)
            store = session.getStore(if (account.imapUseSsl) "imaps" else "imap")
            store?.connect(account.imapHost, account.imapPort, account.emailAddress, account.encryptedPassword)
            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            Result.failure(ImapException.AuthError("Autenticazione fallita: ${e.message}"))
        } catch (e: MessagingException) {
            Result.failure(ImapException.ConnectionError("Connessione fallita: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore: ${e.message}"))
        }
    }

    /**
     * Disconnette dal server IMAP.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            store?.close()
        } catch (_: Exception) {
        } finally {
            store = null
        }
    }

    /**
     * Verifica se è connesso.
     */
    fun isConnected(): Boolean = store?.isConnected == true

    /**
     * Recupera la lista delle cartelle IMAP.
     */
    suspend fun getFolderNames(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val store = requireConnectedStore()
            val defaultFolder = store.defaultFolder
            val folders = defaultFolder.list("*")
            val names = folders
                .filter { (it.type and Folder.HOLDS_MESSAGES) != 0 }
                .map { it.fullName }
            Result.success(names)
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Impossibile ottenere cartelle: ${e.message}"))
        }
    }

    /**
     * Recupera le email da una cartella.
     * @param folderName Nome cartella IMAP (es. "INBOX")
     * @param limit Numero massimo di email da recuperare
     * @param sinceDate Recupera solo email dopo questa data (per sync incrementale)
     */
    suspend fun fetchEmails(
        folderName: String = "INBOX",
        limit: Int = 50,
        sinceDate: Date? = null
    ): Result<List<Email>> = withContext(Dispatchers.IO) {
        var folder: Folder? = null
        try {
            val store = requireConnectedStore()
            folder = store.getFolder(folderName)
            folder.open(Folder.READ_ONLY)

            val messages = if (sinceDate != null) {
                val searchTerm = ReceivedDateTerm(ComparisonTerm.GE, sinceDate)
                folder.search(searchTerm)
            } else {
                val totalMessages = folder.messageCount
                val start = maxOf(1, totalMessages - limit + 1)
                folder.getMessages(start, totalMessages)
            }

            // Prefetch per performance: scarica headers e flags in batch
            val fetchProfile = FetchProfile().apply {
                add(FetchProfile.Item.ENVELOPE)
                add(FetchProfile.Item.FLAGS)
                add(FetchProfile.Item.CONTENT_INFO)
            }
            folder.fetch(messages, fetchProfile)

            val emails = messages.mapNotNull { message ->
                try {
                    convertToEmail(message, folderName)
                } catch (_: Exception) {
                    null // Salta email con problemi di parsing
                }
            }.sortedByDescending { it.receivedDate }

            Result.success(emails.take(limit))
        } catch (e: Exception) {
            Result.failure(ImapException.FetchError("Errore nel recupero email: ${e.message}"))
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Recupera il corpo completo di una singola email.
     */
    suspend fun fetchEmailBody(
        folderName: String,
        messageId: String
    ): Result<Pair<String?, String?>> = withContext(Dispatchers.IO) {
        var folder: Folder? = null
        try {
            val store = requireConnectedStore()
            folder = store.getFolder(folderName)
            folder.open(Folder.READ_ONLY)

            val messages = folder.messages
            val target = messages.firstOrNull { msg ->
                msg.getHeader("Message-ID")?.firstOrNull() == messageId
            } ?: return@withContext Result.failure(
                ImapException.GenericError("Email non trovata")
            )

            val (text, html) = extractBody(target)
            Result.success(Pair(text, html))
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore: ${e.message}"))
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Segna un'email come letta/non letta sul server.
     */
    suspend fun setFlag(
        folderName: String,
        messageId: String,
        flag: Flags.Flag,
        value: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var folder: Folder? = null
        try {
            val store = requireConnectedStore()
            folder = store.getFolder(folderName)
            folder.open(Folder.READ_WRITE)

            val messages = folder.messages
            val target = messages.firstOrNull { msg ->
                msg.getHeader("Message-ID")?.firstOrNull() == messageId
            } ?: return@withContext Result.failure(
                ImapException.GenericError("Email non trovata")
            )

            target.setFlag(flag, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore flag: ${e.message}"))
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Sposta un'email in un'altra cartella (es. Cestino).
     */
    suspend fun moveEmail(
        fromFolder: String,
        toFolder: String,
        messageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var srcFolder: Folder? = null
        try {
            val store = requireConnectedStore()
            srcFolder = store.getFolder(fromFolder)
            srcFolder.open(Folder.READ_WRITE)

            val destFolder = store.getFolder(toFolder)

            val target = srcFolder.messages.firstOrNull { msg ->
                msg.getHeader("Message-ID")?.firstOrNull() == messageId
            } ?: return@withContext Result.failure(
                ImapException.GenericError("Email non trovata")
            )

            srcFolder.copyMessages(arrayOf(target), destFolder)
            target.setFlag(Flags.Flag.DELETED, true)
            srcFolder.expunge()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore spostamento: ${e.message}"))
        } finally {
            try {
                srcFolder?.close(false)
            } catch (_: Exception) {
            }
        }
    }

    // ==========================================
    // METODI PRIVATI
    // ==========================================

    private fun requireConnectedStore(): Store {
        return store?.takeIf { it.isConnected }
            ?: throw ImapException.ConnectionError("Non connesso al server IMAP")
    }

    /**
     * Converte un javax.mail.Message in un Email del dominio.
     */
    private fun convertToEmail(message: Message, folderName: String): Email {
        val from = message.from?.firstOrNull() as? InternetAddress
        val toList = extractAddresses(message.getRecipients(Message.RecipientType.TO))
        val ccList = extractAddresses(message.getRecipients(Message.RecipientType.CC))
        val bccList = extractAddresses(message.getRecipients(Message.RecipientType.BCC))

        val msgId = message.getHeader("Message-ID")?.firstOrNull() ?: "<${System.nanoTime()}@promail>"
        val threadId = message.getHeader("In-Reply-To")?.firstOrNull()
            ?: message.getHeader("References")?.firstOrNull()?.split("\\s+".toRegex())?.firstOrNull()

        // Controlla allegati senza scaricarli
        val attachments = mutableListOf<EmailAttachment>()
        val hasAttachments = checkAttachments(message, attachments)

        val flags = message.flags
        val (bodyText, bodyHtml) = extractBody(message)

        return Email(
            messageId = msgId,
            accountId = account.id,
            fromAddress = from?.address ?: "",
            fromName = from?.personal,
            toAddresses = gson.toJson(toList),
            ccAddresses = if (ccList.isNotEmpty()) gson.toJson(ccList) else null,
            bccAddresses = if (bccList.isNotEmpty()) gson.toJson(bccList) else null,
            subject = message.subject ?: "(Nessun oggetto)",
            bodyText = bodyText,
            bodyHtml = bodyHtml,
            receivedDate = message.receivedDate ?: message.sentDate ?: Date(),
            sentDate = message.sentDate,
            size = message.size,
            isRead = flags.contains(Flags.Flag.SEEN),
            isStarred = flags.contains(Flags.Flag.FLAGGED),
            isImportant = flags.contains(Flags.Flag.FLAGGED),
            isDraft = flags.contains(Flags.Flag.DRAFT),
            folderName = folderName,
            hasAttachments = hasAttachments,
            attachmentsJson = if (attachments.isNotEmpty()) gson.toJson(attachments) else null,
            threadId = threadId
        )
    }

    /**
     * Estrae gli indirizzi email in formato stringa.
     */
    private fun extractAddresses(addresses: Array<Address>?): List<String> {
        return addresses?.mapNotNull { addr ->
            (addr as? InternetAddress)?.address
        } ?: emptyList()
    }

    /**
     * Estrae il corpo dell'email (testo e HTML).
     */
    private fun extractBody(message: Message): Pair<String?, String?> {
        return try {
            when (val content = message.content) {
                is String -> {
                    if (message.isMimeType("text/html")) {
                        Pair(null, content)
                    } else {
                        Pair(content, null)
                    }
                }
                is MimeMultipart -> extractFromMultipart(content)
                else -> Pair(null, null)
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    /**
     * Estrae testo e HTML da un multipart.
     */
    private fun extractFromMultipart(multipart: MimeMultipart): Pair<String?, String?> {
        var text: String? = null
        var html: String? = null

        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            when {
                part.isMimeType("text/plain") && text == null -> {
                    text = part.content as? String
                }
                part.isMimeType("text/html") && html == null -> {
                    html = part.content as? String
                }
                part.content is MimeMultipart -> {
                    val (innerText, innerHtml) = extractFromMultipart(part.content as MimeMultipart)
                    if (text == null) text = innerText
                    if (html == null) html = innerHtml
                }
            }
        }
        return Pair(text, html)
    }

    /**
     * Controlla se l'email ha allegati e raccoglie i metadati.
     */
    private fun checkAttachments(
        message: Message,
        attachments: MutableList<EmailAttachment>
    ): Boolean {
        return try {
            val content = message.content
            if (content is MimeMultipart) {
                scanMultipartForAttachments(content, attachments)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun scanMultipartForAttachments(
        multipart: MimeMultipart,
        attachments: MutableList<EmailAttachment>
    ): Boolean {
        var found = false
        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            val disposition = part.disposition
            if (disposition != null && (disposition.equals(Part.ATTACHMENT, ignoreCase = true)
                        || disposition.equals(Part.INLINE, ignoreCase = true))) {
                val filename = part.fileName ?: "allegato_${i}"
                attachments.add(
                    EmailAttachment(
                        filename = filename,
                        mimeType = part.contentType?.split(";")?.firstOrNull() ?: "application/octet-stream",
                        size = part.size
                    )
                )
                found = true
            } else if (part.content is MimeMultipart) {
                if (scanMultipartForAttachments(part.content as MimeMultipart, attachments)) {
                    found = true
                }
            }
        }
        return found
    }

    /**
     * Sposta un'email nel Trash (cartella Cestino) sul server.
     * @param messageId ID del messaggio da eliminare
     * @param currentFolder Nome della cartella corrente dove si trova il messaggio
     */
    suspend fun moveToTrash(messageId: String, currentFolder: String = "INBOX"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folder = store?.getFolder(currentFolder) as? Folder
                ?: return@withContext Result.failure(ImapException.GenericError("Store non connesso"))

            folder.open(Folder.READ_WRITE)

            try {
                // Cerca il messaggio per Message-ID
                val messages = folder.messages
                val targetMessage = messages.find { msg ->
                    try {
                        val msgId = msg.getHeader("Message-ID")?.firstOrNull()
                        msgId == messageId || msgId?.contains(messageId) == true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (targetMessage != null) {
                    // Prova diverse possibili cartelle Trash
                    val trashFolderNames = listOf("Trash", "[Gmail]/Trash", "Deleted", "Deleted Items", "Cestino")
                    var trashFolder: Folder? = null

                    for (trashName in trashFolderNames) {
                        try {
                            val candidate = store?.getFolder(trashName)
                            if (candidate?.exists() == true) {
                                trashFolder = candidate
                                break
                            }
                        } catch (e: Exception) {
                            // Continua con il prossimo nome
                        }
                    }

                    if (trashFolder != null) {
                        // Sposta nel Trash
                        folder.copyMessages(arrayOf(targetMessage), trashFolder)
                        targetMessage.setFlag(Flags.Flag.DELETED, true)
                        folder.expunge()
                        Result.success(Unit)
                    } else {
                        // Fallback: marca solo come DELETED
                        targetMessage.setFlag(Flags.Flag.DELETED, true)
                        folder.expunge()
                        Result.success(Unit)
                    }
                } else {
                    Result.failure(ImapException.GenericError("Messaggio non trovato"))
                }
            } finally {
                folder.close(true)
            }
        } catch (e: MessagingException) {
            Result.failure(ImapException.GenericError("Errore eliminazione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore: ${e.message}"))
        }
    }

    /**
     * Elimina definitivamente un'email dal server (non reversibile).
     * Usa questa funzione solo per il Trash.
     */
    suspend fun deleteEmailPermanently(messageId: String, folderName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folder = store?.getFolder(folderName) as? Folder
                ?: return@withContext Result.failure(ImapException.GenericError("Store non connesso"))

            folder.open(Folder.READ_WRITE)

            try {
                val messages = folder.messages
                val targetMessage = messages.find { msg ->
                    try {
                        val msgId = msg.getHeader("Message-ID")?.firstOrNull()
                        msgId == messageId || msgId?.contains(messageId) == true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (targetMessage != null) {
                    targetMessage.setFlag(Flags.Flag.DELETED, true)
                    folder.expunge()
                    Result.success(Unit)
                } else {
                    Result.failure(ImapException.GenericError("Messaggio non trovato"))
                }
            } finally {
                folder.close(true)
            }
        } catch (e: MessagingException) {
            Result.failure(ImapException.GenericError("Errore eliminazione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore: ${e.message}"))
        }
    }

    /**
     * Scarica un allegato specifico da un'email.
     * @param messageId ID del messaggio
     * @param folderName Nome della cartella IMAP
     * @param attachmentIndex Indice dell'allegato da scaricare
     * @return ByteArray con i dati dell'allegato
     */
    suspend fun downloadAttachment(
        messageId: String,
        folderName: String = "INBOX",
        attachmentIndex: Int
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        var folder: Folder? = null
        try {
            val store = requireConnectedStore()
            folder = store.getFolder(folderName)
            folder.open(Folder.READ_ONLY)

            val messages = folder.messages
            val targetMessage = messages.find { msg ->
                try {
                    val msgId = msg.getHeader("Message-ID")?.firstOrNull()
                    msgId == messageId || msgId?.contains(messageId) == true
                } catch (e: Exception) {
                    false
                }
            }

            if (targetMessage == null) {
                return@withContext Result.failure(ImapException.GenericError("Messaggio non trovato"))
            }

            val content = targetMessage.content
            if (content is MimeMultipart) {
                val attachmentData = extractAttachmentData(content, attachmentIndex)
                if (attachmentData != null) {
                    Result.success(attachmentData)
                } else {
                    Result.failure(ImapException.GenericError("Allegato non trovato"))
                }
            } else {
                Result.failure(ImapException.GenericError("Il messaggio non contiene allegati"))
            }
        } catch (e: MessagingException) {
            Result.failure(ImapException.FetchError("Errore download allegato: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ImapException.GenericError("Errore: ${e.message}"))
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Estrae i dati di un allegato specifico dal multipart.
     */
    private fun extractAttachmentData(
        multipart: MimeMultipart,
        targetIndex: Int
    ): ByteArray? {
        var currentIndex = 0
        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            val disposition = part.disposition
            if (disposition != null && (disposition.equals(Part.ATTACHMENT, ignoreCase = true)
                        || disposition.equals(Part.INLINE, ignoreCase = true))) {
                if (currentIndex == targetIndex) {
                    // Trovato l'allegato richiesto
                    return part.inputStream.readBytes()
                }
                currentIndex++
            } else if (part.content is MimeMultipart) {
                val data = extractAttachmentData(part.content as MimeMultipart, targetIndex - currentIndex)
                if (data != null) return data
                // Aggiorna currentIndex in base agli allegati trovati nel nested multipart
                currentIndex += countAttachments(part.content as MimeMultipart)
            }
        }
        return null
    }

    /**
     * Conta il numero di allegati in un multipart.
     */
    private fun countAttachments(multipart: MimeMultipart): Int {
        var count = 0
        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            val disposition = part.disposition
            if (disposition != null && (disposition.equals(Part.ATTACHMENT, ignoreCase = true)
                        || disposition.equals(Part.INLINE, ignoreCase = true))) {
                count++
            } else if (part.content is MimeMultipart) {
                count += countAttachments(part.content as MimeMultipart)
            }
        }
        return count
    }
}

/**
 * Eccezioni specifiche per IMAP.
 */
sealed class ImapException(message: String) : Exception(message) {
    class AuthError(message: String) : ImapException(message)
    class ConnectionError(message: String) : ImapException(message)
    class FetchError(message: String) : ImapException(message)
    class GenericError(message: String) : ImapException(message)
}
