package com.mhj.player.core.playback.engine

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.util.Log
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.Eq
import kotlin.math.sqrt

/**
 * All sound shaping, attached once to the player's own audio session and kept for as long as
 * the player lives (in the playback service — not in a screen, so it works with the screen off).
 *
 * One Android "DynamicsProcessing" effect does everything:
 *  - 10-band equalizer + overall level (pre-EQ, input gain)
 *  - Night mode: a gentle compressor — loud parts come down, quiet speech comes up, then a limiter
 *  - Dialogue clarity: lifts the speech range (1–4 kHz) and trims rumble (post-EQ)
 * If the phone refuses it, a plain 5/10-band Equalizer is used for the EQ alone.
 */
class AudioEngine(private val context: Context) {
    private var session = 0
    private var dp: DynamicsProcessing? = null
    private var fallback: Equalizer? = null
    private var last: AppSettings? = null

    /** Edges between bands: geometric middles of the band centres. */
    private val cutoffs: FloatArray = FloatArray(Eq.BANDS) { i ->
        if (i == Eq.BANDS - 1) 20_000f else sqrt(Eq.FREQS[i].toFloat() * Eq.FREQS[i + 1].toFloat())
    }

    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == session) return
        release()
        session = audioSessionId
        try {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true, Eq.BANDS,   // pre-EQ: the equalizer
                true, 1,          // one-band compressor: night mode
                true, 4,          // post-EQ: dialogue clarity
                true,             // limiter
            ).setPreferredFrameDuration(10f).build()
            dp = DynamicsProcessing(0, audioSessionId, cfg).apply { enabled = true }
        } catch (t: Throwable) {
            Log.w(TAG, "DynamicsProcessing unavailable, using Equalizer", t)
            dp = null
            fallback = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
        }
        last?.let { apply(it, force = true) }
    }

    fun apply(s: AppSettings, force: Boolean = false) {
        val prev = last
        last = s
        if (!force && prev != null && prev.eqEnabled == s.eqEnabled && prev.eqGains == s.eqGains &&
            prev.eqPreamp == s.eqPreamp && prev.nightModeAudio == s.nightModeAudio && prev.dialogueClarity == s.dialogueClarity
        ) return
        val d = dp
        if (d != null) try { applyDp(d, s) } catch (t: Throwable) { Log.e(TAG, "apply failed", t) }
        else fallback?.let { applyEq(it, s) }
    }

    private fun applyDp(d: DynamicsProcessing, s: AppSettings) {
        // Equalizer
        val eqOn = s.eqEnabled
        d.setPreEqAllChannelsTo(DynamicsProcessing.Eq(true, eqOn, Eq.BANDS))
        for (i in 0 until Eq.BANDS) {
            val g = if (eqOn) s.eqGains.getOrElse(i) { 0f }.coerceIn(-Eq.MAX_DB, Eq.MAX_DB) else 0f
            d.setPreEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(eqOn, cutoffs[i], g))
        }
        // Keep the loudest boost from clipping: lower the input by part of the biggest boost.
        val maxBoost = if (eqOn) (s.eqGains.maxOrNull() ?: 0f).coerceAtLeast(0f) else 0f
        d.setInputGainAllChannelsTo((if (eqOn) s.eqPreamp else 0f) - maxBoost * 0.5f)

        // Night mode: compress loud parts, lift quiet ones (+ make-up gain), never above the limiter.
        val night = s.nightModeAudio
        d.setMbcAllChannelsTo(DynamicsProcessing.Mbc(true, night, 1))
        d.setMbcBandAllChannelsTo(
            0,
            DynamicsProcessing.MbcBand(
                night, 20_000f,
                5f, 120f,     // attack, release (ms)
                4f, -32f,     // ratio, threshold (dB)
                8f,           // knee
                -70f, 1f,     // noise gate, expander
                0f, if (night) 9f else 0f, // pre / post gain
            ),
        )

        // Dialogue clarity: less rumble, more of the speech range.
        val talk = s.dialogueClarity
        d.setPostEqAllChannelsTo(DynamicsProcessing.Eq(true, talk, 4))
        val post = listOf(150f to -5f, 1_000f to 0f, 4_500f to 5f, 20_000f to 1f)
        post.forEachIndexed { i, (cut, gain) ->
            d.setPostEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(talk, cut, if (talk) gain else 0f))
        }

        // Limiter on whenever something can raise the level.
        val limit = night || talk || maxBoost > 0f || s.eqPreamp > 0f
        d.setLimiterAllChannelsTo(DynamicsProcessing.Limiter(true, limit, 0, 1f, 60f, 10f, -1f, 0f))
        d.enabled = true
    }

    private fun applyEq(e: Equalizer, s: AppSettings) {
        try {
            val n = e.numberOfBands.toInt()
            val (lo, hi) = e.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
            for (b in 0 until n) {
                val centre = e.getCenterFreq(b.toShort()) / 1000f // Hz
                // Nearest of our bands.
                val idx = Eq.FREQS.indices.minByOrNull { kotlin.math.abs(Eq.FREQS[it] - centre) } ?: 0
                val db = if (s.eqEnabled) s.eqGains.getOrElse(idx) { 0f } else 0f
                e.setBandLevel(b.toShort(), (db * 100).toInt().coerceIn(lo, hi).toShort())
            }
            e.enabled = true
        } catch (t: Throwable) { Log.e(TAG, "eq failed", t) }
    }

    fun release() {
        runCatching { dp?.release() }
        runCatching { fallback?.release() }
        dp = null; fallback = null; session = 0
    }

    companion object { private const val TAG = "AudioEngine" }
}
