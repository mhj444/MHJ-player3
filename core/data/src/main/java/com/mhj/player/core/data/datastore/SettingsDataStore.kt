package com.mhj.player.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
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
        val dialogueClarity = booleanPreferencesKey("dialogue_clarity")
        val nightModeAudio = booleanPreferencesKey("night_mode_audio")
        val eqBands = stringPreferencesKey("eq_bands")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[Keys.playbackSpeed] = speed }
    }

    suspend fun setKeepNaturalPitch(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.keepNaturalPitch] = enabled }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.sleepTimerMinutes] = minutes }
    }

    suspend fun setDialogueClarity(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dialogueClarity] = enabled }
    }

    suspend fun setNightModeAudio(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.nightModeAudio] = enabled }
    }

    suspend fun setEqBands(bands: List<Float>) {
        context.settingsDataStore.edit {
            it[Keys.eqBands] = bands.take(5).joinToString(",") { v ->
                v.coerceIn(-1f, 1f).toString()
            }
        }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.keepNaturalPitch] = next.keepNaturalPitch
            prefs[Keys.playbackSpeed] = next.playbackSpeed
            prefs[Keys.sleepTimerMinutes] = next.sleepTimerMinutes
            prefs[Keys.dialogueClarity] = next.dialogueClarity
            prefs[Keys.nightModeAudio] = next.nightModeAudio
            prefs[Keys.eqBands] = next.eqBands.take(5).joinToString(",") { v ->
                v.coerceIn(-1f, 1f).toString()
            }
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        theme = runCatching {
            AppTheme.valueOf(this[Keys.theme] ?: AppTheme.MIDNIGHT_GOLD.name)
        }.getOrDefault(AppTheme.MIDNIGHT_GOLD),
        keepNaturalPitch = this[Keys.keepNaturalPitch] ?: true,
        playbackSpeed = this[Keys.playbackSpeed] ?: 1.0f,
        sleepTimerMinutes = this[Keys.sleepTimerMinutes] ?: 0,
        dialogueClarity = this[Keys.dialogueClarity] ?: false,
        nightModeAudio = this[Keys.nightModeAudio] ?: false,
        eqBands = parseBands(this[Keys.eqBands]),
    )

    private fun parseBands(raw: String?): List<Float> {
        val defaults = listOf(0f, 0f, 0f, 0f, 0f)
        if (raw.isNullOrBlank()) return defaults
        val parsed = raw.split(',').mapNotNull { it.trim().toFloatOrNull()?.coerceIn(-1f, 1f) }
        return if (parsed.size >= 5) parsed.take(5) else defaults
    }
}
