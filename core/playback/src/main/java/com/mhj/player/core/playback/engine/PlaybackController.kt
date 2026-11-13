package com.mhj.player.core.playback.engine

import com.mhj.player.core.model.L

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.model.RepeatMode
import com.mhj.player.core.playback.service.MhjPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * UI-facing playback facade. Talks to [MhjPlaybackService] via Media3 MediaController.
 * Keeps playback logic out of Compose screens.
 */
class PlaybackController(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** A-B loop markers (ms). Both null = off; A set / B null = waiting for B. */
    private var abPointAMs: Long? = null
    private var abPointBMs: Long? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                TAG,
                "player error code=${error.errorCode} name=${error.errorCodeName} " +
                    "msg=${error.message} cause=${error.cause}",
                error,
            )
            _state.value = _state.value.copy(
                errorMessage = error.message ?: error.errorCodeName,
                isPlaying = false,
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val name = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN($playbackState)"
            }
            Log.i(TAG, "playbackState=$name playing=${controller?.isPlaying} session=${audioSessionId()}")
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, MhjPlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(listener)
                publish(c)
                startPositionTicker()
            },
            MoreExecutors.directExecutor(),
        )
    }

    suspend fun awaitConnected(): MediaController? {
        connect()
        val existing = controller
        if (existing != null) return existing
        val future = controllerFuture ?: return null
        return suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    cont.resume(runCatching { future.get() }.getOrNull())
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    fun play(items: List<MediaItemModel>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val c = controller ?: run {
            Log.e(TAG, "play() ignored — MediaController not connected")
            return
        }
        if (items.isEmpty()) {
            Log.e(TAG, "play() ignored — empty queue")
            return
        }
        val start = startIndex.coerceIn(0, items.lastIndex)
        Log.i(TAG, "play index=$start title=${items[start].title} uri=${items[start].uri}")
        c.setMediaItems(items.map { it.toMediaItem() }, start, startPositionMs.coerceAtLeast(0L))
        c.prepare()
        c.play()
    }

    fun playUri(item: MediaItemModel) = play(listOf(item), 0)

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /**
     * Picture follows the time bar. A new seek wipes the frame that is still being drawn,
     * so only one seek runs at a time, then the player jumps to where the finger is now.
     */
    private val scrubHandler = Handler(Looper.getMainLooper())
    private var scrubTarget = -1L
    private var scrubAwaiting = false
    private var scrubWasPlaying = false
    private var scrubActive = false
    private var frameListener: Player.Listener? = null
    private val scrubGiveUp = Runnable { finishScrubStep() }

    fun scrubStart() {
        if (scrubActive) return
        val exo = PlayerHolder.playerOrNull() ?: return
        scrubActive = true
        scrubWasPlaying = exo.isPlaying
        if (exo.isPlaying) exo.pause()
        // Within a fifth of a second of the finger. A nearer keyframe is used when there is one.
        exo.setSeekParameters(SeekParameters(200_000L, 200_000L))
    }

    fun scrubTo(positionMs: Long) {
        if (!scrubActive) scrubStart()
        scrubTarget = positionMs.coerceAtLeast(0L)
        if (!scrubAwaiting) issueScrub()
    }

    private fun issueScrub() {
        if (scrubAwaiting || !scrubActive) return
        val exo = PlayerHolder.playerOrNull() ?: return
        val target = scrubTarget
        if (target < 0L || kotlin.math.abs(exo.currentPosition - target) < 180L) return
        scrubAwaiting = true
        var sawSeek = false
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) sawSeek = true
            }

            override fun onRenderedFirstFrame() {
                if (!sawSeek || frameListener !== this) return
                scrubHandler.post {
                    if (frameListener === listener) finishScrubStep()
                }
            }
        }
        frameListener = listener
        exo.addListener(listener)
        exo.seekTo(target)
        scrubHandler.postDelayed(scrubGiveUp, 140)
    }

    private fun finishScrubStep() {
        scrubHandler.removeCallbacks(scrubGiveUp)
        frameListener?.let { PlayerHolder.playerOrNull()?.removeListener(it) }
        frameListener = null
        scrubAwaiting = false
        if (scrubActive) issueScrub()
    }

    fun scrubEnd(positionMs: Long) {
        scrubActive = false
        scrubTarget = -1L
        scrubHandler.removeCallbacksAndMessages(null)
        frameListener?.let { PlayerHolder.playerOrNull()?.removeListener(it) }
        frameListener = null
        scrubAwaiting = false
        val exo = PlayerHolder.playerOrNull()
        val target = positionMs.coerceAtLeast(0L)
        if (exo != null) {
            exo.setSeekParameters(SeekParameters.EXACT)
            exo.seekTo(target)
            if (scrubWasPlaying) exo.play()
            scrubHandler.postDelayed({
                if (!scrubActive) PlayerHolder.playerOrNull()?.setSeekParameters(SeekParameters.DEFAULT)
            }, 500)
        } else {
            controller?.seekTo(target)
            if (scrubWasPlaying) controller?.play()
        }
        scrubWasPlaying = false
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        val c = controller ?: return
        if (c.currentPosition > 3_000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.25f, 3.0f))
    }

    /**
     * A-B repeat: 1st tap marks A, 2nd marks B (loop), 3rd clears.
     * Returns a short status string for UI feedback.
     */
    fun tapAbLoop(): String {
        val c = controller ?: return L.t("اول یک آهنگ پخش کنید")
        val pos = c.currentPosition.coerceAtLeast(0L)
        val a = abPointAMs
        val b = abPointBMs
        return when {
            a == null -> {
                abPointAMs = pos
                abPointBMs = null
                publish(c)
                L.pick("نقطهٔ A: ${formatMs(pos)} — حالا B را بزنید", "Point A: ${formatMs(pos)} — now tap for B")
            }
            b == null -> {
                if (pos <= a + 250L) {
                    L.t("نقطهٔ B باید بعد از A باشد")
                } else {
                    abPointBMs = pos
                    publish(c)
                    L.pick("تکرار از ${formatMs(a)} تا ${formatMs(pos)}", "Repeating ${formatMs(a)}–${formatMs(pos)}")
                }
            }
            else -> {
                abPointAMs = null
                abPointBMs = null
                publish(c)
                L.t("تکرار A-B خاموش شد")
            }
        }
    }

    fun clearAbLoop() {
        abPointAMs = null
        abPointBMs = null
        controller?.let { publish(it) }
    }

    /** Adds a subtitle file to the video that is playing, keeping its place. */
    fun attachSubtitle(path: String, mime: String) {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        val index = c.currentMediaItemIndex
        val pos = c.currentPosition
        val wasPlaying = c.playWhenReady
        val updated = item.buildUpon().setSubtitleConfigurations(listOf(subtitleConfig(path, mime))).build()
        c.replaceMediaItem(index, updated)
        c.seekTo(index, pos)
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
            .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
            .build()
        c.playWhenReady = wasPlaying
    }

    fun playerOrNull(): Player? = controller

    fun audioSessionId(): Int =
        PlayerHolder.playerOrNull()?.audioSessionId?.takeIf { it > 0 } ?: 0

    fun release() {
        positionJob?.cancel()
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { c ->
                    val a = abPointAMs
                    val b = abPointBMs
                    if (a != null && b != null && b > a) {
                        val pos = c.currentPosition
                        if (pos >= b) {
                            c.seekTo(a)
                        }
                    }
                    publish(c)
                }
                delay(200)
            }
        }
    }

    private fun publish(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId
        val meta = player.currentMediaItem?.mediaMetadata
        val current = if (mediaId != null) {
            MediaItemModel(
                id = mediaId,
                uri = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty(),
                title = meta?.title?.toString() ?: "Unknown",
                artist = meta?.artist?.toString() ?: "Unknown artist",
                album = meta?.albumTitle?.toString().orEmpty(),
                durationMs = player.duration.coerceAtLeast(0L),
                artworkUri = meta?.artworkUri?.toString(),
                kind = meta.kindOr(MediaKind.MUSIC),
                folder = meta?.extras?.getString(MEDIA_FOLDER_EXTRA).orEmpty(),
                format = meta?.extras?.getString(MEDIA_FORMAT_EXTRA).orEmpty(),
            )
        } else {
            null
        }
        val queue = buildList {
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i)
                val m = item.mediaMetadata
                add(
                    MediaItemModel(
                        id = item.mediaId,
                        uri = item.localConfiguration?.uri?.toString().orEmpty(),
                        title = m.title?.toString() ?: "Unknown",
                        artist = m.artist?.toString() ?: "Unknown artist",
                        album = m.albumTitle?.toString().orEmpty(),
                        durationMs = 0L,
                        artworkUri = m.artworkUri?.toString(),
                        kind = m.kindOr(MediaKind.MUSIC),
                        folder = m.extras?.getString(MEDIA_FOLDER_EXTRA).orEmpty(),
                        format = m.extras?.getString(MEDIA_FORMAT_EXTRA).orEmpty(),
                    ),
                )
            }
        }
        _state.value = PlayerUiState(
            current = current,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            shuffle = player.shuffleModeEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            queue = queue,
            errorMessage = player.playerError?.message,
            // MediaController is not ExoPlayer — read live session from the service holder.
            audioSessionId = PlayerHolder.playerOrNull()?.audioSessionId?.takeIf { it > 0 } ?: 0,
            abPointAMs = abPointAMs,
            abPointBMs = abPointBMs,
        )
    }

    companion object {
        private const val TAG = "Playback"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000L).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
}

fun MediaItemModel.toMediaItem(): MediaItem {
    val extras = android.os.Bundle().apply {
        putString(MEDIA_KIND_EXTRA, kind.name)
        putString(MEDIA_FOLDER_EXTRA, folder)
        putString(MEDIA_FORMAT_EXTRA, format)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(
            artist.trim().let {
                if (it.isEmpty() || it.equals("<unknown>", true) || it.equals("unknown artist", true)) L.t("خوانندهٔ نامشخص") else it
            },
        )
        .setAlbumTitle(album.takeUnless { it.equals("<unknown>", true) })
        .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(extras)
        .build()
    val sub = com.mhj.player.core.data.memory.ListenMemory.subtitleFor(id)
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(metadata)
        .apply {
            if (sub != null) setSubtitleConfigurations(listOf(subtitleConfig(sub.first, sub.second)))
        }
        .build()
}

fun subtitleConfig(path: String, mime: String): MediaItem.SubtitleConfiguration =
    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(java.io.File(path)))
        .setMimeType(mime)
        .setLanguage("fa")
        .setLabel("زیرنویس")
        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
        .build()

const val MEDIA_KIND_EXTRA = "mhj_media_kind"
const val MEDIA_FOLDER_EXTRA = "mhj_media_folder"
const val MEDIA_FORMAT_EXTRA = "mhj_media_format"

private fun MediaMetadata?.kindOr(fallback: MediaKind): MediaKind {
    val raw = this?.extras?.getString(MEDIA_KIND_EXTRA) ?: return fallback
    return runCatching { MediaKind.valueOf(raw) }.getOrDefault(fallback)
}
