package com.mhj.player.core.playback.transcript

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Decodes an audio/video content URI to 16-bit mono PCM at [TARGET_SAMPLE_RATE] for ASR.
 */
object MediaPcmDecoder {
    const val TARGET_SAMPLE_RATE = 16_000

    fun decodeToPcm16Mono(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): ShortArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Missing mime")
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcm = ArrayList<Short>(TARGET_SAMPLE_RATE * 60)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var inSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                TARGET_SAMPLE_RATE
            }
            var channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }

            while (!outputDone) {
                if (isCancelled()) break
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                            if (durationUs > 0) {
                                onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 0.95f))
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            inSampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        if (info.size > 0) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            val shorts = ShortArray(info.size / 2)
                            outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                            appendResampledMono(pcm, shorts, channelCount, inSampleRate)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
            codec.stop()
            codec.release()
            onProgress(1f)
            return pcm.toShortArray()
        } finally {
            extractor.release()
        }
    }

    private fun appendResampledMono(
        out: MutableList<Short>,
        interleaved: ShortArray,
        channels: Int,
        sampleRate: Int,
    ) {
        val ch = channels.coerceAtLeast(1)
        // Downmix
        val mono = ShortArray(interleaved.size / ch)
        var i = 0
        var o = 0
        while (i + ch - 1 < interleaved.size) {
            var sum = 0
            for (c in 0 until ch) sum += interleaved[i + c].toInt()
            mono[o++] = (sum / ch).toShort()
            i += ch
        }
        if (sampleRate == TARGET_SAMPLE_RATE) {
            for (s in 0 until o) out.add(mono[s])
            return
        }
        // Linear resample
        val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE
        val outLen = (o / ratio).toInt().coerceAtLeast(0)
        for (n in 0 until outLen) {
            val srcPos = n * ratio
            val idx = srcPos.toInt().coerceIn(0, o - 1)
            val next = min(idx + 1, o - 1)
            val frac = (srcPos - idx).toFloat()
            val sample = mono[idx] * (1f - frac) + mono[next] * frac
            out.add(sample.toInt().toShort())
        }
    }

    fun pcmToByteBuffer(pcm: ShortArray): ByteBuffer {
        val buf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        buf.asShortBuffer().put(pcm)
        buf.rewind()
        return buf
    }
}
