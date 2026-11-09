package com.mhj.player.core.playback.engine

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Real Android [Equalizer] + [BassBoost] bound to the ExoPlayer audio session
 * for the whole time audio is playing. In-app band levels call [Equalizer.setBandLevel].
 */
class AudioEffectsController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var sessionId: Int = 0
    private var nightMode: Boolean = false
    /** Five normalized band gains in -1f..+1f (mapped onto the device millibel range). */
    private var bands: FloatArray = FloatArray(BAND_COUNT) { 0f }
    private var releasedForSystemPanel: Boolean = false

    fun bindSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == sessionId && equalizer != null && !releasedForSystemPanel) {
            applyBands()
            return
        }
        releaseEffectsOnly()
        sessionId = audioSessionId
        releasedForSystemPanel = false
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer unavailable", t)
            equalizer = null
        }
        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost unavailable", t)
            bassBoost = null
        }
        applyBands()
    }

    fun currentSessionId(): Int = sessionId

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        applyBands()
    }

    fun setBandLevels(normalized: FloatArray) {
        val copy = FloatArray(BAND_COUNT) { i ->
            normalized.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
        }
        bands = copy
        applyBands()
    }

    fun currentBands(): FloatArray = bands.copyOf()

    fun applyPreset(name: String) {
        bands = when (name.lowercase()) {
            "bass" -> floatArrayOf(0.75f, 0.45f, 0f, -0.1f, -0.15f)
            "treble" -> floatArrayOf(-0.2f, -0.05f, 0.15f, 0.45f, 0.7f)
            "vocal" -> floatArrayOf(-0.25f, 0.15f, 0.55f, 0.35f, 0.05f)
            "flat" -> FloatArray(BAND_COUNT) { 0f }
            else -> FloatArray(BAND_COUNT) { 0f }
        }
        applyBands()
    }

    /**
     * OPEN_SESSION broadcast + DISPLAY panel. Does **not** send CLOSE.
     * Temporarily releases our Equalizer so OEM panels (MusicFX) can attach
     * to the live session; caller should [restoreAfterSystemPanel] on resume.
     */
    fun buildEqualizerIntents(packageName: String): List<Intent> {
        val session = sessionId
        if (session <= 0) return emptyList()
        // Free the session for the system panel — do not CLOSE_AUDIO_EFFECT_CONTROL_SESSION
        releaseEffectsOnly()
        releasedForSystemPanel = true
        val openSession = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        val display = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            // No FLAG_ACTIVITY_NEW_TASK — caller is an Activity; NEW_TASK can make MusicFX finish
        }
        return listOf(openSession, display)
    }

    fun restoreAfterSystemPanel() {
        if (sessionId <= 0) return
        releasedForSystemPanel = false
        bindSession(sessionId)
    }

    /** Keep session advertised while playing (never paired with CLOSE on open). */
    fun openSessionBroadcast(packageName: String): Intent? {
        val session = sessionId
        if (session <= 0) return null
        return Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
    }

    private fun applyBands() {
        val eq = equalizer ?: return
        try {
            val n = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val lo = range[0].toInt()
            val hi = range[1].toInt()
            // Map our 5 UI bands onto whatever the device exposes
            for (i in 0 until n) {
                val src = if (n == BAND_COUNT) {
                    bands[i]
                } else {
                    val t = i / (n - 1f).coerceAtLeast(1f)
                    val f = t * (BAND_COUNT - 1)
                    val a = f.toInt().coerceIn(0, BAND_COUNT - 1)
                    val b = (a + 1).coerceAtMost(BAND_COUNT - 1)
                    val frac = f - a
                    bands[a] * (1f - frac) + bands[b] * frac
                }
                var level = (lo + (hi - lo) * ((src + 1f) / 2f)).toInt()
                if (nightMode && i == 0) level = (level + (hi * 0.25f)).toInt().coerceAtMost(hi)
                if (nightMode && i == n - 1) level = (level + (lo * 0.2f)).toInt().coerceAtLeast(lo)
                eq.setBandLevel(i.toShort(), level.toShort())
            }
            eq.enabled = true
        } catch (t: Throwable) {
            Log.w(TAG, "EQ apply failed", t)
        }
        try {
            bassBoost?.let {
                val bass = bands.getOrElse(0) { 0f }
                if (nightMode || bass > 0.15f) {
                    it.enabled = true
                    val strength = ((if (nightMode) 0.55f else 0f) + bass.coerceAtLeast(0f) * 0.45f)
                        .coerceIn(0f, 1f)
                    it.setStrength((strength * 1000).toInt().toShort())
                } else {
                    it.enabled = false
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseEffectsOnly() {
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        try { bassBoost?.release() } catch (_: Throwable) {}
        bassBoost = null
    }

    fun release() {
        releaseEffectsOnly()
        sessionId = 0
        releasedForSystemPanel = false
    }

    companion object {
        const val BAND_COUNT = 5
        private const val TAG = "AudioEffects"
    }
}
