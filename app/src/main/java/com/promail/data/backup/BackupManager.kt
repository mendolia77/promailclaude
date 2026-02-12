package com.smartmail.data.backup

import android.content.Context
import android.os.Environment
import com.smartmail.data.local.database.SmartMailDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_DIR = "SmartMail/Backups"
        private const val DB_NAME = "smartmail_db"
    }

    /**
     * Crea un backup del database.
     * @return Il file di backup creato o null in caso di errore
     */
    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Chiudi il database per sicurezza
            val db = SmartMailDatabase.getInstance(context)
            db.close()

            // Percorso database corrente
            val currentDB = context.getDatabasePath(DB_NAME)

            if (!currentDB.exists()) {
                return@withContext Result.failure(Exception("Database non trovato"))
            }

            // Directory backup in Downloads
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_DIR
            )

            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Nome file con timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "smartmail_backup_$timestamp.db")

            // Copia il file
            FileInputStream(currentDB).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Riapri il database
            SmartMailDatabase.getInstance(context)

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ripristina il database da un backup.
     * @param backupFile Il file di backup da ripristinare
     */
    suspend fun restoreBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("File di backup non trovato"))
            }

            // Chiudi il database
            val db = SmartMailDatabase.getInstance(context)
            db.close()

            // Percorso database corrente
            val currentDB = context.getDatabasePath(DB_NAME)

            // Fai un backup del database corrente prima di sovrascriverlo
            val tempBackup = File(currentDB.parent, "${DB_NAME}_temp_backup")
            if (currentDB.exists()) {
                FileInputStream(currentDB).use { input ->
                    FileOutputStream(tempBackup).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            try {
                // Sovrascrivi con il backup
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(currentDB).use { output ->
                        input.copyTo(output)
                    }
                }

                // Elimina il backup temporaneo se tutto è andato bene
                tempBackup.delete()

                // Riapri il database
                SmartMailDatabase.getInstance(context)

                Result.success(Unit)
            } catch (e: Exception) {
                // Ripristina il backup temporaneo in caso di errore
                if (tempBackup.exists()) {
                    FileInputStream(tempBackup).use { input ->
                        FileOutputStream(currentDB).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempBackup.delete()
                }
                throw e
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottiene la lista dei backup disponibili.
     */
    suspend fun getAvailableBackups(): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_DIR
            )

            if (!backupDir.exists()) {
                return@withContext Result.success(emptyList())
            }

            val backups = backupDir.listFiles { file ->
                file.name.startsWith("smartmail_backup_") && file.extension == "db"
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un backup.
     */
    suspend fun deleteBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (backupFile.exists() && backupFile.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossibile eliminare il backup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
