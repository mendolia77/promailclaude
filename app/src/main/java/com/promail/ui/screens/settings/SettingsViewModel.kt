package com.smartmail.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmail.data.backup.BackupManager
import com.smartmail.data.backup.GoogleDriveBackupManager
import com.smartmail.data.backup.DriveBackupFile
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
    // Google Drive
    val isSignedInToGoogleDrive: Boolean = false,
    val googleAccount: GoogleSignInAccount? = null,
    val driveBackups: List<DriveBackupFile> = emptyList(),
    val isUploadingToDrive: Boolean = false,
    val isDownloadingFromDrive: Boolean = false,
    val driveResult: DriveResult? = null
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

sealed class DriveResult {
    data class Success(val message: String) : DriveResult()
    data class Error(val message: String) : DriveResult()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = SmartMailDatabase.getInstance(application)
    private val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())
    private val prefs = AppPreferences(context)
    private val backupManager = BackupManager(context)
    private val driveBackupManager = GoogleDriveBackupManager(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        loadSyncInterval()
        checkGoogleDriveSignIn()
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

    // === GOOGLE DRIVE BACKUP ===

    fun getGoogleSignInClient() = driveBackupManager.getGoogleSignInClient()

    private fun checkGoogleDriveSignIn() {
        val isSignedIn = driveBackupManager.isSignedIn()
        val account = driveBackupManager.getSignedInAccount()
        _uiState.update {
            it.copy(
                isSignedInToGoogleDrive = isSignedIn,
                googleAccount = account
            )
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        viewModelScope.launch {
            val result = driveBackupManager.initializeDriveService(account)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSignedInToGoogleDrive = true,
                            googleAccount = account
                        )
                    }
                    loadDriveBackups()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(driveResult = DriveResult.Error("Errore login: ${error.message}"))
                    }
                }
            )
        }
    }

    fun signOutFromGoogleDrive() {
        viewModelScope.launch {
            driveBackupManager.signOut()
            _uiState.update {
                it.copy(
                    isSignedInToGoogleDrive = false,
                    googleAccount = null,
                    driveBackups = emptyList()
                )
            }
        }
    }

    fun loadDriveBackups() {
        viewModelScope.launch {
            val result = driveBackupManager.listBackups()
            result.fold(
                onSuccess = { backups ->
                    _uiState.update { it.copy(driveBackups = backups) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(driveResult = DriveResult.Error("Errore caricamento: ${error.message}"))
                    }
                }
            )
        }
    }

    fun uploadBackupToDrive(localBackup: File) {
        _uiState.update { it.copy(isUploadingToDrive = true, driveResult = null) }

        viewModelScope.launch {
            val result = driveBackupManager.uploadBackup(localBackup)

            result.fold(
                onSuccess = { fileId ->
                    _uiState.update {
                        it.copy(
                            isUploadingToDrive = false,
                            driveResult = DriveResult.Success("Backup caricato su Drive")
                        )
                    }
                    loadDriveBackups()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingToDrive = false,
                            driveResult = DriveResult.Error("Errore upload: ${error.message}")
                        )
                    }
                }
            )
        }
    }

    fun downloadAndRestoreFromDrive(driveFile: DriveBackupFile) {
        _uiState.update { it.copy(isDownloadingFromDrive = true, driveResult = null) }

        viewModelScope.launch {
            // Scarica il file in una directory temporanea
            val tempFile = File(context.cacheDir, driveFile.name)

            val downloadResult = driveBackupManager.downloadBackup(driveFile.id, tempFile)

            downloadResult.fold(
                onSuccess = {
                    // Ora ripristina dal file scaricato
                    db.close()
                    val restoreResult = backupManager.restoreBackup(tempFile)

                    restoreResult.fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isDownloadingFromDrive = false,
                                    driveResult = DriveResult.Success("Ripristino completato. Riavvia l'app.")
                                )
                            }
                            // Elimina il file temporaneo
                            tempFile.delete()
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isDownloadingFromDrive = false,
                                    driveResult = DriveResult.Error("Errore ripristino: ${error.message}")
                                )
                            }
                            tempFile.delete()
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloadingFromDrive = false,
                            driveResult = DriveResult.Error("Errore download: ${error.message}")
                        )
                    }
                }
            )
        }
    }

    fun deleteDriveBackup(driveFile: DriveBackupFile) {
        viewModelScope.launch {
            val result = driveBackupManager.deleteBackup(driveFile.id)

            result.fold(
                onSuccess = {
                    loadDriveBackups()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(driveResult = DriveResult.Error("Errore eliminazione: ${error.message}"))
                    }
                }
            )
        }
    }

    fun clearDriveResult() {
        _uiState.update { it.copy(driveResult = null) }
    }
}
