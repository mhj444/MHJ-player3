package com.mhj.player.core.playback.transcript

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Streams decoded 16-bit mono PCM @ 16 kHz from a content/file/http URI for ASR.
 * Caps decode length so long audiobooks cannot hang the UI forever.
 */
object MediaPcmDecoder {
    const val TARGET_SAMPLE_RATE = 16_000
    /** Enough audio to prove ASR without decoding an entire book. */
    const val MAX_SECONDS = 30

    private const val TAG = "MediaPcmDecoder"

    /**
     * Decode up to [MAX_SECONDS] of audio, invoking [onPcmChunk] with little-endian PCM16 bytes.
     * Returns total PCM samples delivered (mono).
     */
    fun decodeStreaming(
        context: Context,
        uri: Uri,
        onPcmChunk: (ByteArray, Int) -> Unit,
        onProgress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false },
        maxSeconds: Int = MAX_SECONDS,
    ): Int {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("هیچ ترک صوتی در فایل پیدا نشد")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("فرمت صدا نامعتبر است")
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L)
            } else {
                0L
            }
            val maxUs = maxSeconds * 1_000_000L
            codec = MediaCodec.createDecoderByType(mime).also {
                it.configure(format, null, null, 0)
                it.start()
            }
            val c = codec
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var inSampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, TARGET_SAMPLE_RATE)
            var channelCount = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
            var totalOutSamples = 0
            val maxSamples = TARGET_SAMPLE_RATE * maxSeconds
            var lastSampleTime = 0L
            var idleLoops = 0

            while (!outputDone) {
                if (isCancelled()) break
                if (totalOutSamples >= maxSamples) break
                if (lastSampleTime >= maxUs && maxUs > 0) {
                    // Force EOS after cap even if decoder keeps going
                    if (!inputDone) {
                        val inIndex = c.dequeueInputBuffer(2_000)
                        if (inIndex >= 0) {
                            c.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }

                if (!inputDone) {
                    val inIndex = c.dequeueInputBuffer(5_000)
                    if (inIndex >= 0) {
                        val buffer = c.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0 || (maxUs > 0 && extractor.sampleTime >= maxUs)) {
                            c.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            lastSampleTime = extractor.sampleTime.coerceAtLeast(0L)
                            c.queueInputBuffer(inIndex, 0, sampleSize, lastSampleTime, 0)
                            extractor.advance()
                            val denom = when {
                                durationUs > 0 -> minOf(durationUs, maxUs).toFloat().coerceAtLeast(1f)
                                else -> maxUs.toFloat()
                            }
                            onProgress((lastSampleTime / denom).coerceIn(0f, 0.95f))
                        }
                        idleLoops = 0
                    } else {
                        idleLoops++
                    }
                }

                val outIndex = c.dequeueOutputBuffer(info, 5_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = c.outputFormat
                        inSampleRate = outFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, inSampleRate)
                        channelCount = outFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
                        idleLoops = 0
                    }
                    outIndex >= 0 -> {
                        idleLoops = 0
                        val outBuf = c.getOutputBuffer(outIndex)!!
                        if (info.size > 0) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            val shorts = ShortArray(info.size / 2)
                            outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                            val mono = downmixResample(shorts, channelCount, inSampleRate)
                            if (mono.isNotEmpty()) {
                                val remain = maxSamples - totalOutSamples
                                val take = minOf(mono.size, remain)
                                if (take > 0) {
                                    val bytes = ByteArray(take * 2)
                                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                                    for (i in 0 until take) bb.putShort(mono[i])
                                    onPcmChunk(bytes, bytes.size)
                                    totalOutSamples += take
                                }
                            }
                        }
                        c.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                        if (totalOutSamples >= maxSamples) outputDone = true
                    }
                    else -> {
                        idleLoops++
                        if (idleLoops > 200) {
                            error("رمزگشایی صدا متوقف شد (timeout)")
                        }
                    }
                }
            }
            onProgress(1f)
            if (totalOutSamples == 0) error("هیچ نمونه‌ی صدایی از فایل خوانده نشد")
            return totalOutSamples
        } catch (t: Throwable) {
            Log.e(TAG, "decode failed for $uri", t)
            throw t
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun MediaFormat.intOr(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    private fun downmixResample(interleaved: ShortArray, channels: Int, sampleRate: Int): ShortArray {
        val ch = channels.coerceAtLeast(1)
        val frames = interleaved.size / ch
        if (frames <= 0) return ShortArray(0)
        val mono = ShortArray(frames)
        var i = 0
        for (f in 0 until frames) {
            var sum = 0
            for (c in 0 until ch) sum += interleaved[i + c].toInt()
            mono[f] = (sum / ch).toShort()
            i += ch
        }
        if (sampleRate == TARGET_SAMPLE_RATE) return mono
        val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE
        val outLen = (frames / ratio).toInt().coerceAtLeast(0)
        if (outLen <= 0) return ShortArray(0)
        val out = ShortArray(outLen)
        for (n in 0 until outLen) {
            val srcPos = n * ratio
            val idx = srcPos.toInt().coerceIn(0, frames - 1)
            val next = (idx + 1).coerceAtMost(frames - 1)
            val frac = (srcPos - idx).toFloat()
            out[n] = (mono[idx] * (1f - frac) + mono[next] * frac).toInt().toShort()
        }
        return out
    }
}
