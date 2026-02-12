package com.smartmail.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_filters")
data class EmailFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nome del filtro (es: "Fatture", "Newsletter", "Lavoro Urgente")
    val name: String,

    // Attivo/disattivo
    val isEnabled: Boolean = true,

    // === CONDIZIONI ===

    // Filtro per mittente (contiene)
    val fromContains: String? = null,

    // Filtro per oggetto (contiene)
    val subjectContains: String? = null,

    // Filtro per corpo email (contiene)
    val bodyContains: String? = null,

    // Filtro per destinatario
    val toContains: String? = null,

    // Ha allegati
    val hasAttachments: Boolean? = null,

    // === AZIONI ===

    // Sposta in smart folder (ID)
    val moveToSmartFolderId: Long? = null,

    // Marca come importante
    val markAsImportant: Boolean = false,

    // Marca come stella
    val markAsStarred: Boolean = false,

    // Marca come letto
    val markAsRead: Boolean = false,

    // Priorità del filtro (più basso = più prioritario)
    val priority: Int = 100,

    // Data creazione
    val createdAt: Long = System.currentTimeMillis()
)
