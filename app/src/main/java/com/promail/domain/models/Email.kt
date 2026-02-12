package com.smartmail.domain.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "emails",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("messageId", unique = true),
        Index("folderName"),
        Index("receivedDate"),
        Index("isRead"),
        Index("smartFolderId")
    ]
)
data class Email(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Identificativo unico del messaggio (Message-ID header)
    val messageId: String,
    
    // Account associato
    val accountId: Long,
    
    // Informazioni mittente
    val fromAddress: String,
    val fromName: String?,
    
    // Destinatari
    val toAddresses: String, // JSON array of addresses
    val ccAddresses: String? = null,
    val bccAddresses: String? = null,
    
    // Contenuto
    val subject: String,
    val bodyText: String?,
    val bodyHtml: String?,
    
    // Metadati
    val receivedDate: Date,
    val sentDate: Date?,
    val size: Int, // in bytes
    
    // Flags
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val isImportant: Boolean = false,
    val isDraft: Boolean = false,
    
    // Folder
    val folderName: String = "INBOX",
    val smartFolderId: Long? = null, // ID del filtro smart applicato
    
    // Allegati
    val hasAttachments: Boolean = false,
    val attachmentsJson: String? = null, // JSON array of attachments
    
    // Thread
    val threadId: String? = null,
    
    // Sync
    val locallyDeleted: Boolean = false,
    val syncedToServer: Boolean = true
)

data class EmailAttachment(
    val filename: String,
    val mimeType: String,
    val size: Int,
    val localPath: String? = null
)
