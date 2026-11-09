package com.mhj.player.core.playback.engine

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Optional Equalizer / BassBoost on the ExoPlayer audio session.
 * Failures never throw into playback — attach is best-effort and can be disabled.
 */
class AudioEffectsController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var sessionId: Int = 0
    private var nightMode: Boolean = false
    private var bands: FloatArray = FloatArray(BAND_COUNT) { 0f }
    private var releasedForSystemPanel: Boolean = false
    /** When true, skip attaching effects so playback stays audible. */
    @Volatile var effectsDisabled: Boolean = false
        private set

    /** Track the live session without creating an Equalizer (safe for plain playback). */
    fun bindSessionLazy(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId != sessionId) {
            releaseEffectsOnly()
            sessionId = audioSessionId
            releasedForSystemPanel = false
        } else {
            sessionId = audioSessionId
        }
    }

    /**
     * Remember the live session and try to attach. On any failure, [effectsDisabled]
     * is set and playback continues without an Equalizer.
     */
    fun bindSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        sessionId = audioSessionId
        if (effectsDisabled) return
        if (releasedForSystemPanel) return
        if (equalizer != null && audioSessionId == sessionId) {
            applyBandsSafely()
            return
        }
        attachSafely(audioSessionId)
    }

    fun currentSessionId(): Int = sessionId

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        applyBandsSafely()
    }

    fun setBandLevels(normalized: FloatArray) {
        bands = FloatArray(BAND_COUNT) { i ->
            normalized.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
        }
        if (sessionId > 0 && equalizer == null && !effectsDisabled && !releasedForSystemPanel) {
            attachSafely(sessionId)
        }
        applyBandsSafely()
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
        if (sessionId > 0 && equalizer == null && !effectsDisabled && !releasedForSystemPanel) {
            attachSafely(sessionId)
        }
        applyBandsSafely()
    }

    fun buildEqualizerIntents(packageName: String): List<Intent> {
        val session = sessionId
        if (session <= 0) return emptyList()
        // Free our effect so OEM panels can attach; do NOT send CLOSE
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
        }
        return listOf(openSession, display)
    }

    fun restoreAfterSystemPanel() {
        releasedForSystemPanel = false
        if (sessionId <= 0 || effectsDisabled) return
        attachSafely(sessionId)
    }

    fun openSessionBroadcast(packageName: String): Intent? {
        val session = sessionId
        if (session <= 0) return null
        return Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
    }

    private fun attachSafely(audioSessionId: Int) {
        releaseEffectsOnly()
        sessionId = audioSessionId
        try {
            val eq = Equalizer(0, audioSessionId)
            // Flat first, then enable — never leave unknown levels on the track
            val n = eq.numberOfBands.toInt()
            for (i in 0 until n) {
                runCatching { eq.setBandLevel(i.toShort(), 0) }
            }
            eq.enabled = true
            equalizer = eq
        } catch (t: Throwable) {
            Log.e(TAG, "Equalizer attach failed — disabling effects to protect playback", t)
            releaseEffectsOnly()
            effectsDisabled = true
            return
        }
        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost unavailable", t)
            bassBoost = null
        }
        applyBandsSafely()
    }

    private fun applyBandsSafely() {
        val eq = equalizer ?: return
        try {
            val n = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val lo = range[0].toInt()
            val hi = range[1].toInt()
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
                // Map -1..+1 onto millibel range with 0 → midpoint (flat)
                var level = (lo + (hi - lo) * ((src + 1f) / 2f)).toInt()
                if (nightMode && i == 0) level = (level + (hi - lo) * 0.12f).toInt().coerceAtMost(hi)
                if (nightMode && i == n - 1) level = (level - (hi - lo) * 0.08f).toInt().coerceAtLeast(lo)
                eq.setBandLevel(i.toShort(), level.toShort())
            }
            eq.enabled = true
        } catch (t: Throwable) {
            Log.e(TAG, "EQ apply failed — releasing effects", t)
            releaseEffectsOnly()
            effectsDisabled = true
            return
        }
        try {
            bassBoost?.let {
                val bass = bands.getOrElse(0) { 0f }
                if (nightMode || bass > 0.15f) {
                    it.enabled = true
                    val strength = ((if (nightMode) 0.45f else 0f) + bass.coerceAtLeast(0f) * 0.4f)
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
