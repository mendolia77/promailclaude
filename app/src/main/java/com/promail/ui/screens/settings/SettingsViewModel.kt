package com.smartmail.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.backup.BackupManager
import com.smartmail.data.backup.CloudBackupManager
import android.content.Intent
import android.net.Uri
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.preferences.AppPreferences
import com.smartmail.data.remote.ImapClient
import com.smartmail.data.remote.SmtpClient
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Account
import com.smartmail.domain.models.AccountPresets
import com.smartmail.domain.models.AccountType
import com.smartmail.workers.EmailSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    // Form aggiunta account
    val showAddAccount: Boolean = false,
    val formEmail: String = "",
    val formPassword: String = "",
    val formDisplayName: String = "",
    val formAccountType: AccountType = AccountType.GMAIL,
    val formColor: String = "#6B2FBF",
    // Configurazione avanzata (per IMAP_CUSTOM)
    val formImapHost: String = "",
    val formImapPort: String = "993",
    val formSmtpHost: String = "",
    val formSmtpPort: String = "587",
    // Stato
    val isTesting: Boolean = false,
    val testResult: TestResult? = null,
    val saveResult: SaveResult? = null,
    // Impostazioni generali
    val syncIntervalMinutes: Long = 15,
    val syncIntervalLabel: String = "15 minuti",
    val showSyncIntervalDialog: Boolean = false,
    // Backup
    val showBackupDialog: Boolean = false,
    val availableBackups: List<File> = emptyList(),
    val isCreatingBackup: Boolean = false,
    val isRestoringBackup: Boolean = false,
    val backupResult: BackupResult? = null,
    // Cloud Backup
    val cloudBackupIntent: Intent? = null,
    val cloudRestoreIntent: Intent? = null
)

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}

sealed class SaveResult {
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}


class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())
    private val prefs = AppPreferences(context)
    private val backupManager = BackupManager(context)
    private val cloudBackupManager = CloudBackupManager(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        loadSyncInterval()
    }

    private fun loadSyncInterval() {
        _uiState.update {
            it.copy(
                syncIntervalMinutes = prefs.syncIntervalMinutes,
                syncIntervalLabel = prefs.getSyncIntervalLabel()
            )
        }
    }

    // === Navigazione form ===

    fun showAddAccount() {
        _uiState.update {
            it.copy(
                showAddAccount = true,
                formEmail = "",
                formPassword = "",
                formDisplayName = "",
                formAccountType = AccountType.GMAIL,
                formColor = "#6B2FBF",
                formImapHost = "",
                formImapPort = "993",
                formSmtpHost = "",
                formSmtpPort = "587",
                testResult = null,
                saveResult = null
            )
        }
    }

    fun hideAddAccount() {
        _uiState.update { it.copy(showAddAccount = false) }
    }

    // === Aggiornamento form ===

    fun updateFormEmail(value: String) { _uiState.update { it.copy(formEmail = value) } }
    fun updateFormPassword(value: String) { _uiState.update { it.copy(formPassword = value) } }
    fun updateFormDisplayName(value: String) { _uiState.update { it.copy(formDisplayName = value) } }
    fun updateFormColor(value: String) { _uiState.update { it.copy(formColor = value) } }

    fun updateFormAccountType(type: AccountType) {
        val config = AccountPresets.getImapConfig(type)
        _uiState.update {
            it.copy(
                formAccountType = type,
                formImapHost = config.imapHost,
                formImapPort = config.imapPort.toString(),
                formSmtpHost = config.smtpHost,
                formSmtpPort = config.smtpPort.toString()
            )
        }
    }

    fun updateFormImapHost(value: String) { _uiState.update { it.copy(formImapHost = value) } }
    fun updateFormImapPort(value: String) { _uiState.update { it.copy(formImapPort = value) } }
    fun updateFormSmtpHost(value: String) { _uiState.update { it.copy(formSmtpHost = value) } }
    fun updateFormSmtpPort(value: String) { _uiState.update { it.copy(formSmtpPort = value) } }

    fun clearResults() {
        _uiState.update { it.copy(testResult = null, saveResult = null) }
    }

    /**
     * Testa la connessione con le credenziali inserite.
     */
    fun testConnection() {
        val state = _uiState.value
        if (state.formEmail.isBlank() || state.formPassword.isBlank()) {
            _uiState.update { it.copy(testResult = TestResult.Error("Inserisci email e password")) }
            return
        }

        _uiState.update { it.copy(isTesting = true, testResult = null) }

        viewModelScope.launch {
            val account = buildAccountFromForm()
            val smtpClient = SmtpClient(account)
            val result = smtpClient.testConnection()

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isTesting = false, testResult = TestResult.Success) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isTesting = false, testResult = TestResult.Error(error.message ?: "Errore sconosciuto"))
                    }
                }
            )
        }
    }

    /**
     * Salva l'account nel database.
     */
    fun saveAccount() {
        val state = _uiState.value
        if (state.formEmail.isBlank() || state.formPassword.isBlank()) {
            _uiState.update { it.copy(saveResult = SaveResult.Error("Compila tutti i campi obbligatori")) }
            return
        }

        _uiState.update { it.copy(isLoading = true, saveResult = null) }

        viewModelScope.launch {
            try {
                val account = buildAccountFromForm()
                val isFirst = _uiState.value.accounts.isEmpty()
                repository.addAccount(account.copy(isDefault = isFirst))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        saveResult = SaveResult.Success,
                        showAddAccount = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, saveResult = SaveResult.Error("Errore salvataggio: ${e.message}"))
                }
            }
        }
    }

    /**
     * Rimuove un account.
     */
    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    /**
     * Imposta un account come predefinito.
     */
    fun setDefaultAccount(accountId: Long) {
        viewModelScope.launch {
            repository.setDefaultAccount(accountId)
        }
    }

    /**
     * Attiva/disattiva la sync per un account.
     */
    fun toggleSync(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account.copy(syncEnabled = !account.syncEnabled))
        }
    }

    private fun buildAccountFromForm(): Account {
        val state = _uiState.value
        val config = AccountPresets.getImapConfig(state.formAccountType)

        return Account(
            emailAddress = state.formEmail.trim(),
            displayName = state.formDisplayName.ifBlank { null },
            encryptedPassword = state.formPassword, // TODO: cifrare con EncryptedSharedPreferences
            accountType = state.formAccountType,
            imapHost = state.formImapHost.ifBlank { config.imapHost },
            imapPort = state.formImapPort.toIntOrNull() ?: config.imapPort,
            smtpHost = state.formSmtpHost.ifBlank { config.smtpHost },
            smtpPort = state.formSmtpPort.toIntOrNull() ?: config.smtpPort,
            color = state.formColor
        )
    }

    // === SYNC INTERVAL ===

    fun showSyncIntervalDialog() {
        _uiState.update { it.copy(showSyncIntervalDialog = true) }
    }

    fun hideSyncIntervalDialog() {
        _uiState.update { it.copy(showSyncIntervalDialog = false) }
    }

    fun updateSyncInterval(minutes: Long) {
        prefs.syncIntervalMinutes = minutes
        loadSyncInterval()
        hideSyncIntervalDialog()

        // Riavvia il worker con il nuovo intervallo
        EmailSyncWorker.schedulePeriodic(context, minutes)
    }

    // === BACKUP & RESTORE ===

    fun showBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = true) }
        loadAvailableBackups()
    }

    fun hideBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = false, backupResult = null) }
    }

    private fun loadAvailableBackups() {
        viewModelScope.launch {
            val result = backupManager.getAvailableBackups()
            result.fold(
                onSuccess = { backups ->
                    _uiState.update { it.copy(availableBackups = backups) }
                },
                onFailure = {
                    _uiState.update { it.copy(availableBackups = emptyList()) }
                }
            )
        }
    }

    fun createBackup() {
        _uiState.update { it.copy(isCreatingBackup = true, backupResult = null) }

        viewModelScope.launch {
            val result = backupManager.createBackup()

            result.fold(
                onSuccess = { backupFile ->
                    _uiState.update {
                        it.copy(
                            isCreatingBackup = false,
                            backupResult = BackupResult.Success("Backup creato: ${backupFile.name}")
                        )
                    }
                    loadAvailableBackups()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingBackup = false,
                            backupResult = BackupResult.Error("Errore backup: ${error.message}")
                        )
                    }
                }
            )
        }
    }

    fun restoreBackup(backupFile: File) {
        _uiState.update { it.copy(isRestoringBackup = true, backupResult = null) }

        viewModelScope.launch {
            // Chiudi il database prima del ripristino
            db.close()

            val result = backupManager.restoreBackup(backupFile)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isRestoringBackup = false,
                            backupResult = BackupResult.Success("Ripristino completato. Riavvia l'app.")
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRestoringBackup = false,
                            backupResult = BackupResult.Error("Errore ripristino: ${error.message}")
                        )
                    }
                }
            )
        }
    }

    fun deleteBackup(backupFile: File) {
        viewModelScope.launch {
            val result = backupManager.deleteBackup(backupFile)

            result.fold(
                onSuccess = {
                    loadAvailableBackups()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(backupResult = BackupResult.Error("Errore eliminazione: ${error.message}"))
                    }
                }
            )
        }
    }

    fun clearBackupResult() {
        _uiState.update { it.copy(backupResult = null) }
    }

    // === CLOUD BACKUP (via sistema Android) ===

    /**
     * Prepara l'Intent per condividere il backup su cloud.
     */
    fun prepareShareBackupToCloud(backupFile: File) {
        viewModelScope.launch {
            val result = cloudBackupManager.shareBackupToCloud(backupFile)
            result.fold(
                onSuccess = { intent ->
                    _uiState.update { it.copy(cloudBackupIntent = intent) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(backupResult = BackupResult.Error("Errore: ${error.message}"))
                    }
                }
            )
        }
    }

    /**
     * Crea l'Intent per ripristinare da cloud.
     */
    fun prepareRestoreFromCloud() {
        val intent = cloudBackupManager.createRestoreFromCloudIntent()
        _uiState.update { it.copy(cloudRestoreIntent = intent) }
    }

    /**
     * Importa e ripristina un backup selezionato dal cloud.
     */
    fun importAndRestoreFromCloud(uri: Uri) {
        _uiState.update { it.copy(isRestoringBackup = true, backupResult = null) }

        viewModelScope.launch {
            // Importa il file dal cloud
            val importResult = cloudBackupManager.importBackupFromCloud(uri)

            importResult.fold(
                onSuccess = { importedFile ->
                    // Chiudi il database
                    db.close()

                    // Ripristina dal file importato
                    val restoreResult = backupManager.restoreBackup(importedFile)

                    restoreResult.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isRestoringBackup = false,
                                    backupResult = BackupResult.Success("Ripristino completato. Riavvia l'app.")
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isRestoringBackup = false,
                                    backupResult = BackupResult.Error("Errore ripristino: ${error.message}")
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRestoringBackup = false,
                            backupResult = BackupResult.Error("Errore importazione: ${error.message}")
                        )
                    }
                }
            )
        }
    }

    fun clearCloudIntents() {
        _uiState.update { it.copy(cloudBackupIntent = null, cloudRestoreIntent = null) }
    }
}
