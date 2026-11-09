package com.mhj.player.core.playback.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log

/**
 * Real Android AudioEffect chain on the ExoPlayer audio session.
 * Dialogue clarity ≈ mid/presence lift + slight loudness; night mode ≈ bass boost + soft compressor.
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
                    1,
                    true,
                    5,
                    true,
                    3,
                    false,
                    0,
                    true,
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
    }

    fun setDialogueClarity(enabled: Boolean) {
        dialogueClarity = enabled
        apply()
    }

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        apply()
    }

    fun openSystemEqualizerIntent(packageName: String): android.content.Intent? {
        val session = sessionId
        if (session <= 0) return null
        return android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            .putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, session)
            .putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            .putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
    }

    private fun apply() {
        val eq = equalizer
        if (eq != null) {
            try {
                val n = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val flat = 0.toShort()
                for (i in 0 until n) {
                    eq.setBandLevel(i.toShort(), flat)
                }
                if (dialogueClarity && n >= 3) {
                    // Presence / speech intelligibility (~1–4 kHz)
                    val midLo = (n * 0.30f).toInt().coerceIn(0, n - 1)
                    val midHi = (n * 0.70f).toInt().coerceIn(0, n - 1)
                    val lift = (range[1] * 0.55f).toInt().coerceAtMost(range[1].toInt()).toShort()
                    val slightCut = (range[0] * 0.15f).toInt().toShort()
                    for (i in midLo..midHi) {
                        eq.setBandLevel(i.toShort(), lift)
                    }
                    // Soften extreme lows so dialogue sits forward
                    eq.setBandLevel(0, slightCut)
                }
                if (nightMode && n >= 2) {
                    val bass = (range[1] * 0.35f).toInt().coerceAtMost(range[1].toInt()).toShort()
                    val trebleCut = (range[0] * 0.30f).toInt().toShort()
                    eq.setBandLevel(0, bass)
                    if (n > 2) eq.setBandLevel(1, (bass / 2).toShort())
                    eq.setBandLevel((n - 1).toShort(), trebleCut)
                }
                eq.enabled = dialogueClarity || nightMode || true
            } catch (t: Throwable) {
                Log.w(TAG, "EQ apply failed", t)
            }
        }

        try {
            bassBoost?.let { bb ->
                bb.enabled = nightMode
                if (nightMode) bb.setStrength(650.toShort())
            }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost apply failed", t)
        }

        try {
            loudness?.let { le ->
                le.enabled = dialogueClarity
                if (dialogueClarity) le.setTargetGain(600) // mB
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Loudness apply failed", t)
        }

        val dyn = dynamics
        if (dyn != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dyn.enabled = nightMode
            } catch (t: Throwable) {
                Log.w(TAG, "Dynamics toggle failed", t)
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
