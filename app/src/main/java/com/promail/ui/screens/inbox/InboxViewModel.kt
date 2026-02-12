package com.smartmail.ui.screens.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InboxUiState(
    val smartFolders: List<SmartFolder> = emptyList(),
    val emails: List<Email> = emptyList(),
    val unreadCount: Int = 0,
    val starredCount: Int = 0,
    val folderUnreadCounts: Map<Long, Int> = emptyMap(),
    val accountCount: Int = 0,
    val isLoading: Boolean = true,
    // Cartella selezionata (null = inbox unificata)
    val selectedFolder: SmartFolder? = null,
    val selectedFolderName: String = "Posta in arrivo"
)

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadSmartFolders()
        loadUnifiedInbox()
        loadAccountCount()
        loadUnreadCount()
        loadStarredCount()
    }

    private fun loadSmartFolders() {
        viewModelScope.launch {
            repository.getActiveSmartFolders().collect { folders ->
                _uiState.update { it.copy(smartFolders = folders) }
                // Carica contatori non lette per ogni folder
                folders.forEach { folder ->
                    launch {
                        repository.getUnreadCountBySmartFolder(folder.id).collect { count ->
                            _uiState.update { state ->
                                state.copy(folderUnreadCounts = state.folderUnreadCounts + (folder.id to count))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadUnifiedInbox() {
        viewModelScope.launch {
            repository.getUnifiedInbox().collect { emails ->
                _uiState.update { it.copy(emails = emails, isLoading = false) }
            }
        }
    }

    private fun loadAccountCount() {
        viewModelScope.launch {
            repository.getAccountCount().collect { count ->
                _uiState.update { it.copy(accountCount = count) }
            }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            repository.getTotalUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    private fun loadStarredCount() {
        viewModelScope.launch {
            repository.getStarredCount().collect { count ->
                _uiState.update { it.copy(starredCount = count) }
            }
        }
    }

    fun selectFolder(folder: SmartFolder) {
        _uiState.update { it.copy(selectedFolder = folder, selectedFolderName = folder.name, isLoading = true) }
        viewModelScope.launch {
            repository.getEmailsBySmartFolder(folder.id).collect { emails ->
                _uiState.update { it.copy(emails = emails, isLoading = false) }
            }
        }
    }

    fun selectInbox() {
        _uiState.update { it.copy(selectedFolder = null, selectedFolderName = "Posta in arrivo", isLoading = true) }
        viewModelScope.launch {
            repository.getUnifiedInbox().collect { emails ->
                _uiState.update { it.copy(emails = emails, isLoading = false) }
            }
        }
    }

    fun selectStarred() {
        _uiState.update { it.copy(selectedFolder = null, selectedFolderName = "Importanti", isLoading = true) }
        viewModelScope.launch {
            repository.getStarredEmails().collect { emails ->
                _uiState.update { it.copy(emails = emails, isLoading = false) }
            }
        }
    }

    fun selectAttachments() {
        _uiState.update { it.copy(selectedFolder = null, selectedFolderName = "Con allegati", isLoading = true) }
        viewModelScope.launch {
            repository.getEmailsWithAttachments().collect { emails ->
                _uiState.update { it.copy(emails = emails, isLoading = false) }
            }
        }
    }

    fun toggleRead(emailId: Long, currentlyRead: Boolean) {
        viewModelScope.launch {
            if (currentlyRead) repository.markAsUnread(emailId)
            else repository.markAsRead(emailId)
        }
    }

    fun toggleStar(emailId: Long, currentlyStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(emailId, !currentlyStarred)
        }
    }

    fun deleteEmail(emailId: Long) {
        viewModelScope.launch {
            repository.markAsDeleted(emailId)
        }
    }

    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALIAN)
        private val dateFormat = SimpleDateFormat("dd MMM", Locale.ITALIAN)

        fun formatDate(date: Date): String {
            val now = Calendar.getInstance()
            val emailCal = Calendar.getInstance().apply { time = date }
            return when {
                now.get(Calendar.DAY_OF_YEAR) == emailCal.get(Calendar.DAY_OF_YEAR)
                        && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> timeFormat.format(date)
                now.get(Calendar.DAY_OF_YEAR) - emailCal.get(Calendar.DAY_OF_YEAR) == 1
                        && now.get(Calendar.YEAR) == emailCal.get(Calendar.YEAR) -> "Ieri"
                else -> dateFormat.format(date)
            }
        }
    }
}
