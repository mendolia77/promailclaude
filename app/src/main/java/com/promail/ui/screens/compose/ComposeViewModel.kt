package com.smartmail.ui.screens.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.remote.SmtpClient
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Account
import com.smartmail.domain.models.Draft
import com.smartmail.domain.models.Email
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.util.Date

data class ComposeUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val attachments: List<File> = emptyList(),
    val showCcBcc: Boolean = false,
    val isSending: Boolean = false,
    val sendResult: SendResult? = null,
    // Per risposte
    val replyToMessageId: String? = null,
    val replyToEmail: Email? = null,
    // Bozze
    val currentDraftId: Long? = null,
    val isAutoSaving: Boolean = false,
    val lastSaved: Date? = null
)

sealed class SendResult {
    object Success : SendResult()
    data class Error(val message: String) : SendResult()
}

class ComposeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())
    private val draftDao = db.draftDao()
    private val gson = Gson()

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
        startAutoSave()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            repository.getActiveAccounts().collect { accounts ->
                val default = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                _uiState.update {
                    it.copy(accounts = accounts, selectedAccount = it.selectedAccount ?: default)
                }
            }
        }
    }

    fun selectAccount(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun updateTo(value: String) { _uiState.update { it.copy(to = value) } }
    fun updateCc(value: String) { _uiState.update { it.copy(cc = value) } }
    fun updateBcc(value: String) { _uiState.update { it.copy(bcc = value) } }
    fun updateSubject(value: String) { _uiState.update { it.copy(subject = value) } }
    fun updateBody(value: String) { _uiState.update { it.copy(body = value) } }
    fun toggleCcBcc() { _uiState.update { it.copy(showCcBcc = !it.showCcBcc) } }

    fun addAttachment(file: File) {
        _uiState.update { it.copy(attachments = it.attachments + file) }
    }

    fun removeAttachment(file: File) {
        _uiState.update { it.copy(attachments = it.attachments - file) }
    }

    fun clearSendResult() {
        _uiState.update { it.copy(sendResult = null) }
    }

    /**
     * Prepopola i campi per una risposta.
     */
    fun setupReply(emailId: Long) {
        viewModelScope.launch {
            val email = repository.getEmailById(emailId) ?: return@launch
            _uiState.update {
                it.copy(
                    to = email.fromAddress,
                    subject = if (email.subject.startsWith("Re:", ignoreCase = true))
                        email.subject else "Re: ${email.subject}",
                    body = "\n\n--- Messaggio originale ---\nDa: ${email.fromName ?: email.fromAddress}\nData: ${email.receivedDate}\n\n${email.bodyText ?: ""}",
                    replyToMessageId = email.messageId,
                    replyToEmail = email
                )
            }
        }
    }

    /**
     * Prepopola per un inoltro.
     */
    fun setupForward(emailId: Long) {
        viewModelScope.launch {
            val email = repository.getEmailById(emailId) ?: return@launch
            _uiState.update {
                it.copy(
                    subject = if (email.subject.startsWith("Fwd:", ignoreCase = true))
                        email.subject else "Fwd: ${email.subject}",
                    body = "\n\n--- Messaggio inoltrato ---\nDa: ${email.fromName ?: email.fromAddress}\nA: ${email.toAddresses}\nData: ${email.receivedDate}\nOggetto: ${email.subject}\n\n${email.bodyText ?: ""}"
                )
            }
        }
    }

    /**
     * Invia l'email.
     */
    fun send() {
        val state = _uiState.value
        val account = state.selectedAccount ?: return

        if (state.to.isBlank()) {
            _uiState.update { it.copy(sendResult = SendResult.Error("Inserisci almeno un destinatario")) }
            return
        }

        _uiState.update { it.copy(isSending = true, sendResult = null) }

        viewModelScope.launch {
            val smtpClient = SmtpClient(account)

            val toList = parseRecipients(state.to)
            val ccList = parseRecipients(state.cc)
            val bccList = parseRecipients(state.bcc)

            val result = smtpClient.sendEmail(
                to = toList,
                cc = ccList,
                bcc = bccList,
                subject = state.subject.ifBlank { "(Nessun oggetto)" },
                bodyText = state.body,
                attachments = state.attachments,
                replyToMessageId = state.replyToMessageId
            )

            result.fold(
                onSuccess = { messageId ->
                    // Salva nel database come email inviata
                    val sentEmail = Email(
                        messageId = messageId,
                        accountId = account.id,
                        fromAddress = account.emailAddress,
                        fromName = account.displayName,
                        toAddresses = gson.toJson(toList),
                        ccAddresses = if (ccList.isNotEmpty()) gson.toJson(ccList) else null,
                        bccAddresses = if (bccList.isNotEmpty()) gson.toJson(bccList) else null,
                        subject = state.subject.ifBlank { "(Nessun oggetto)" },
                        bodyText = state.body,
                        bodyHtml = null,
                        receivedDate = Date(),
                        sentDate = Date(),
                        size = state.body.toByteArray().size,
                        folderName = "SENT",
                        hasAttachments = state.attachments.isNotEmpty()
                    )
                    repository.saveEmail(sentEmail)

                    // Elimina la bozza se esisteva
                    state.currentDraftId?.let { draftId ->
                        launch { draftDao.deleteById(draftId) }
                    }

                    _uiState.update { it.copy(isSending = false, sendResult = SendResult.Success) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSending = false, sendResult = SendResult.Error(error.message ?: "Errore sconosciuto"))
                    }
                }
            )
        }
    }

    private fun parseRecipients(input: String): List<String> {
        return input.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains("@") }
    }

    // ==========================================
    // GESTIONE BOZZE
    // ==========================================

    /**
     * Avvia il salvataggio automatico ogni 30 secondi.
     */
    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // 30 secondi
                if (shouldSaveDraft()) {
                    saveDraft()
                }
            }
        }
    }

    private fun shouldSaveDraft(): Boolean {
        val state = _uiState.value
        return !state.isSending &&
               state.selectedAccount != null &&
               (state.to.isNotBlank() || state.subject.isNotBlank() || state.body.isNotBlank())
    }

    /**
     * Salva la bozza corrente.
     */
    fun saveDraft() {
        val state = _uiState.value
        val account = state.selectedAccount ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isAutoSaving = true) }

            try {
                val draft = Draft(
                    id = state.currentDraftId ?: 0,
                    accountId = account.id,
                    toAddresses = gson.toJson(parseRecipients(state.to)),
                    ccAddresses = if (state.cc.isNotBlank()) gson.toJson(parseRecipients(state.cc)) else null,
                    bccAddresses = if (state.bcc.isNotBlank()) gson.toJson(parseRecipients(state.bcc)) else null,
                    subject = state.subject,
                    bodyText = state.body,
                    bodyHtml = null,
                    createdDate = Date(),
                    lastModifiedDate = Date(),
                    inReplyToEmailId = state.replyToEmail?.id
                )

                val draftId = draftDao.insert(draft)
                _uiState.update {
                    it.copy(
                        currentDraftId = if (draft.id == 0L) draftId else draft.id,
                        isAutoSaving = false,
                        lastSaved = Date()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAutoSaving = false) }
            }
        }
    }

    /**
     * Carica una bozza esistente.
     */
    fun loadDraft(draftId: Long) {
        viewModelScope.launch {
            val draft = draftDao.getDraftById(draftId) ?: return@launch

            val toList = gson.fromJson(draft.toAddresses, Array<String>::class.java).toList()
            val ccList = draft.ccAddresses?.let { gson.fromJson(it, Array<String>::class.java).toList() } ?: emptyList()
            val bccList = draft.bccAddresses?.let { gson.fromJson(it, Array<String>::class.java).toList() } ?: emptyList()

            _uiState.update {
                it.copy(
                    currentDraftId = draft.id,
                    to = toList.joinToString(", "),
                    cc = ccList.joinToString(", "),
                    bcc = bccList.joinToString(", "),
                    subject = draft.subject,
                    body = draft.bodyText ?: "",
                    showCcBcc = ccList.isNotEmpty() || bccList.isNotEmpty(),
                    lastSaved = draft.lastModifiedDate
                )
            }
        }
    }

    /**
     * Elimina la bozza corrente.
     */
    fun deleteDraft() {
        val draftId = _uiState.value.currentDraftId ?: return
        viewModelScope.launch {
            draftDao.deleteById(draftId)
            _uiState.update { it.copy(currentDraftId = null) }
        }
    }

    /**
     * Elimina la bozza dopo l'invio.
     */
    override fun onCleared() {
        super.onCleared()
        // Se c'è una bozza salvata quando chiudi, mantienila
        // Se vuoi eliminarla automaticamente, puoi chiamare deleteDraft() qui
    }
}
