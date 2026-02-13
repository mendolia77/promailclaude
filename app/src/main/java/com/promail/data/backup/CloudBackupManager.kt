package com.smartmail.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestisce il backup su cloud usando il sistema di condivisione nativo di Android.
 * Permette di salvare i backup su qualsiasi servizio cloud (Google Drive, Dropbox, OneDrive, etc.)
 */
class CloudBackupManager(private val context: Context) {

    /**
     * Crea un Intent per condividere/salvare il backup su cloud.
     * L'utente può scegliere dove salvare il file (Drive, Dropbox, email, etc.)
     */
    suspend fun shareBackupToCloud(backupFile: File): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            // Usa FileProvider per creare un URI sicuro
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            // Crea l'Intent per condividere il file
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SmartMail Backup - ${backupFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Backup del database SmartMail")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Crea il chooser per mostrare tutte le app disponibili
            val chooserIntent = Intent.createChooser(shareIntent, "Salva backup su...")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            Result.success(chooserIntent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea un Intent per aprire il file picker e selezionare un backup da ripristinare.
     */
    fun createRestoreFromCloudIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            // Permette di selezionare file .db
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",
                "application/x-sqlite3",
                "*/*"
            ))
        }
        return intent
    }

    /**
     * Copia il file selezionato dall'Intent nella directory dei backup locali.
     */
    suspend fun importBackupFromCloud(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Impossibile aprire il file"))

            // Crea il file di destinazione
            val timestamp = System.currentTimeMillis()
            val backupDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                "SmartMail/Backups"
            )
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val destinationFile = File(backupDir, "smartmail_backup_imported_$timestamp.db")

            // Copia il contenuto
            inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
