package com.smartmail.data.local.database.dao

import androidx.room.*
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartFolderDao {

    // === INSERT ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: SmartFolder): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(folders: List<SmartFolder>)

    // === UPDATE ===

    @Update
    suspend fun update(folder: SmartFolder)

    @Query("UPDATE smart_folders SET isActive = :active WHERE id = :folderId")
    suspend fun setActive(folderId: Long, active: Boolean)

    @Query("UPDATE smart_folders SET notificationsEnabled = :enabled WHERE id = :folderId")
    suspend fun setNotifications(folderId: Long, enabled: Boolean)

    @Query("UPDATE smart_folders SET sortOrder = :order WHERE id = :folderId")
    suspend fun updateSortOrder(folderId: Long, order: Int)

    @Query("UPDATE smart_folders SET rulesJson = :rulesJson WHERE id = :folderId")
    suspend fun updateRules(folderId: Long, rulesJson: String)

    // === DELETE ===

    @Delete
    suspend fun delete(folder: SmartFolder)

    @Query("DELETE FROM smart_folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)

    @Query("DELETE FROM smart_folders WHERE isSystemFolder = 0")
    suspend fun deleteAllCustomFolders()

    // === QUERY ===

    @Query("SELECT * FROM smart_folders ORDER BY sortOrder ASC")
    fun getAllFolders(): Flow<List<SmartFolder>>

    @Query("SELECT * FROM smart_folders WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getActiveFolders(): Flow<List<SmartFolder>>

    @Query("SELECT * FROM smart_folders WHERE isSystemFolder = 1 ORDER BY sortOrder ASC")
    fun getSystemFolders(): Flow<List<SmartFolder>>

    @Query("SELECT * FROM smart_folders WHERE isSystemFolder = 0 ORDER BY sortOrder ASC")
    fun getCustomFolders(): Flow<List<SmartFolder>>

    @Query("SELECT * FROM smart_folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Long): SmartFolder?

    @Query("SELECT * FROM smart_folders WHERE id = :folderId")
    fun getFolderByIdFlow(folderId: Long): Flow<SmartFolder?>

    @Query("SELECT * FROM smart_folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): SmartFolder?

    @Query("SELECT COUNT(*) FROM smart_folders")
    fun getFolderCount(): Flow<Int>

    @Query("SELECT MAX(sortOrder) FROM smart_folders")
    suspend fun getMaxSortOrder(): Int?

    // Elimina cartelle duplicate mantenendo solo la prima di ogni nome
    @Query("""
        DELETE FROM smart_folders
        WHERE id NOT IN (
            SELECT MIN(id) FROM smart_folders GROUP BY name
        )
    """)
    suspend fun removeDuplicates()
}
