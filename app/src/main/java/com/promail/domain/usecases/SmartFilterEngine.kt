package com.smartmail.domain.usecases

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smartmail.domain.models.Email
import com.smartmail.domain.models.EmailAttachment
import com.smartmail.domain.models.FilterRule
import com.smartmail.domain.models.SmartFolder

/**
 * Motore per valutare i filtri intelligenti sulle email.
 * Parsa le regole JSON delle SmartFolder e le applica alle email.
 */
class SmartFilterEngine {

    private val gson = Gson()

    /**
     * Valuta tutte le smart folder per un'email e restituisce quelle corrispondenti.
     */
    fun matchFolders(email: Email, folders: List<SmartFolder>): List<SmartFolder> {
        return folders.filter { folder ->
            try {
                val rule = parseRule(folder.rulesJson)
                rule != null && evaluateRule(rule, email)
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Valuta se un'email corrisponde a una singola smart folder.
     */
    fun matches(email: Email, folder: SmartFolder): Boolean {
        return try {
            val rule = parseRule(folder.rulesJson)
            rule != null && evaluateRule(rule, email)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parsa una stringa JSON in una FilterRule.
     */
    fun parseRule(json: String): FilterRule? {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            parseRuleObject(obj)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Valuta una FilterRule su un'email.
     */
    fun evaluateRule(rule: FilterRule, email: Email): Boolean {
        return when (rule) {
            // === Filtri mittente ===
            is FilterRule.FromAddress -> {
                rule.addresses.any { addr ->
                    email.fromAddress.equals(addr, ignoreCase = true)
                }
            }
            is FilterRule.FromDomain -> {
                val emailDomain = email.fromAddress.substringAfter("@", "")
                rule.domains.any { domain ->
                    emailDomain.equals(domain, ignoreCase = true)
                }
            }
            is FilterRule.FromName -> {
                val fromName = email.fromName ?: ""
                rule.names.any { name ->
                    fromName.contains(name, ignoreCase = true)
                }
            }

            // === Filtri contenuto ===
            is FilterRule.SubjectContains -> {
                rule.keywords.any { keyword ->
                    email.subject.contains(keyword, ignoreCase = !rule.caseSensitive)
                }
            }
            is FilterRule.BodyContains -> {
                val body = email.bodyText ?: email.bodyHtml ?: ""
                rule.keywords.any { keyword ->
                    body.contains(keyword, ignoreCase = !rule.caseSensitive)
                }
            }

            // === Filtri allegati ===
            is FilterRule.HasAttachment -> {
                email.hasAttachments == rule.hasIt
            }
            is FilterRule.AttachmentType -> {
                val attachments = parseAttachments(email.attachmentsJson)
                rule.mimeTypes.any { mime ->
                    attachments.any { it.mimeType.startsWith(mime, ignoreCase = true) }
                }
            }
            is FilterRule.AttachmentName -> {
                val attachments = parseAttachments(email.attachmentsJson)
                rule.patterns.any { pattern ->
                    attachments.any { att ->
                        att.filename.contains(pattern, ignoreCase = true)
                    }
                }
            }

            // === Filtri dimensione ===
            is FilterRule.SizeGreaterThan -> email.size > rule.sizeKb * 1024
            is FilterRule.SizeLessThan -> email.size < rule.sizeKb * 1024

            // === Filtri data ===
            is FilterRule.ReceivedAfter -> email.receivedDate.time > rule.timestamp
            is FilterRule.ReceivedBefore -> email.receivedDate.time < rule.timestamp

            // === Filtri flag ===
            is FilterRule.IsStarred -> email.isStarred == rule.starred
            is FilterRule.IsRead -> email.isRead == rule.read
            is FilterRule.IsImportant -> email.isImportant == rule.important

            // === Operatori logici ===
            is FilterRule.And -> rule.rules.all { evaluateRule(it, email) }
            is FilterRule.Or -> rule.rules.any { evaluateRule(it, email) }
            is FilterRule.Not -> !evaluateRule(rule.rule, email)
        }
    }

    // ==========================================
    // PARSING JSON -> FilterRule
    // ==========================================

    private fun parseRuleObject(obj: JsonObject): FilterRule? {
        val type = obj.get("type")?.asString ?: return null

        return when (type) {
            // Mittente
            "fromAddress" -> FilterRule.FromAddress(
                addresses = obj.getAsJsonArray("addresses")?.map { it.asString } ?: emptyList()
            )
            "fromDomain" -> FilterRule.FromDomain(
                domains = obj.getAsJsonArray("domains")?.map { it.asString } ?: emptyList()
            )
            "fromName" -> FilterRule.FromName(
                names = obj.getAsJsonArray("names")?.map { it.asString } ?: emptyList()
            )

            // Contenuto
            "subjectContains" -> FilterRule.SubjectContains(
                keywords = obj.getAsJsonArray("keywords")?.map { it.asString } ?: emptyList(),
                caseSensitive = obj.get("caseSensitive")?.asBoolean ?: false
            )
            "bodyContains" -> FilterRule.BodyContains(
                keywords = obj.getAsJsonArray("keywords")?.map { it.asString } ?: emptyList(),
                caseSensitive = obj.get("caseSensitive")?.asBoolean ?: false
            )

            // Allegati
            "hasAttachment" -> FilterRule.HasAttachment(
                hasIt = obj.get("hasIt")?.asBoolean ?: true
            )
            "attachmentType" -> FilterRule.AttachmentType(
                mimeTypes = obj.getAsJsonArray("mimeTypes")?.map { it.asString } ?: emptyList()
            )
            "attachmentName" -> FilterRule.AttachmentName(
                patterns = obj.getAsJsonArray("patterns")?.map { it.asString } ?: emptyList()
            )

            // Dimensione
            "sizeGreaterThan" -> FilterRule.SizeGreaterThan(
                sizeKb = obj.get("sizeKb")?.asInt ?: 0
            )
            "sizeLessThan" -> FilterRule.SizeLessThan(
                sizeKb = obj.get("sizeKb")?.asInt ?: 0
            )

            // Date
            "receivedAfter" -> FilterRule.ReceivedAfter(
                timestamp = obj.get("timestamp")?.asLong ?: 0
            )
            "receivedBefore" -> FilterRule.ReceivedBefore(
                timestamp = obj.get("timestamp")?.asLong ?: 0
            )

            // Flag
            "isStarred" -> FilterRule.IsStarred(
                starred = obj.get("starred")?.asBoolean ?: true
            )
            "isRead" -> FilterRule.IsRead(
                read = obj.get("read")?.asBoolean ?: true
            )
            "isImportant" -> FilterRule.IsImportant(
                important = obj.get("important")?.asBoolean ?: true
            )

            // Logici
            "and" -> {
                val rules = obj.getAsJsonArray("rules")?.mapNotNull {
                    parseRuleObject(it.asJsonObject)
                } ?: emptyList()
                FilterRule.And(rules)
            }
            "or" -> {
                val rules = obj.getAsJsonArray("rules")?.mapNotNull {
                    parseRuleObject(it.asJsonObject)
                } ?: emptyList()
                FilterRule.Or(rules)
            }
            "not" -> {
                val inner = obj.getAsJsonObject("rule")?.let { parseRuleObject(it) }
                    ?: return null
                FilterRule.Not(inner)
            }

            else -> null
        }
    }

    private fun parseAttachments(json: String?): List<EmailAttachment> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<EmailAttachment>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
