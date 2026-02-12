package com.smartmail.data.repository

import com.smartmail.data.local.database.dao.AccountDao
import com.smartmail.data.local.database.dao.EmailDao
import com.smartmail.data.local.database.dao.SmartFolderDao
import com.smartmail.domain.models.Account
import com.smartmail.domain.models.AccountType
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.Flow

class EmailRepository(
    private val accountDao: AccountDao,
    private val emailDao: EmailDao,
    private val smartFolderDao: SmartFolderDao
) {

    // ==========================================
    // ACCOUNT
    // ==========================================

    suspend fun addAccount(account: Account): Long = accountDao.insert(account)

    suspend fun updateAccount(account: Account) = accountDao.update(account)

    suspend fun deleteAccount(accountId: Long) = accountDao.deleteById(accountId)

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    fun getActiveAccounts(): Flow<List<Account>> = accountDao.getActiveAccounts()

    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)

    suspend fun getDefaultAccount(): Account? = accountDao.getDefaultAccount()

    suspend fun setDefaultAccount(accountId: Long) = accountDao.changeDefaultAccount(accountId)

    suspend fun getAccountsToSync(): List<Account> = accountDao.getAccountsToSync()

    suspend fun updateLastSync(accountId: Long, timestamp: Long) =
        accountDao.updateLastSync(accountId, timestamp)

    fun getAccountCount(): Flow<Int> = accountDao.getAccountCount()

    // ==========================================
    // EMAIL
    // ==========================================

    suspend fun saveEmail(email: Email): Long = emailDao.insert(email)

    suspend fun saveEmails(emails: List<Email>): List<Long> = emailDao.insertAll(emails)

    suspend fun updateEmail(email: Email) = emailDao.update(email)

    fun getInbox(accountId: Long): Flow<List<Email>> =
        emailDao.getEmailsByFolder(accountId, "INBOX")

    fun getUnifiedInbox(): Flow<List<Email>> =
        emailDao.getUncategorizedEmails()

    fun getEmailsByFolder(accountId: Long, folder: String): Flow<List<Email>> =
        emailDao.getEmailsByFolder(accountId, folder)

    fun getAllEmailsPaged(limit: Int, offset: Int): Flow<List<Email>> =
        emailDao.getAllEmailsPaged(limit, offset)

    suspend fun getEmailById(id: Long): Email? = emailDao.getEmailById(id)

    fun getEmailByIdFlow(id: Long): Flow<Email?> = emailDao.getEmailByIdFlow(id)

    suspend fun getEmailByMessageId(messageId: String): Email? =
        emailDao.getEmailByMessageId(messageId)

    // Flags
    suspend fun markAsRead(emailId: Long) = emailDao.setRead(emailId, true)

    suspend fun markAsUnread(emailId: Long) = emailDao.setRead(emailId, false)

    suspend fun toggleStar(emailId: Long, starred: Boolean) =
        emailDao.setStarred(emailId, starred)

    suspend fun setImportant(emailId: Long, important: Boolean) =
        emailDao.setImportant(emailId, important)

    suspend fun markAllAsRead(accountId: Long, folder: String) =
        emailDao.markAllAsRead(accountId, folder)

    // Spostamento e cancellazione
    suspend fun moveToTrash(emailId: Long) = emailDao.moveToFolder(emailId, "TRASH")

    suspend fun moveToFolder(emailId: Long, folder: String) =
        emailDao.moveToFolder(emailId, folder)

    suspend fun markAsDeleted(emailId: Long) = emailDao.markAsDeleted(emailId)

    suspend fun deleteEmail(emailId: Long) = emailDao.deleteById(emailId)

    /**
     * Elimina un'email spostandola nel Trash sul server IMAP
     */
    suspend fun deleteEmailOnServer(email: Email, account: Account): Result<Unit> {
        return try {
            val imapClient = com.smartmail.data.remote.ImapClient(account)
            val connectResult = imapClient.connect()

            if (connectResult.isFailure) {
                return Result.failure(connectResult.exceptionOrNull() ?: Exception("Connessione fallita"))
            }

            val result = imapClient.moveToTrash(email.messageId, email.folderName)
            imapClient.disconnect()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ricerca
    fun searchEmails(query: String): Flow<List<Email>> = emailDao.searchEmails(query)

    fun searchEmailsInAccount(accountId: Long, query: String): Flow<List<Email>> =
        emailDao.searchEmailsInAccount(accountId, query)

    // Contatori
    fun getTotalUnreadCount(): Flow<Int> = emailDao.getTotalUnreadCount()

    fun getUnreadCount(accountId: Long, folder: String): Flow<Int> =
        emailDao.getUnreadCount(accountId, folder)

    // Thread
    fun getEmailThread(threadId: String): Flow<List<Email>> =
        emailDao.getEmailThread(threadId)

    // Filtri rapidi
    fun getStarredEmails(): Flow<List<Email>> = emailDao.getStarredEmails()

    fun getStarredCount(): Flow<Int> = emailDao.getStarredCount()

    fun getEmailsWithAttachments(): Flow<List<Email>> = emailDao.getEmailsWithAttachments()

    fun getImportantEmails(): Flow<List<Email>> = emailDao.getImportantEmails()

    // Sync
    suspend fun getUnsyncedEmails(): List<Email> = emailDao.getUnsyncedEmails()

    suspend fun getPendingDeletions(): List<Email> = emailDao.getPendingDeletions()

    suspend fun purgeDeletedEmails() = emailDao.purgeDeletedEmails()

    suspend fun getLatestEmailDate(accountId: Long, folder: String): Long? =
        emailDao.getLatestEmailDate(accountId, folder)

    // ==========================================
    // SMART FOLDER
    // ==========================================

    suspend fun addSmartFolder(folder: SmartFolder): Long = smartFolderDao.insert(folder)

    suspend fun updateSmartFolder(folder: SmartFolder) = smartFolderDao.update(folder)

    suspend fun deleteSmartFolder(folderId: Long) = smartFolderDao.deleteById(folderId)

    fun getAllSmartFolders(): Flow<List<SmartFolder>> = smartFolderDao.getAllFolders()

    fun getActiveSmartFolders(): Flow<List<SmartFolder>> = smartFolderDao.getActiveFolders()

    suspend fun getSmartFolderById(id: Long): SmartFolder? = smartFolderDao.getFolderById(id)

    fun getEmailsBySmartFolder(folderId: Long): Flow<List<Email>> =
        emailDao.getEmailsBySmartFolder(folderId)

    fun getUnreadCountBySmartFolder(folderId: Long): Flow<Int> =
        emailDao.getUnreadCountBySmartFolder(folderId)

    suspend fun updateSmartFolderRules(folderId: Long, rulesJson: String) =
        smartFolderDao.updateRules(folderId, rulesJson)

    suspend fun assignEmailToSmartFolder(emailId: Long, folderId: Long?) =
        emailDao.assignSmartFolder(emailId, folderId)
}
