package com.smartmail.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Credenziali (password criptata)
    val emailAddress: String,
    val displayName: String?,
    val encryptedPassword: String,
    
    // Tipo di account
    val accountType: AccountType,
    
    // Configurazione IMAP
    val imapHost: String,
    val imapPort: Int,
    val imapUseSsl: Boolean = true,
    
    // Configurazione SMTP (per invio)
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUseSsl: Boolean = true,
    
    // Sync settings
    val syncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val lastSyncTimestamp: Long = 0,
    
    // UI
    val color: String, // Hex color per visualizzazione
    val isDefault: Boolean = false,
    
    // Stato
    val isActive: Boolean = true
)

enum class AccountType {
    GMAIL,
    VIRGILIO,
    OUTLOOK,
    YAHOO,
    IMAP_CUSTOM
}

// Preset di configurazione per provider comuni
object AccountPresets {
    fun getImapConfig(type: AccountType): ImapConfig {
        return when (type) {
            AccountType.GMAIL -> ImapConfig(
                imapHost = "imap.gmail.com",
                imapPort = 993,
                smtpHost = "smtp.gmail.com",
                smtpPort = 587
            )
            AccountType.VIRGILIO -> ImapConfig(
                imapHost = "in.virgilio.it",
                imapPort = 993,
                smtpHost = "out.virgilio.it",
                smtpPort = 465
            )
            AccountType.OUTLOOK -> ImapConfig(
                imapHost = "outlook.office365.com",
                imapPort = 993,
                smtpHost = "smtp-mail.outlook.com",
                smtpPort = 587
            )
            AccountType.YAHOO -> ImapConfig(
                imapHost = "imap.mail.yahoo.com",
                imapPort = 993,
                smtpHost = "smtp.mail.yahoo.com",
                smtpPort = 587
            )
            AccountType.IMAP_CUSTOM -> ImapConfig(
                imapHost = "",
                imapPort = 993,
                smtpHost = "",
                smtpPort = 587
            )
        }
    }
}

data class ImapConfig(
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int
)
