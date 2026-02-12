package com.smartmail.data.local.database.dao

import androidx.room.*
import com.smartmail.domain.models.Account
import com.smartmail.domain.models.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    // === INSERT ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)

    // === UPDATE ===

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET lastSyncTimestamp = :timestamp WHERE id = :accountId")
    suspend fun updateLastSync(accountId: Long, timestamp: Long)

    @Query("UPDATE accounts SET isActive = :active WHERE id = :accountId")
    suspend fun setActive(accountId: Long, active: Boolean)

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :accountId")
    suspend fun setDefaultAccount(accountId: Long)

    @Transaction
    suspend fun changeDefaultAccount(accountId: Long) {
        clearDefaultFlag()
        setDefaultAccount(accountId)
    }

    @Query("UPDATE accounts SET syncEnabled = :enabled WHERE id = :accountId")
    suspend fun setSyncEnabled(accountId: Long, enabled: Boolean)

    // === DELETE ===

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: Long)

    // === QUERY ===

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, emailAddress ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY isDefault DESC, emailAddress ASC")
    fun getActiveAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdFlow(accountId: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?

    @Query("SELECT * FROM accounts WHERE emailAddress = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): Account?

    @Query("SELECT * FROM accounts WHERE accountType = :type")
    fun getAccountsByType(type: AccountType): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE syncEnabled = 1 AND isActive = 1")
    suspend fun getAccountsToSync(): List<Account>

    @Query("SELECT COUNT(*) FROM accounts")
    fun getAccountCount(): Flow<Int>
}
