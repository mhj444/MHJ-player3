package com.mhj.player.core.playback.transcript

import android.content.Context
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

data class TranscriptionUiState(
    val isRunning: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val statusFa: String = "تبدیل گفتار رسانه به متن",
    val errorFa: String? = null,
    val activeMediaId: String? = null,
    val viewerText: String? = null,
    val viewerTitle: String? = null,
)

/**
 * On-device ASR of the currently playing media file (not the microphone) via Vosk.
 */
class TranscriptionController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val store = TranscriptStore(appContext)
    private val models = VoskModelManager(appContext)

    private val _state = MutableStateFlow(TranscriptionUiState())
    val state: StateFlow<TranscriptionUiState> = _state.asStateFlow()

    private var job: Job? = null
    @Volatile private var cancelRequested = false

    fun toggleForMedia(mediaId: String, title: String, uri: String) {
        if (_state.value.isRunning && _state.value.activeMediaId == mediaId) {
            stop()
            return
        }
        if (_state.value.isRunning) {
            stop()
        }
        start(mediaId, title, uri)
    }

    fun start(mediaId: String, title: String, uri: String) {
        cancelRequested = false
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            try {
                _state.value = TranscriptionUiState(
                    isRunning = true,
                    isDownloading = !models.isModelReady(),
                    progress = 0f,
                    statusFa = if (models.isModelReady()) "در حال تبدیل به متن…" else "در حال دانلود مدل فارسی…",
                    activeMediaId = mediaId,
                )
                val modelDir = try {
                    models.ensureModel { p, label ->
                        _state.value = _state.value.copy(
                            isDownloading = p < 1f && !models.isModelReady(),
                            progress = p,
                            statusFa = label,
                            errorFa = null,
                        )
                    }
                } catch (t: Throwable) {
                    val msg = when {
                        t.message == "offline" || !models.isOnline() ->
                            "آفلاین هستید و مدل فارسی هنوز دانلود نشده است."
                        else -> "دانلود مدل فارسی ناموفق بود."
                    }
                    _state.value = TranscriptionUiState(
                        isRunning = false,
                        statusFa = "تبدیل گفتار رسانه به متن",
                        errorFa = msg,
                        activeMediaId = mediaId,
                    )
                    return@launch
                }

                if (cancelRequested) {
                    _state.value = TranscriptionUiState(statusFa = "متوقف شد")
                    return@launch
                }

                _state.value = _state.value.copy(
                    isDownloading = false,
                    progress = 0.05f,
                    statusFa = "در حال خواندن صدا از رسانه…",
                )

                val pcm = MediaPcmDecoder.decodeToPcm16Mono(
                    context = appContext,
                    uri = Uri.parse(uri),
                    onProgress = { p ->
                        _state.value = _state.value.copy(
                            progress = 0.05f + p * 0.35f,
                            statusFa = "در حال آماده‌سازی صدا…",
                        )
                    },
                    isCancelled = { cancelRequested || !isActive },
                )

                if (cancelRequested || !isActive) {
                    flushPartial(mediaId, title, "")
                    _state.value = TranscriptionUiState(
                        statusFa = "متوقف شد",
                        activeMediaId = mediaId,
                    )
                    return@launch
                }

                _state.value = _state.value.copy(
                    progress = 0.42f,
                    statusFa = "در حال تبدیل به متن…",
                )

                val model = Model(modelDir.absolutePath)
                val recognizer = Recognizer(model, MediaPcmDecoder.TARGET_SAMPLE_RATE.toFloat())
                val chunk = ShortArray(CHUNK_SAMPLES)
                var offset = 0
                val paragraphs = StringBuilder()
                try {
                    while (offset < pcm.size && !cancelRequested && isActive) {
                        val len = minOf(CHUNK_SAMPLES, pcm.size - offset)
                        System.arraycopy(pcm, offset, chunk, 0, len)
                        offset += len
                        val bytes = ByteBuffer.allocate(len * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until len) bytes.putShort(chunk[i])
                        val accepted = recognizer.acceptWaveForm(bytes.array(), bytes.array().size)
                        if (accepted) {
                            val text = extractText(recognizer.result)
                            if (text.isNotBlank()) {
                                paragraphs.append(text).append('\n')
                                store.appendParagraph(mediaId, title, text)
                            }
                        }
                        val p = offset.toFloat() / pcm.size.toFloat()
                        _state.value = _state.value.copy(
                            progress = 0.42f + p * 0.55f,
                            statusFa = "در حال تبدیل به متن… ${(p * 100).toInt()}٪",
                        )
                    }
                    val finalText = extractText(recognizer.finalResult)
                    if (finalText.isNotBlank()) {
                        paragraphs.append(finalText)
                        store.appendParagraph(mediaId, title, finalText)
                    }
                } finally {
                    runCatching { recognizer.close() }
                    runCatching { model.close() }
                }

                _state.value = TranscriptionUiState(
                    isRunning = false,
                    progress = 1f,
                    statusFa = if (cancelRequested) "متوقف شد" else "متن ذخیره شد",
                    activeMediaId = mediaId,
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Transcription failed", t)
                _state.value = TranscriptionUiState(
                    isRunning = false,
                    statusFa = "تبدیل گفتار رسانه به متن",
                    errorFa = "خطا در تبدیل گفتار به متن",
                    activeMediaId = mediaId,
                )
            }
        }
    }

    fun stop() {
        cancelRequested = true
        // Job continues until it flushes finalResult; mark UI immediately
        _state.value = _state.value.copy(statusFa = "در حال توقف…")
    }

    fun openViewerFor(mediaId: String?, titleHint: String?): Boolean {
        val text = when {
            !mediaId.isNullOrBlank() -> store.read(mediaId)
            else -> store.latest()?.second
        }
        if (text.isNullOrBlank()) {
            _state.value = _state.value.copy(
                errorFa = "هنوز متنی برای این رسانه ذخیره نشده است.",
                viewerText = null,
            )
            return false
        }
        val title = titleHint
            ?: store.listAll().firstOrNull { it.mediaKey == mediaId }?.title
            ?: text.lineSequence().firstOrNull().orEmpty()
        _state.value = _state.value.copy(
            viewerText = text,
            viewerTitle = title,
            errorFa = null,
        )
        return true
    }

    fun dismissViewer() {
        _state.value = _state.value.copy(viewerText = null, viewerTitle = null)
    }

    private fun flushPartial(mediaId: String, title: String, text: String) {
        if (text.isNotBlank()) store.appendParagraph(mediaId, title, text)
    }

    private fun extractText(json: String): String {
        return runCatching {
            JSONObject(json).optString("text").trim()
        }.getOrDefault("").trim()
    }

    companion object {
        private const val TAG = "Transcription"
        private const val CHUNK_SAMPLES = 4000 // 250ms @ 16kHz
    }
}
