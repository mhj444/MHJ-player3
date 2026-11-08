package com.mhj.player.core.playback.engine

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mhj.player.core.model.MediaItemModel
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

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(
                errorMessage = error.message ?: "Playback error",
                isPlaying = false,
            )
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

    fun play(items: List<MediaItemModel>, startIndex: Int = 0) {
        val c = controller ?: return
        if (items.isEmpty()) return
        c.setMediaItems(items.map { it.toMediaItem() }, startIndex, 0L)
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

    fun playerOrNull(): Player? = controller

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
                controller?.let { publish(it) }
                delay(400)
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
            audioSessionId = (player as? androidx.media3.exoplayer.ExoPlayer)?.audioSessionId ?: 0,
        )
    }
}

fun MediaItemModel.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}
