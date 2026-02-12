package com.smartmail.data.local.database.dao

import androidx.room.*
import com.smartmail.domain.models.Draft
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: Draft): Long

    @Update
    suspend fun update(draft: Draft)

    @Delete
    suspend fun delete(draft: Draft)

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteById(draftId: Long)

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftById(draftId: Long): Draft?

    @Query("SELECT * FROM drafts WHERE accountId = :accountId ORDER BY lastModifiedDate DESC")
    fun getDraftsByAccount(accountId: Long): Flow<List<Draft>>

    @Query("SELECT * FROM drafts ORDER BY lastModifiedDate DESC")
    fun getAllDrafts(): Flow<List<Draft>>

    @Query("SELECT COUNT(*) FROM drafts")
    fun getDraftCount(): Flow<Int>

    @Query("DELETE FROM drafts WHERE accountId = :accountId")
    suspend fun deleteAllByAccount(accountId: Long)
}
