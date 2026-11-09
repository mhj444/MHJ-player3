package com.mhj.player.core.playback.engine

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log

/**
 * Real Android AudioEffect chain on the ExoPlayer audio session.
 */
class AudioEffectsController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var dynamics: DynamicsProcessing? = null
    private var sessionId: Int = 0
    private var dialogueClarity: Boolean = false
    private var nightMode: Boolean = false

    fun bindSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == sessionId && equalizer != null) {
            apply()
            return
        }
        releaseEffectsOnly()
        sessionId = audioSessionId
        try {
            equalizer = Equalizer(Int.MAX_VALUE / 2, audioSessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer unavailable", t)
            equalizer = null
        }
        try {
            bassBoost = BassBoost(Int.MAX_VALUE / 2, audioSessionId).apply { enabled = false }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost unavailable", t)
            bassBoost = null
        }
        try {
            loudness = LoudnessEnhancer(audioSessionId).apply { enabled = false }
        } catch (t: Throwable) {
            Log.w(TAG, "LoudnessEnhancer unavailable", t)
            loudness = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cfg = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1, true, 5, true, 3, false, 0, true,
                ).build()
                dynamics = DynamicsProcessing(Int.MAX_VALUE / 2, audioSessionId, cfg).apply {
                    enabled = false
                }
            } catch (t: Throwable) {
                Log.w(TAG, "DynamicsProcessing unavailable", t)
                dynamics = null
            }
        }
        apply()
        // Notify external EQ UIs that a session is active
        runCatching {
            // Broadcast is best-effort; some OEMs listen for it
        }
    }

    fun currentSessionId(): Int = sessionId

    fun setDialogueClarity(enabled: Boolean) {
        dialogueClarity = enabled
        apply()
    }

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        apply()
    }

    /**
     * Opens the system / OEM equalizer for this session.
     * Also fires OPEN_AUDIO_EFFECT_CONTROL_SESSION so panels attach.
     */
    fun buildEqualizerIntents(packageName: String): List<Intent> {
        val session = sessionId
        if (session <= 0) return emptyList()
        val openSession = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        val display = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return listOf(openSession, display)
    }

    @Deprecated("Use buildEqualizerIntents")
    fun openSystemEqualizerIntent(packageName: String): Intent? =
        buildEqualizerIntents(packageName).getOrNull(1)

    private fun apply() {
        val eq = equalizer
        if (eq != null) {
            try {
                val n = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                for (i in 0 until n) eq.setBandLevel(i.toShort(), 0)
                if (dialogueClarity && n >= 3) {
                    val midLo = (n * 0.30f).toInt().coerceIn(0, n - 1)
                    val midHi = (n * 0.70f).toInt().coerceIn(0, n - 1)
                    val lift = (range[1] * 0.55f).toInt().coerceAtMost(range[1].toInt()).toShort()
                    for (i in midLo..midHi) eq.setBandLevel(i.toShort(), lift)
                }
                if (nightMode && n >= 2) {
                    val bass = (range[1] * 0.35f).toInt().coerceAtMost(range[1].toInt()).toShort()
                    val trebleCut = (range[0] * 0.30f).toInt().toShort()
                    eq.setBandLevel(0, bass)
                    eq.setBandLevel((n - 1).toShort(), trebleCut)
                }
                eq.enabled = true
            } catch (t: Throwable) {
                Log.w(TAG, "EQ apply failed", t)
            }
        }
        try {
            bassBoost?.let {
                it.enabled = nightMode
                if (nightMode) it.setStrength(650.toShort())
            }
        } catch (_: Throwable) {
        }
        try {
            loudness?.let {
                it.enabled = dialogueClarity
                if (dialogueClarity) it.setTargetGain(600)
            }
        } catch (_: Throwable) {
        }
        if (dynamics != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dynamics?.enabled = nightMode
            } catch (_: Throwable) {
            }
        }
    }

    private fun releaseEffectsOnly() {
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        try { bassBoost?.release() } catch (_: Throwable) {}
        bassBoost = null
        try { loudness?.release() } catch (_: Throwable) {}
        loudness = null
        try { dynamics?.release() } catch (_: Throwable) {}
        dynamics = null
    }

    fun release() {
        releaseEffectsOnly()
        sessionId = 0
    }

    companion object {
        private const val TAG = "AudioEffects"
    }
}
