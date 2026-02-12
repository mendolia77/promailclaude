package com.smartmail.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.smartmail.MainActivity
import com.smartmail.data.local.database.SmartMailDatabase
import com.smartmail.data.remote.ImapClient
import com.smartmail.data.repository.EmailRepository
import com.smartmail.domain.models.Account
import java.util.Date
import java.util.concurrent.TimeUnit

class EmailSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "EmailSyncWorker"
        private const val WORK_NAME = "email_sync"
        private const val CHANNEL_ID = "email_sync_channel"
        private const val NOTIFICATION_ID = 1001

        // Chiave per sync di un singolo account
        const val KEY_ACCOUNT_ID = "account_id"
        // Chiave per forzare sync completo
        const val KEY_FORCE_FULL = "force_full"

        /**
         * Programma la sync periodica.
         */
        fun schedulePeriodic(context: Context, intervalMinutes: Long = 1) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<EmailSyncWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "Sync periodica programmata ogni $intervalMinutes minuti")
        }

        /**
         * Esegui una sync immediata (one-shot).
         */
        fun syncNow(context: Context, accountId: Long? = null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dataBuilder = Data.Builder()
            if (accountId != null) {
                dataBuilder.putLong(KEY_ACCOUNT_ID, accountId)
            }

            val request = OneTimeWorkRequestBuilder<EmailSyncWorker>()
                .setConstraints(constraints)
                .setInputData(dataBuilder.build())
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Annulla tutta la sync.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Inizio sincronizzazione email...")

        val db = SmartMailDatabase.getInstance(applicationContext)
        val repository = EmailRepository(
            db.accountDao(),
            db.emailDao(),
            db.smartFolderDao()
        )

        // Determina quali account sincronizzare
        val specificAccountId = inputData.getLong(KEY_ACCOUNT_ID, -1L)
        val accounts = if (specificAccountId > 0) {
            listOfNotNull(repository.getAccountById(specificAccountId))
        } else {
            repository.getAccountsToSync()
        }

        if (accounts.isEmpty()) {
            Log.d(TAG, "Nessun account da sincronizzare")
            return Result.success()
        }

        var totalNewEmails = 0
        var errors = 0

        accounts.forEach { account ->
            try {
                val newCount = syncAccount(account, repository)
                totalNewEmails += newCount
                repository.updateLastSync(account.id, System.currentTimeMillis())
                Log.d(TAG, "Account ${account.emailAddress}: $newCount nuove email")
            } catch (e: Exception) {
                Log.e(TAG, "Errore sync ${account.emailAddress}: ${e.message}")
                errors++
            }
        }

        // Notifica se ci sono nuove email
        if (totalNewEmails > 0) {
            showNotification(totalNewEmails)
        }

        // Pulisci email cancellate e già sincronizzate
        repository.purgeDeletedEmails()

        return if (errors == accounts.size) {
            // Tutti gli account hanno fallito: ritenta
            Result.retry()
        } else {
            Result.success(
                workDataOf("new_emails" to totalNewEmails, "errors" to errors)
            )
        }
    }

    /**
     * Sincronizza un singolo account.
     */
    private suspend fun syncAccount(account: Account, repository: EmailRepository): Int {
        val client = ImapClient(account)

        return try {
            val connectResult = client.connect()
            if (connectResult.isFailure) {
                throw connectResult.exceptionOrNull()!!
            }

            // Sync incrementale: recupera solo email dopo l'ultima sync
            val sinceDate = repository.getLatestEmailDate(account.id, "INBOX")?.let {
                Date(it)
            }

            val fetchResult = client.fetchEmails(
                folderName = "INBOX",
                limit = 100,
                sinceDate = sinceDate
            )

            if (fetchResult.isFailure) {
                throw fetchResult.exceptionOrNull()!!
            }

            val emails = fetchResult.getOrThrow()

            // Salva solo email nuove (INSERT IGNORE per messageId unico)
            val inserted = repository.saveEmails(emails)
            inserted.count { it != -1L } // Conta solo quelle effettivamente inserite
        } finally {
            client.disconnect()
        }
    }

    /**
     * Mostra notifica per nuove email.
     */
    private fun showNotification(count: Int) {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crea canale (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nuove Email",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per nuove email ricevute"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val text = if (count == 1) "1 nuova email" else "$count nuove email"

        // Intent per aprire l'app quando si clicca sulla notifica
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Suono di notifica predefinito
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("SmartMail")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setNumber(count)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
