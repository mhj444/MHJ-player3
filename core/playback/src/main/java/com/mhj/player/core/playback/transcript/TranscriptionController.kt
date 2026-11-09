package com.mhj.player.core.playback.transcript

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    /** File being written right now (shown as "در حال نوشتن" in the list). */
    val activePath: String? = null,
    val viewerText: String? = null,
    val viewerTitle: String? = null,
)

/**
 * On-device ASR of the currently playing media file (not the microphone) via Vosk.
 * Streams the full file as PCM chunks on a background coroutine — never loads all
 * samples into an ArrayList — and appends each finished utterance to the same txt.
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

    private val _history = MutableStateFlow<List<TranscriptFileInfo>>(emptyList())
    /** All saved transcripts, newest first. */
    val history: StateFlow<List<TranscriptFileInfo>> = _history.asStateFlow()

    fun refreshHistory() {
        scope.launch(Dispatchers.IO) { _history.value = runCatching { store.listAll() }.getOrDefault(emptyList()) }
    }

    fun openSaved(info: TranscriptFileInfo) {
        scope.launch(Dispatchers.IO) {
            val text = store.readPath(info.path) ?: return@launch
            _state.value = _state.value.copy(viewerText = text, viewerTitle = info.title, errorFa = null)
        }
    }

    fun deleteSaved(info: TranscriptFileInfo) {
        scope.launch(Dispatchers.IO) {
            store.delete(info.path)
            _history.value = runCatching { store.listAll() }.getOrDefault(emptyList())
        }
    }

    fun readSaved(info: TranscriptFileInfo): String? = store.readPath(info.path)

    private var job: Job? = null
    @Volatile private var cancelRequested = false

    init { refreshHistory() }

    fun toggleForMedia(mediaId: String, title: String, uri: String) {
        if (_state.value.isRunning && _state.value.activeMediaId == mediaId) {
            stop()
            return
        }
        if (_state.value.isRunning) stop()
        start(mediaId, title, uri)
    }

    fun start(mediaId: String, title: String, uri: String) {
        val previous = job
        // Ask any in-flight job to stop+flush, then wait briefly before replacing it
        if (previous?.isActive == true) {
            cancelRequested = true
        }
        job = scope.launch(Dispatchers.IO) {
            if (previous?.isActive == true) {
                withTimeoutOrNull(8_000) { previous.join() }
                if (previous.isActive) previous.cancel()
            }
            cancelRequested = false
            try {
                if (uri.isBlank()) error("آدرس فایل خالی است")
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
                    Log.e(TAG, "model ensure failed", t)
                    val msg = when {
                        t.message == "offline" || !models.isOnline() ->
                            "آفلاین هستید و مدل فارسی هنوز دانلود نشده است."
                        else -> "دانلود مدل فارسی ناموفق بود (${t.message?.take(40) ?: "خطای شبکه"}). دوباره امتحان کنید."
                    }
                    _state.value = TranscriptionUiState(
                        isRunning = false,
                        statusFa = "تبدیل گفتار رسانه به متن",
                        errorFa = msg,
                        activeMediaId = mediaId,
                    )
                    return@launch
                }

                if (cancelRequested || !isActive) {
                    _state.value = TranscriptionUiState(statusFa = "متوقف شد", activeMediaId = mediaId)
                    return@launch
                }

                _state.value = _state.value.copy(
                    isDownloading = false,
                    progress = 0.02f,
                    statusFa = "در حال آماده‌سازی صدا…",
                )

                // Each run writes its own new file (a second run does not repeat into the first one).
                val out = store.newRun(mediaId, title)
                _state.value = _state.value.copy(activePath = out.absolutePath)
                refreshHistory()

                val model = Model(modelDir.absolutePath)
                val recognizer = Recognizer(model, MediaPcmDecoder.TARGET_SAMPLE_RATE.toFloat())
                var wroteAny = false
                var gotPcm = false
                try {
                    MediaPcmDecoder.decodeStreaming(
                        context = appContext,
                        uri = Uri.parse(uri),
                        onPcmChunk = { bytes, len ->
                            if (cancelRequested || !isActive) return@decodeStreaming
                            if (!gotPcm) {
                                gotPcm = true
                                _state.value = _state.value.copy(statusFa = "در حال نوشتن…")
                            }
                            val accepted = recognizer.acceptWaveForm(bytes, len)
                            if (accepted) {
                                val text = extractText(recognizer.result)
                                if (text.isNotBlank()) {
                                    store.appendTo(out, text)
                                    wroteAny = true
                                    val pct = (_state.value.progress * 100).toInt().coerceIn(0, 99)
                                    _state.value = _state.value.copy(
                                        statusFa = "در حال نوشتن… $pct٪",
                                    )
                                }
                            }
                        },
                        onProgress = { p ->
                            val pct = (p * 100).toInt().coerceIn(0, 99)
                            _state.value = _state.value.copy(
                                progress = p.coerceIn(0f, 0.99f),
                                statusFa = when {
                                    wroteAny || gotPcm -> "در حال نوشتن… $pct٪"
                                    else -> "در حال آماده‌سازی صدا… $pct٪"
                                },
                            )
                        },
                        isCancelled = { cancelRequested || !isActive },
                    )

                    // Stop / EOS: flush remaining hypothesis into the same txt
                    val finalText = extractText(recognizer.finalResult)
                    if (finalText.isNotBlank()) {
                        store.appendTo(out, finalText)
                        wroteAny = true
                    }
                } finally {
                    runCatching { recognizer.close() }
                    runCatching { model.close() }
                }

                if (!wroteAny) runCatching { out.delete() }
                if (cancelRequested) {
                    _state.value = TranscriptionUiState(
                        isRunning = false,
                        progress = _state.value.progress.coerceAtLeast(0.01f),
                        statusFa = "متوقف شد",
                        activeMediaId = mediaId,
                    )
                } else if (!wroteAny) {
                    _state.value = TranscriptionUiState(
                        isRunning = false,
                        progress = 1f,
                        statusFa = "تبدیل گفتار رسانه به متن",
                        errorFa = "متنی تشخیص داده نشد. فایل دیگری را امتحان کنید.",
                        activeMediaId = mediaId,
                    )
                } else {
                    _state.value = TranscriptionUiState(
                        isRunning = false,
                        progress = 1f,
                        statusFa = "متن ذخیره شد",
                        activeMediaId = mediaId,
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Transcription failed uri=$uri", t)
                val brief = t.message?.take(80)?.takeIf { it.isNotBlank() }
                _state.value = TranscriptionUiState(
                    isRunning = false,
                    statusFa = "تبدیل گفتار رسانه به متن",
                    errorFa = brief?.let { "خطا: $it" } ?: "خطا در تبدیل گفتار به متن",
                    activeMediaId = mediaId,
                )
            }
        }.also { it.invokeOnCompletion { refreshHistory() } }
    }

    /** Request cancel; the running job breaks decode and flushes finalResult into the txt. */
    fun stop() {
        cancelRequested = true
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

    private fun extractText(json: String): String {
        return runCatching {
            JSONObject(json).optString("text").trim()
        }.getOrDefault("").trim()
    }

    companion object {
        private const val TAG = "Transcription"
    }
}
