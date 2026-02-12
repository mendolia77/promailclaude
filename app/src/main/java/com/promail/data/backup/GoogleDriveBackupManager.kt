package com.smartmail.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Gestisce il backup e ripristino del database su Google Drive.
 */
class GoogleDriveBackupManager(private val context: Context) {

    companion object {
        private const val APP_FOLDER_NAME = "SmartMailBackups"
        private const val MIME_TYPE = "application/x-sqlite3"
    }

    private var driveService: Drive? = null
    private var appFolderId: String? = null

    /**
     * Crea il client Google Sign-In.
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Verifica se l'utente è già autenticato.
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_FILE)
        )
    }

    /**
     * Ottiene l'account Google attualmente autenticato.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Inizializza il servizio Drive con l'account fornito.
     */
    suspend fun initializeDriveService(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("SmartMail")
                .build()

            // Trova o crea la cartella dell'app
            appFolderId = findOrCreateAppFolder()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trova o crea la cartella SmartMailBackups su Drive.
     */
    private suspend fun findOrCreateAppFolder(): String = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Drive service non inizializzato")

        // Cerca se esiste già
        val query = "mimeType='application/vnd.google-apps.folder' and name='$APP_FOLDER_NAME' and trashed=false"
        val result: FileList = service.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) {
            return@withContext result.files[0].id
        }

        // Crea nuova cartella
        val folderMetadata = File().apply {
            name = APP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = service.files().create(folderMetadata)
            .setFields("id")
            .execute()

        folder.id
    }

    /**
     * Carica un backup su Google Drive.
     */
    suspend fun uploadBackup(backupFile: java.io.File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Effettua prima il login con Google")
            )
            val folderId = appFolderId ?: return@withContext Result.failure(
                IllegalStateException("Cartella Drive non trovata")
            )

            val fileMetadata = File().apply {
                name = backupFile.name
                parents = listOf(folderId)
                mimeType = MIME_TYPE
            }

            val mediaContent = com.google.api.client.http.FileContent(MIME_TYPE, backupFile)

            val file = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, modifiedTime")
                .execute()

            Result.success(file.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista tutti i backup presenti su Google Drive.
     */
    suspend fun listBackups(): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Effettua prima il login con Google")
            )
            val folderId = appFolderId ?: return@withContext Result.failure(
                IllegalStateException("Cartella Drive non trovata")
            )

            val query = "'$folderId' in parents and trashed=false"
            val result: FileList = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()

            val backups = result.files.map { file ->
                DriveBackupFile(
                    id = file.id,
                    name = file.name,
                    size = file.getSize(),
                    modifiedTime = file.modifiedTime?.value ?: 0L
                )
            }

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scarica un backup da Google Drive.
     */
    suspend fun downloadBackup(fileId: String, destinationFile: java.io.File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Effettua prima il login con Google")
            )

            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)

            FileOutputStream(destinationFile).use { fos ->
                fos.write(outputStream.toByteArray())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un backup da Google Drive.
     */
    suspend fun deleteBackup(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Effettua prima il login con Google")
            )

            service.files().delete(fileId).execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnette l'account Google.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            getGoogleSignInClient().signOut()
            driveService = null
            appFolderId = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Rappresenta un file di backup su Google Drive.
 */
data class DriveBackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long
)
