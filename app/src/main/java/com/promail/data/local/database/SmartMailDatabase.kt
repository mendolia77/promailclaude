package com.smartmail.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartmail.data.local.database.dao.AccountDao
import com.smartmail.data.local.database.dao.DraftDao
import com.smartmail.data.local.database.dao.EmailDao
import com.smartmail.data.local.database.dao.EmailFilterDao
import com.smartmail.data.local.database.dao.SmartFolderDao
import com.smartmail.domain.models.Account
import com.smartmail.domain.models.Draft
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.EmailFilter
import com.smartmail.domain.models.SmartFolder
import com.smartmail.domain.models.SmartFolderPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Email::class, SmartFolder::class, EmailFilter::class, Draft::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartMailDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun emailDao(): EmailDao
    abstract fun smartFolderDao(): SmartFolderDao
    abstract fun emailFilterDao(): EmailFilterDao
    abstract fun draftDao(): DraftDao

    companion object {
        private const val DATABASE_NAME = "smartmail_db"

        @Volatile
        private var INSTANCE: SmartMailDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crea la tabella drafts
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS drafts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        toAddresses TEXT NOT NULL,
                        ccAddresses TEXT,
                        bccAddresses TEXT,
                        subject TEXT NOT NULL,
                        bodyText TEXT,
                        bodyHtml TEXT,
                        createdDate INTEGER NOT NULL,
                        lastModifiedDate INTEGER NOT NULL,
                        inReplyToEmailId INTEGER,
                        isForward INTEGER NOT NULL,
                        attachmentsJson TEXT,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """)

                // Crea indice per accountId
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_drafts_accountId ON drafts(accountId)
                """)
            }
        }

        fun getInstance(context: Context): SmartMailDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SmartMailDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmartMailDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration() // Solo per versioni non migrate
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Inserisce le smart folder predefinite al primo avvio
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).smartFolderDao()
                                .insertAll(SmartFolderPresets.getDefaultFolders())
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Assicura che le cartelle predefinite esistano sempre (solo se non esistono)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).smartFolderDao()

                            // Rimuovi eventuali duplicati esistenti
                            dao.removeDuplicates()

                            // Controlla se esistono già cartelle di sistema
                            val cursor = db.query("SELECT COUNT(*) FROM smart_folders WHERE isSystemFolder = 1")
                            cursor.moveToFirst()
                            val systemFolderCount = cursor.getInt(0)
                            cursor.close()

                            // Inserisci solo se non ci sono cartelle di sistema
                            if (systemFolderCount == 0) {
                                dao.insertAll(SmartFolderPresets.getDefaultFolders())
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
