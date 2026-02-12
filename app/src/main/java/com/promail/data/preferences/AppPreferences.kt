package com.smartmail.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "smartmail_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val DEFAULT_SYNC_INTERVAL = 15L // 15 minuti di default
    }

    // Intervallo di sincronizzazione in minuti
    var syncIntervalMinutes: Long
        get() = prefs.getLong(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL)
        set(value) = prefs.edit { putLong(KEY_SYNC_INTERVAL_MINUTES, value) }

    // Opzioni predefinite per l'intervallo
    fun getSyncIntervalOptions(): List<SyncInterval> = listOf(
        SyncInterval(1, "1 minuto"),
        SyncInterval(5, "5 minuti"),
        SyncInterval(15, "15 minuti"),
        SyncInterval(30, "30 minuti"),
        SyncInterval(60, "1 ora"),
        SyncInterval(120, "2 ore"),
        SyncInterval(360, "6 ore")
    )

    fun getSyncIntervalLabel(): String {
        val interval = syncIntervalMinutes
        return when {
            interval < 60 -> "$interval minuti"
            interval == 60L -> "1 ora"
            interval < 1440 -> "${interval / 60} ore"
            else -> "${interval / 1440} giorni"
        }
    }
}

data class SyncInterval(
    val minutes: Long,
    val label: String
)
