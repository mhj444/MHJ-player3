package com.mhj.player.core.playback.engine

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log

/**
 * Hooks Android [android.media.audiofx.AudioEffect] onto Media3's audio session.
 * Dialogue clarity ≈ mid/presence lift; night mode ≈ soft compressor + bass shelf.
 * Requires a live [audioSessionId] from ExoPlayer (kept via Media3).
 */
class AudioEffectsController {
    private var equalizer: Equalizer? = null
    private var dynamics: DynamicsProcessing? = null
    private var sessionId: Int = 0
    private var dialogueClarity: Boolean = false
    private var nightMode: Boolean = false

    fun bindSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == sessionId) return
        release()
        sessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer unavailable", t)
            equalizer = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cfg = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    /*channelCount*/ 1,
                    /*preEqInUse*/ true,
                    /*preEqBandCount*/ 5,
                    /*mbcInUse*/ true,
                    /*mbcBandCount*/ 3,
                    /*postEqInUse*/ false,
                    /*postEqBandCount*/ 0,
                    /*limiterInUse*/ true,
                ).build()
                dynamics = DynamicsProcessing(0, audioSessionId, cfg).apply { enabled = false }
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

    private fun apply() {
        val eq = equalizer ?: return
        try {
            val n = eq.numberOfBands.toInt()
            // Flat baseline
            for (i in 0 until n) {
                val range = eq.getBandLevelRange()
                val mid = ((range[0] + range[1]) / 2).toShort()
                eq.setBandLevel(i.toShort(), mid)
            }
            if (dialogueClarity && n >= 3) {
                // Presence / dialogue: lift mid bands (~1–4 kHz)
                val midLo = (n * 0.35f).toInt().coerceIn(0, n - 1)
                val midHi = (n * 0.65f).toInt().coerceIn(0, n - 1)
                val range = eq.bandLevelRange
                val lift = (range[1] * 0.45f).toInt().toShort()
                for (i in midLo..midHi) {
                    eq.setBandLevel(i.toShort(), lift)
                }
            }
            if (nightMode && n >= 2) {
                // Soft night: gentle bass shelf + slight high cut for late listening
                val range = eq.bandLevelRange
                val bass = (range[1] * 0.28f).toInt().toShort()
                val trebleCut = (range[0] * 0.25f).toInt().toShort()
                eq.setBandLevel(0, bass)
                eq.setBandLevel((n - 1).toShort(), trebleCut)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "EQ apply failed", t)
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

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Throwable) {
        }
        equalizer = null
        try {
            dynamics?.release()
        } catch (_: Throwable) {
        }
        dynamics = null
        sessionId = 0
    }

    companion object {
        private const val TAG = "AudioEffects"
    }
}
