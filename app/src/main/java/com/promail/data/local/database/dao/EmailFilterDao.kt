package com.smartmail.data.local.database.dao

import androidx.room.*
import com.smartmail.domain.models.EmailFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailFilterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: EmailFilter): Long

    @Update
    suspend fun update(filter: EmailFilter)

    @Delete
    suspend fun delete(filter: EmailFilter)

    @Query("SELECT * FROM email_filters WHERE id = :id")
    suspend fun getById(id: Long): EmailFilter?

    @Query("SELECT * FROM email_filters ORDER BY priority ASC, createdAt DESC")
    fun getAllFilters(): Flow<List<EmailFilter>>

    @Query("SELECT * FROM email_filters WHERE isEnabled = 1 ORDER BY priority ASC, createdAt DESC")
    suspend fun getEnabledFilters(): List<EmailFilter>

    @Query("DELETE FROM email_filters WHERE id = :id")
    suspend fun deleteById(id: Long)
}
