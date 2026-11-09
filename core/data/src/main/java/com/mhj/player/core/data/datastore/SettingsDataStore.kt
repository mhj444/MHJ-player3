package com.mhj.player.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.model.Eq
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mhj_settings",
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val keepNaturalPitch = booleanPreferencesKey("keep_natural_pitch")
        val playbackSpeed = floatPreferencesKey("playback_speed")
        val sleepTimerMinutes = intPreferencesKey("sleep_timer_minutes")
        val sleepAtMs = longPreferencesKey("sleep_at_ms")
        val sleepAtEnd = booleanPreferencesKey("sleep_at_end")
        val dialogueClarity = booleanPreferencesKey("dialogue_clarity")
        val nightModeAudio = booleanPreferencesKey("night_mode_audio")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqGains = stringPreferencesKey("eq_gains_db")
        val eqPreamp = floatPreferencesKey("eq_preamp_db")
        val eqPreset = stringPreferencesKey("eq_preset")
        val showRemaining = booleanPreferencesKey("show_remaining")
        val englishUi = booleanPreferencesKey("english_ui")
    }

    private val playlistOrderKey = stringPreferencesKey("playlist_order")

    /** The order you dragged your own playlists into (playlist ids). */
    val playlistOrder: Flow<List<Long>> = context.settingsDataStore.data.map { p ->
        p[playlistOrderKey].orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }
    }

    suspend fun setPlaylistOrder(ids: List<Long>) {
        context.settingsDataStore.edit { it[playlistOrderKey] = ids.joinToString(",") }
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }
    suspend fun setPlaybackSpeed(speed: Float) = update { it.copy(playbackSpeed = speed.coerceIn(0.25f, 3f)) }
    suspend fun setKeepNaturalPitch(enabled: Boolean) = update { it.copy(keepNaturalPitch = enabled) }
    suspend fun setDialogueClarity(enabled: Boolean) = update { it.copy(dialogueClarity = enabled) }
    suspend fun setNightModeAudio(enabled: Boolean) = update { it.copy(nightModeAudio = enabled) }

    /** Starts (minutes > 0) or stops (0) the sleep timer. */
    suspend fun setSleepTimerMinutes(minutes: Int) = update {
        val m = minutes.coerceIn(0, 180)
        it.copy(
            sleepTimerMinutes = if (m > 0) m else it.sleepTimerMinutes,
            sleepAtMs = if (m > 0) System.currentTimeMillis() + m * 60_000L else 0L,
            sleepAtEnd = false,
        )
    }

    suspend fun setSleepAtEnd(enabled: Boolean) = update { it.copy(sleepAtEnd = enabled, sleepAtMs = 0L) }
    suspend fun clearSleep() = update { it.copy(sleepAtMs = 0L, sleepAtEnd = false) }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.keepNaturalPitch] = next.keepNaturalPitch
            prefs[Keys.playbackSpeed] = next.playbackSpeed
            prefs[Keys.sleepTimerMinutes] = next.sleepTimerMinutes
            prefs[Keys.sleepAtMs] = next.sleepAtMs
            prefs[Keys.sleepAtEnd] = next.sleepAtEnd
            prefs[Keys.dialogueClarity] = next.dialogueClarity
            prefs[Keys.nightModeAudio] = next.nightModeAudio
            prefs[Keys.eqEnabled] = next.eqEnabled
            prefs[Keys.eqGains] = next.eqGains.take(Eq.BANDS).joinToString(",") { v ->
                v.coerceIn(-Eq.MAX_DB, Eq.MAX_DB).toString()
            }
            prefs[Keys.eqPreamp] = next.eqPreamp.coerceIn(-12f, 6f)
            prefs[Keys.eqPreset] = next.eqPreset
            prefs[Keys.showRemaining] = next.showRemaining
            prefs[Keys.englishUi] = next.englishUi
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        theme = runCatching {
            AppTheme.valueOf(this[Keys.theme] ?: AppTheme.MIDNIGHT_GOLD.name)
        }.getOrDefault(AppTheme.MIDNIGHT_GOLD),
        keepNaturalPitch = this[Keys.keepNaturalPitch] ?: true,
        playbackSpeed = this[Keys.playbackSpeed] ?: 1.0f,
        sleepTimerMinutes = this[Keys.sleepTimerMinutes] ?: 30,
        sleepAtMs = this[Keys.sleepAtMs] ?: 0L,
        sleepAtEnd = this[Keys.sleepAtEnd] ?: false,
        dialogueClarity = this[Keys.dialogueClarity] ?: false,
        nightModeAudio = this[Keys.nightModeAudio] ?: false,
        eqEnabled = this[Keys.eqEnabled] ?: true,
        eqGains = parseGains(this[Keys.eqGains]),
        eqPreamp = this[Keys.eqPreamp] ?: 0f,
        eqPreset = this[Keys.eqPreset] ?: "flat",
        showRemaining = this[Keys.showRemaining] ?: false,
        englishUi = this[Keys.englishUi] ?: false,
    )

    private fun parseGains(raw: String?): List<Float> {
        val defaults = List(Eq.BANDS) { 0f }
        if (raw.isNullOrBlank()) return defaults
        val parsed = raw.split(',').mapNotNull { it.trim().toFloatOrNull()?.coerceIn(-Eq.MAX_DB, Eq.MAX_DB) }
        return if (parsed.size >= Eq.BANDS) parsed.take(Eq.BANDS) else defaults
    }
}
