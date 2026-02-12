package com.smartmail.domain.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "drafts",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val accountId: Long,

    // Destinatari
    val toAddresses: String, // JSON array
    val ccAddresses: String? = null,
    val bccAddresses: String? = null,

    // Contenuto
    val subject: String,
    val bodyText: String?,
    val bodyHtml: String?,

    // Metadata
    val createdDate: Date,
    val lastModifiedDate: Date,

    // Reply/Forward context
    val inReplyToEmailId: Long? = null, // ID dell'email a cui si risponde
    val isForward: Boolean = false,

    // Allegati (JSON array of attachment metadata)
    val attachmentsJson: String? = null
)
