package com.smartmail.domain.usecases

import com.smartmail.domain.models.Email
import com.smartmail.domain.models.SmartFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Motore di apprendimento automatico per suggerimenti intelligenti.
 * Analizza il comportamento dell'utente per migliorare la categorizzazione.
 */
class SmartLearningEngine {

    // Cache delle azioni dell'utente per pattern learning
    private val userActionHistory = ConcurrentHashMap<String, MutableList<UserAction>>()

    // Pattern appresi (sender -> categoria preferita)
    private val learnedPatterns = ConcurrentHashMap<String, CategoryPattern>()

    // Soglia per suggerire un nuovo filtro (numero minimo di azioni simili)
    private val suggestionThreshold = 3

    /**
     * Registra un'azione dell'utente (spostamento manuale, star, ecc.)
     */
    fun recordUserAction(
        email: Email,
        targetFolder: SmartFolder?,
        actionType: ActionType
    ) {
        val action = UserAction(
            senderEmail = email.fromAddress,
            senderDomain = email.fromAddress.substringAfter("@", ""),
            targetFolderId = targetFolder?.id,
            targetFolderName = targetFolder?.name,
            actionType = actionType,
            timestamp = System.currentTimeMillis(),
            subjectKeywords = extractKeywords(email.subject),
            bodyKeywords = extractKeywords(email.bodyText ?: "")
        )

        val key = email.fromAddress
        userActionHistory.getOrPut(key) { mutableListOf() }.add(action)

        // Aggiorna i pattern appresi
        updateLearnedPatterns(key)
    }

    /**
     * Suggerisce automaticamente una categoria per una nuova email
     */
    fun suggestCategory(email: Email): CategorySuggestion? {
        // 1. Controlla pattern appresi per questo sender esatto
        learnedPatterns[email.fromAddress]?.let { pattern ->
            if (pattern.confidence > 0.7f) {
                return CategorySuggestion(
                    folderName = pattern.preferredFolderName,
                    confidence = pattern.confidence,
                    reason = "Hai sempre spostato le email da ${email.fromName ?: email.fromAddress} in questa categoria"
                )
            }
        }

        // 2. Controlla pattern per il dominio
        val domain = email.fromAddress.substringAfter("@", "")
        learnedPatterns.values
            .filter { it.senderDomain == domain && it.confidence > 0.6f }
            .maxByOrNull { it.confidence }
            ?.let { pattern ->
                return CategorySuggestion(
                    folderName = pattern.preferredFolderName,
                    confidence = pattern.confidence,
                    reason = "Le email da $domain vengono solitamente categorizzate qui"
                )
            }

        // 3. Analizza keywords nel subject/body
        val subjectKeywords = extractKeywords(email.subject)
        val matchingPatterns = learnedPatterns.values.filter { pattern ->
            pattern.commonKeywords.any { it in subjectKeywords }
        }

        if (matchingPatterns.isNotEmpty()) {
            val bestMatch = matchingPatterns.maxByOrNull { it.confidence }!!
            if (bestMatch.confidence > 0.5f) {
                return CategorySuggestion(
                    folderName = bestMatch.preferredFolderName,
                    confidence = bestMatch.confidence * 0.8f, // Ridotta perché basata su keywords
                    reason = "Le email simili vengono categorizzate qui"
                )
            }
        }

        return null
    }

    /**
     * Genera suggerimenti per nuovi filtri automatici
     */
    fun generateFilterSuggestions(): List<FilterSuggestion> {
        val suggestions = mutableListOf<FilterSuggestion>()

        // Analizza gli action history per trovare pattern ripetitivi
        userActionHistory.forEach { (sender, actions) ->
            if (actions.size >= suggestionThreshold) {
                // Controlla se l'utente sposta sempre le email da questo sender nella stessa categoria
                val folderCounts = actions.groupingBy { it.targetFolderName }.eachCount()
                val mostCommon = folderCounts.maxByOrNull { it.value }

                if (mostCommon != null && mostCommon.value >= suggestionThreshold) {
                    val confidence = mostCommon.value.toFloat() / actions.size

                    if (confidence > 0.75f) {
                        suggestions.add(
                            FilterSuggestion(
                                title = "Automatizza categorizzazione per ${extractDisplayName(sender)}",
                                description = "Hai spostato ${mostCommon.value} email da questo mittente in '${mostCommon.key}'. Vuoi creare un filtro automatico?",
                                filterRule = """{"type":"fromAddress","addresses":["$sender"]}""",
                                targetFolderName = mostCommon.key ?: "",
                                confidence = confidence,
                                priority = SuggestionPriority.HIGH
                            )
                        )
                    }
                }
            }
        }

        // Analizza domini frequenti
        val domainActions = mutableMapOf<String, MutableList<UserAction>>()
        userActionHistory.values.flatten().forEach { action ->
            domainActions.getOrPut(action.senderDomain) { mutableListOf() }.add(action)
        }

        domainActions.forEach { (domain, actions) ->
            if (actions.size >= suggestionThreshold * 2) {
                val folderCounts = actions.groupingBy { it.targetFolderName }.eachCount()
                val mostCommon = folderCounts.maxByOrNull { it.value }

                if (mostCommon != null && mostCommon.value >= suggestionThreshold * 2) {
                    val confidence = mostCommon.value.toFloat() / actions.size

                    if (confidence > 0.7f) {
                        suggestions.add(
                            FilterSuggestion(
                                title = "Filtro automatico per @$domain",
                                description = "Hai categorizzato ${mostCommon.value} email da $domain in '${mostCommon.key}'. Vuoi creare un filtro per questo dominio?",
                                filterRule = """{"type":"fromDomain","domains":["$domain"]}""",
                                targetFolderName = mostCommon.key ?: "",
                                confidence = confidence,
                                priority = SuggestionPriority.MEDIUM
                            )
                        )
                    }
                }
            }
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    /**
     * Rileva email "rumore" (lette raramente, bassa priorità)
     */
    fun detectNoiseEmails(emails: List<Email>): List<Email> {
        // Email con "unsubscribe" nel body che non sono mai state lette
        val newsletterNoise = emails.filter { email ->
            val body = email.bodyText ?: email.bodyHtml ?: ""
            !email.isRead &&
            (body.contains("unsubscribe", ignoreCase = true) ||
             body.contains("disiscrivi", ignoreCase = true))
        }

        return newsletterNoise
    }

    /**
     * Suggerisce mittenti VIP in base alla frequenza di interazione
     */
    fun suggestVIPSenders(): List<VIPSuggestion> {
        val suggestions = mutableListOf<VIPSuggestion>()

        // Analizza quante volte l'utente ha segnato email da un sender come importanti
        val importantActions = userActionHistory.values.flatten()
            .filter { it.actionType == ActionType.MARK_IMPORTANT }
            .groupBy { it.senderEmail }

        importantActions.forEach { (sender, actions) ->
            if (actions.size >= 2) {
                suggestions.add(
                    VIPSuggestion(
                        senderEmail = sender,
                        senderName = extractDisplayName(sender),
                        reason = "Hai segnato ${actions.size} email da questo mittente come importanti",
                        confidence = (actions.size / 5f).coerceAtMost(1f)
                    )
                )
            }
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    /**
     * Analizza il tempo migliore per inviare notifiche (quiet hours)
     */
    fun analyzeQuietHoursPattern(): QuietHoursPattern {
        // TODO: Implementare analisi del comportamento utente per identificare
        // quando l'utente tende a non leggere le email (ore di sonno, lavoro intenso, ecc.)

        return QuietHoursPattern(
            startHour = 22, // 10 PM
            endHour = 7,    // 7 AM
            confidence = 0.8f,
            reason = "Pattern tipico: nessuna attività di notte"
        )
    }

    // === HELPER FUNCTIONS ===

    private fun updateLearnedPatterns(senderEmail: String) {
        val actions = userActionHistory[senderEmail] ?: return

        if (actions.size < 2) return

        // Calcola la categoria più frequente
        val folderCounts = actions.groupingBy { it.targetFolderName }.eachCount()
        val mostCommon = folderCounts.maxByOrNull { it.value } ?: return

        val confidence = mostCommon.value.toFloat() / actions.size

        // Estrai keywords comuni
        val allKeywords = actions.flatMap { it.subjectKeywords + it.bodyKeywords }
        val keywordFrequency = allKeywords.groupingBy { it }.eachCount()
        val commonKeywords = keywordFrequency.filter { it.value >= 2 }.keys.toList()

        learnedPatterns[senderEmail] = CategoryPattern(
            senderEmail = senderEmail,
            senderDomain = senderEmail.substringAfter("@", ""),
            preferredFolderName = mostCommon.key ?: "",
            confidence = confidence,
            commonKeywords = commonKeywords,
            sampleCount = actions.size
        )
    }

    private fun extractKeywords(text: String): List<String> {
        // Estrae parole significative (> 3 caratteri, non stop words)
        val stopWords = setOf("the", "and", "or", "but", "in", "on", "at", "to", "for",
            "di", "da", "in", "con", "su", "per", "tra", "fra", "a", "il", "lo", "la", "i", "gli", "le")

        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9àèéìòù]+"))
            .filter { it.length > 3 && it !in stopWords }
            .distinct()
            .take(10) // Massimo 10 keywords per email
    }

    private fun extractDisplayName(email: String): String {
        return email.substringBefore("@").replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }
    }
}

// === DATA CLASSES ===

data class UserAction(
    val senderEmail: String,
    val senderDomain: String,
    val targetFolderId: Long?,
    val targetFolderName: String?,
    val actionType: ActionType,
    val timestamp: Long,
    val subjectKeywords: List<String>,
    val bodyKeywords: List<String>
)

enum class ActionType {
    MOVE_TO_FOLDER,
    MARK_IMPORTANT,
    MARK_READ,
    ARCHIVE,
    DELETE,
    STAR
}

data class CategoryPattern(
    val senderEmail: String,
    val senderDomain: String,
    val preferredFolderName: String,
    val confidence: Float, // 0.0 - 1.0
    val commonKeywords: List<String>,
    val sampleCount: Int
)

data class CategorySuggestion(
    val folderName: String,
    val confidence: Float,
    val reason: String
)

data class FilterSuggestion(
    val title: String,
    val description: String,
    val filterRule: String, // JSON
    val targetFolderName: String,
    val confidence: Float,
    val priority: SuggestionPriority
)

enum class SuggestionPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class VIPSuggestion(
    val senderEmail: String,
    val senderName: String,
    val reason: String,
    val confidence: Float
)

data class QuietHoursPattern(
    val startHour: Int, // 0-23
    val endHour: Int,   // 0-23
    val confidence: Float,
    val reason: String
)
