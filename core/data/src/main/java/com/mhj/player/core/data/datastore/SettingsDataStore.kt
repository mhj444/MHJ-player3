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
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            theme = runCatching {
                AppTheme.valueOf(prefs[Keys.theme] ?: AppTheme.MIDNIGHT_GOLD.name)
            }.getOrDefault(AppTheme.MIDNIGHT_GOLD),
            keepNaturalPitch = prefs[Keys.keepNaturalPitch] ?: true,
            playbackSpeed = prefs[Keys.playbackSpeed] ?: 1.0f,
            sleepTimerMinutes = prefs[Keys.sleepTimerMinutes] ?: 0,
            dialogueClarity = prefs[Keys.dialogueClarity] ?: false,
            nightModeAudio = prefs[Keys.nightModeAudio] ?: false,
        )
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

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = AppSettings(
                theme = runCatching {
                    AppTheme.valueOf(prefs[Keys.theme] ?: AppTheme.MIDNIGHT_GOLD.name)
                }.getOrDefault(AppTheme.MIDNIGHT_GOLD),
                keepNaturalPitch = prefs[Keys.keepNaturalPitch] ?: true,
                playbackSpeed = prefs[Keys.playbackSpeed] ?: 1.0f,
                sleepTimerMinutes = prefs[Keys.sleepTimerMinutes] ?: 0,
                dialogueClarity = prefs[Keys.dialogueClarity] ?: false,
                nightModeAudio = prefs[Keys.nightModeAudio] ?: false,
            )
            val next = transform(current)
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.keepNaturalPitch] = next.keepNaturalPitch
            prefs[Keys.playbackSpeed] = next.playbackSpeed
            prefs[Keys.sleepTimerMinutes] = next.sleepTimerMinutes
            prefs[Keys.dialogueClarity] = next.dialogueClarity
            prefs[Keys.nightModeAudio] = next.nightModeAudio
        }
    }
}
