package com.smartmail.data.remote

import com.smartmail.domain.models.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.*

class SmtpClient(private val account: Account) {

    /**
     * Invia un'email.
     *
     * @param to Lista destinatari principali
     * @param cc Lista destinatari in copia
     * @param bcc Lista destinatari in copia nascosta
     * @param subject Oggetto dell'email
     * @param bodyText Corpo in testo semplice
     * @param bodyHtml Corpo in HTML (opzionale, ha priorità)
     * @param attachments Lista di file allegati
     * @param replyToMessageId Message-ID dell'email a cui si risponde (opzionale)
     */
    suspend fun sendEmail(
        to: List<String>,
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList(),
        subject: String,
        bodyText: String,
        bodyHtml: String? = null,
        attachments: List<File> = emptyList(),
        replyToMessageId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val props = buildProperties()
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(account.emailAddress, account.encryptedPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(account.emailAddress, account.displayName))

                // Destinatari
                to.forEach { addRecipient(Message.RecipientType.TO, InternetAddress(it)) }
                cc.forEach { addRecipient(Message.RecipientType.CC, InternetAddress(it)) }
                bcc.forEach { addRecipient(Message.RecipientType.BCC, InternetAddress(it)) }

                this.subject = subject

                // Header di risposta
                if (replyToMessageId != null) {
                    setHeader("In-Reply-To", replyToMessageId)
                    setHeader("References", replyToMessageId)
                }

                // Corpo
                if (attachments.isEmpty() && bodyHtml == null) {
                    // Solo testo semplice
                    setText(bodyText, "UTF-8")
                } else {
                    // Multipart
                    val multipart = MimeMultipart("mixed")

                    // Parte corpo
                    val bodyPart = MimeBodyPart()
                    if (bodyHtml != null) {
                        // Corpo con alternative text/html
                        val alternative = MimeMultipart("alternative")

                        val textPart = MimeBodyPart()
                        textPart.setText(bodyText, "UTF-8", "plain")
                        alternative.addBodyPart(textPart)

                        val htmlPart = MimeBodyPart()
                        htmlPart.setContent(bodyHtml, "text/html; charset=UTF-8")
                        alternative.addBodyPart(htmlPart)

                        bodyPart.setContent(alternative)
                    } else {
                        bodyPart.setText(bodyText, "UTF-8")
                    }
                    multipart.addBodyPart(bodyPart)

                    // Allegati
                    attachments.forEach { file ->
                        val attachPart = MimeBodyPart()
                        val source = FileDataSource(file)
                        attachPart.dataHandler = DataHandler(source)
                        attachPart.fileName = MimeUtility.encodeText(file.name)
                        multipart.addBodyPart(attachPart)
                    }

                    setContent(multipart)
                }

                saveChanges()
            }

            Transport.send(message)

            val sentMessageId = message.messageID ?: "<${System.nanoTime()}@promail>"
            Result.success(sentMessageId)

        } catch (e: AuthenticationFailedException) {
            Result.failure(SmtpException.AuthError("Autenticazione SMTP fallita: ${e.message}"))
        } catch (e: SendFailedException) {
            Result.failure(SmtpException.SendError("Invio fallito: ${e.message}"))
        } catch (e: MessagingException) {
            Result.failure(SmtpException.ConnectionError("Errore connessione SMTP: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(SmtpException.GenericError("Errore: ${e.message}"))
        }
    }

    /**
     * Verifica la connessione SMTP (utile per validare le credenziali).
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val props = buildProperties()
            val session = Session.getInstance(props)
            val transport = session.getTransport(if (account.smtpUseSsl && account.smtpPort == 465) "smtps" else "smtp")
            transport.connect(account.smtpHost, account.smtpPort, account.emailAddress, account.encryptedPassword)
            transport.close()
            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            Result.failure(SmtpException.AuthError("Credenziali non valide"))
        } catch (e: Exception) {
            Result.failure(SmtpException.ConnectionError("Impossibile connettersi: ${e.message}"))
        }
    }

    private fun buildProperties(): Properties = Properties().apply {
        put("mail.smtp.host", account.smtpHost)
        put("mail.smtp.port", account.smtpPort.toString())
        put("mail.smtp.auth", "true")
        put("mail.smtp.connectiontimeout", "15000")
        put("mail.smtp.timeout", "30000")

        if (account.smtpUseSsl) {
            if (account.smtpPort == 465) {
                // SSL diretto (Virgilio usa porta 465)
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            } else {
                // STARTTLS (Gmail usa porta 587)
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }
        }
    }
}

/**
 * Eccezioni specifiche per SMTP.
 */
sealed class SmtpException(message: String) : Exception(message) {
    class AuthError(message: String) : SmtpException(message)
    class ConnectionError(message: String) : SmtpException(message)
    class SendError(message: String) : SmtpException(message)
    class GenericError(message: String) : SmtpException(message)
}
