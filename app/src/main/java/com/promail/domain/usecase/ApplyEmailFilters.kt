package com.smartmail.domain.usecase

import com.smartmail.domain.models.Email
import com.smartmail.domain.models.EmailFilter

class ApplyEmailFilters {

    /**
     * Applica tutti i filtri a un'email e ritorna l'email modificata
     */
    fun apply(email: Email, filters: List<EmailFilter>): Email {
        var modifiedEmail = email

        // Ordina per priorità (più basso = più prioritario)
        val sortedFilters = filters.filter { it.isEnabled }.sortedBy { it.priority }

        for (filter in sortedFilters) {
            if (matches(email, filter)) {
                modifiedEmail = applyActions(modifiedEmail, filter)
            }
        }

        return modifiedEmail
    }

    /**
     * Verifica se un'email matcha con le condizioni del filtro
     */
    private fun matches(email: Email, filter: EmailFilter): Boolean {
        // Controlla mittente
        if (filter.fromContains != null) {
            val fromMatch = email.fromAddress.contains(filter.fromContains, ignoreCase = true) ||
                    email.fromName?.contains(filter.fromContains, ignoreCase = true) == true
            if (!fromMatch) return false
        }

        // Controlla oggetto
        if (filter.subjectContains != null) {
            if (!email.subject.contains(filter.subjectContains, ignoreCase = true)) {
                return false
            }
        }

        // Controlla corpo
        if (filter.bodyContains != null) {
            val bodyMatch = email.bodyText?.contains(filter.bodyContains, ignoreCase = true) == true ||
                    email.bodyHtml?.contains(filter.bodyContains, ignoreCase = true) == true
            if (!bodyMatch) return false
        }

        // Controlla destinatario
        if (filter.toContains != null) {
            if (!email.toAddresses.contains(filter.toContains, ignoreCase = true)) {
                return false
            }
        }

        // Controlla allegati
        if (filter.hasAttachments != null) {
            if (email.hasAttachments != filter.hasAttachments) {
                return false
            }
        }

        return true
    }

    /**
     * Applica le azioni del filtro all'email
     */
    private fun applyActions(email: Email, filter: EmailFilter): Email {
        return email.copy(
            smartFolderId = filter.moveToSmartFolderId ?: email.smartFolderId,
            isImportant = if (filter.markAsImportant) true else email.isImportant,
            isStarred = if (filter.markAsStarred) true else email.isStarred,
            isRead = if (filter.markAsRead) true else email.isRead
        )
    }
}
