package com.smartmail.data.local.database.dao

import androidx.room.*
import com.smartmail.domain.models.Email
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    // === INSERT ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(email: Email): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(emails: List<Email>): List<Long>

    // === UPDATE ===

    @Update
    suspend fun update(email: Email)

    @Query("UPDATE emails SET isRead = :read WHERE id = :emailId")
    suspend fun setRead(emailId: Long, read: Boolean)

    @Query("UPDATE emails SET isStarred = :starred WHERE id = :emailId")
    suspend fun setStarred(emailId: Long, starred: Boolean)

    @Query("UPDATE emails SET isImportant = :important WHERE id = :emailId")
    suspend fun setImportant(emailId: Long, important: Boolean)

    @Query("UPDATE emails SET isRead = 1 WHERE accountId = :accountId AND folderName = :folder")
    suspend fun markAllAsRead(accountId: Long, folder: String)

    @Query("UPDATE emails SET locallyDeleted = 1, syncedToServer = 0 WHERE id = :emailId")
    suspend fun markAsDeleted(emailId: Long)

    @Query("UPDATE emails SET folderName = :newFolder, syncedToServer = 0 WHERE id = :emailId")
    suspend fun moveToFolder(emailId: Long, newFolder: String)

    @Query("UPDATE emails SET smartFolderId = :folderId WHERE id = :emailId")
    suspend fun assignSmartFolder(emailId: Long, folderId: Long?)

    // === DELETE ===

    @Delete
    suspend fun delete(email: Email)

    @Query("DELETE FROM emails WHERE id = :emailId")
    suspend fun deleteById(emailId: Long)

    @Query("DELETE FROM emails WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: Long)

    @Query("DELETE FROM emails WHERE locallyDeleted = 1 AND syncedToServer = 1")
    suspend fun purgeDeletedEmails()

    @Query("SELECT * FROM emails WHERE locallyDeleted = 1 AND syncedToServer = 0")
    suspend fun getUnsyncedDeletedEmails(): List<Email>

    // === QUERY - Inbox & Folder ===

    @Query("""
        SELECT * FROM emails
        WHERE accountId = :accountId AND folderName = :folder AND locallyDeleted = 0
        ORDER BY receivedDate DESC
    """)
    fun getEmailsByFolder(accountId: Long, folder: String): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE folderName = :folder AND locallyDeleted = 0
        ORDER BY receivedDate DESC
    """)
    fun getAllEmailsByFolder(folder: String): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0
        AND smartFolderId IS NULL
        AND isStarred = 0
        AND isImportant = 0
        ORDER BY receivedDate DESC
    """)
    fun getUncategorizedEmails(): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0
        ORDER BY receivedDate DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getAllEmailsPaged(limit: Int, offset: Int): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0
        ORDER BY receivedDate DESC
    """)
    fun getAllEmails(): Flow<List<Email>>

    @Query("SELECT * FROM emails WHERE id = :emailId")
    suspend fun getEmailById(emailId: Long): Email?

    @Query("SELECT * FROM emails WHERE id = :emailId")
    fun getEmailByIdFlow(emailId: Long): Flow<Email?>

    @Query("SELECT * FROM emails WHERE messageId = :messageId LIMIT 1")
    suspend fun getEmailByMessageId(messageId: String): Email?

    // === QUERY - Smart Folder ===

    @Query("""
        SELECT * FROM emails
        WHERE smartFolderId = :folderId AND locallyDeleted = 0
        ORDER BY receivedDate DESC
    """)
    fun getEmailsBySmartFolder(folderId: Long): Flow<List<Email>>

    // === QUERY - Thread ===

    @Query("""
        SELECT * FROM emails
        WHERE threadId = :threadId AND locallyDeleted = 0
        ORDER BY receivedDate ASC
    """)
    fun getEmailThread(threadId: String): Flow<List<Email>>

    // === QUERY - Ricerca ===

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0
        AND (subject LIKE '%' || :query || '%'
             OR fromName LIKE '%' || :query || '%'
             OR fromAddress LIKE '%' || :query || '%'
             OR bodyText LIKE '%' || :query || '%')
        ORDER BY receivedDate DESC
        LIMIT :limit
    """)
    fun searchEmails(query: String, limit: Int = 50): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE accountId = :accountId AND locallyDeleted = 0
        AND (subject LIKE '%' || :query || '%'
             OR fromName LIKE '%' || :query || '%'
             OR fromAddress LIKE '%' || :query || '%')
        ORDER BY receivedDate DESC
        LIMIT :limit
    """)
    fun searchEmailsInAccount(accountId: Long, query: String, limit: Int = 50): Flow<List<Email>>

    // === QUERY - Contatori ===

    @Query("SELECT COUNT(*) FROM emails WHERE isRead = 0 AND locallyDeleted = 0")
    fun getTotalUnreadCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM emails
        WHERE accountId = :accountId AND folderName = :folder
        AND isRead = 0 AND locallyDeleted = 0
    """)
    fun getUnreadCount(accountId: Long, folder: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM emails
        WHERE smartFolderId = :folderId AND isRead = 0 AND locallyDeleted = 0
    """)
    fun getUnreadCountBySmartFolder(folderId: Long): Flow<Int>

    // === QUERY - Sync ===

    @Query("SELECT * FROM emails WHERE syncedToServer = 0")
    suspend fun getUnsyncedEmails(): List<Email>

    @Query("SELECT * FROM emails WHERE locallyDeleted = 1 AND syncedToServer = 0")
    suspend fun getPendingDeletions(): List<Email>

    @Query("""
        SELECT MAX(receivedDate) FROM emails
        WHERE accountId = :accountId AND folderName = :folder
    """)
    suspend fun getLatestEmailDate(accountId: Long, folder: String): Long?

    // === QUERY - Filtri per Smart Folder ===

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0 AND isStarred = 1
        ORDER BY receivedDate DESC
    """)
    fun getStarredEmails(): Flow<List<Email>>

    @Query("""
        SELECT COUNT(*) FROM emails
        WHERE locallyDeleted = 0 AND isStarred = 1
    """)
    fun getStarredCount(): Flow<Int>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0 AND hasAttachments = 1
        ORDER BY receivedDate DESC
    """)
    fun getEmailsWithAttachments(): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0 AND isImportant = 1
        ORDER BY receivedDate DESC
    """)
    fun getImportantEmails(): Flow<List<Email>>

    @Query("""
        SELECT * FROM emails
        WHERE locallyDeleted = 0
        AND fromAddress LIKE '%@' || :domain
        ORDER BY receivedDate DESC
    """)
    fun getEmailsByDomain(domain: String): Flow<List<Email>>
}
