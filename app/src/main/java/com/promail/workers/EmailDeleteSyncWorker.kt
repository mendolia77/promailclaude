package com.smartmail.workers

import android.content.Context
import androidx.work.*
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker per sincronizzare le eliminazioni di email sul server IMAP.
 * Eseguito periodicamente per spostare le email eliminate nel Trash del server.
 */
class EmailDeleteSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = SmartMailDatabase.getInstance(applicationContext)
            val repository = EmailRepository(db.accountDao(), db.emailDao(), db.smartFolderDao())

            // Ottieni tutte le email marcate come eliminate localmente ma non sincronizzate
            val deletedEmails = db.emailDao().getUnsyncedDeletedEmails()

            if (deletedEmails.isEmpty()) {
                return@withContext Result.success()
            }

            // Raggruppa per account
            val emailsByAccount = deletedEmails.groupBy { it.accountId }

            var successCount = 0
            var failureCount = 0

            for ((accountId, emails) in emailsByAccount) {
                // Ottieni l'account
                val account = repository.getAccountById(accountId)
                if (account == null) {
                    failureCount += emails.size
                    continue
                }

                // Elimina ogni email sul server
                for (email in emails) {
                    val result = repository.deleteEmailOnServer(email, account)

                    if (result.isSuccess) {
                        // Marca come sincronizzata o elimina definitivamente dal DB locale
                        db.emailDao().deleteById(email.id)
                        successCount++
                    } else {
                        failureCount++
                        // Log dell'errore (opzionale)
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
            }

            // Se ci sono fallimenti ma anche successi, ritorna Success
            // Se tutti falliscono, ritorna Retry
            return@withContext if (failureCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "email_delete_sync"

        /**
         * Schedula la sincronizzazione periodica delle eliminazioni.
         */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<EmailDeleteSyncWorker>(
                30, java.util.concurrent.TimeUnit.MINUTES // Ogni 30 minuti
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Sincronizza immediatamente le eliminazioni.
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<EmailDeleteSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
