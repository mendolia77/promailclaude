package com.smartmail.domain.models

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_folders")
data class SmartFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val icon: String, // Emoji or material icon name
    val colorHex: String,
    
    // Regole di filtro (JSON)
    val rulesJson: String,
    
    // Impostazioni
    val notificationsEnabled: Boolean = true,
    val isSystemFolder: Boolean = false, // true per cartelle predefinite
    val sortOrder: Int = 0,
    
    val isActive: Boolean = true
)

// Regole di filtro
sealed class FilterRule {
    // Mittente
    data class FromAddress(val addresses: List<String>) : FilterRule()
    data class FromDomain(val domains: List<String>) : FilterRule()
    data class FromName(val names: List<String>) : FilterRule()
    
    // Oggetto e corpo
    data class SubjectContains(val keywords: List<String>, val caseSensitive: Boolean = false) : FilterRule()
    data class BodyContains(val keywords: List<String>, val caseSensitive: Boolean = false) : FilterRule()
    
    // Allegati
    data class HasAttachment(val hasIt: Boolean = true) : FilterRule()
    data class AttachmentType(val mimeTypes: List<String>) : FilterRule()
    data class AttachmentName(val patterns: List<String>) : FilterRule()
    
    // Dimensione
    data class SizeGreaterThan(val sizeKb: Int) : FilterRule()
    data class SizeLessThan(val sizeKb: Int) : FilterRule()
    
    // Date
    data class ReceivedAfter(val timestamp: Long) : FilterRule()
    data class ReceivedBefore(val timestamp: Long) : FilterRule()
    
    // Flags
    data class IsStarred(val starred: Boolean = true) : FilterRule()
    data class IsRead(val read: Boolean) : FilterRule()
    data class IsImportant(val important: Boolean = true) : FilterRule()
    
    // Logici
    data class And(val rules: List<FilterRule>) : FilterRule()
    data class Or(val rules: List<FilterRule>) : FilterRule()
    data class Not(val rule: FilterRule) : FilterRule()
}

// Preset di cartelle intelligenti predefinite
object SmartFolderPresets {
    fun getDefaultFolders(): List<SmartFolder> {
        return listOf(
            SmartFolder(
                name = "Lavoro",
                icon = "🏢",
                colorHex = "#2196F3",
                rulesJson = """{"type":"or","rules":[{"type":"fromDomain","domains":["azienda.com","work.com"]}]}""",
                isSystemFolder = true,
                sortOrder = 1
            ),
            SmartFolder(
                name = "Personale",
                icon = "👥",
                colorHex = "#4CAF50",
                rulesJson = """{"type":"not","rule":{"type":"or","rules":[{"type":"fromDomain","domains":["@"]},{"type":"bodyContains","keywords":["unsubscribe"]}]}}""",
                isSystemFolder = true,
                sortOrder = 2
            ),
            SmartFolder(
                name = "Importanti",
                icon = "⭐",
                colorHex = "#FF5722",
                rulesJson = """{"type":"or","rules":[{"type":"isStarred","starred":true},{"type":"isImportant","important":true}]}""",
                isSystemFolder = true,
                sortOrder = 3
            ),
            SmartFolder(
                name = "Con allegati",
                icon = "📎",
                colorHex = "#9C27B0",
                rulesJson = """{"type":"hasAttachment","hasIt":true}""",
                isSystemFolder = true,
                sortOrder = 4
            ),
            SmartFolder(
                name = "Shopping",
                icon = "🛒",
                colorHex = "#FF9800",
                rulesJson = """{"type":"or","rules":[{"type":"subjectContains","keywords":["ordine","acquisto","spedizione","tracking"]},{"type":"bodyContains","keywords":["ordine","acquisto","spedizione"]}]}""",
                isSystemFolder = true,
                sortOrder = 5
            ),
            SmartFolder(
                name = "Newsletter",
                icon = "📰",
                colorHex = "#607D8B",
                rulesJson = """{"type":"bodyContains","keywords":["unsubscribe","disiscrivi","newsletter"]}""",
                isSystemFolder = true,
                sortOrder = 6
            )
        )
    }
}
