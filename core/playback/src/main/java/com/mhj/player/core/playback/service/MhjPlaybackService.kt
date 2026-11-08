package com.mhj.player.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mhj.player.core.data.db.MhjDatabase
import com.mhj.player.core.data.repository.PlaybackPersistenceRepository
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.RepeatMode
import com.mhj.player.core.playback.engine.PlayerHolder
import com.mhj.player.core.playback.engine.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground media playback service with MediaSession (lock-screen / notification controls).
 * Periodically persists position + queue for crash-safe restore.
 */
@UnstableApi
class MhjPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var persistJob: Job? = null
    private lateinit var persistence: PlaybackPersistenceRepository
    private var restored = false

    private val persistListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            persistNow()
        }

        override fun onMediaItemTransition(
            mediaItem: androidx.media3.common.MediaItem?,
            reason: Int,
        ) {
            persistNow()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_READY) {
                persistNow()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = MhjDatabase.get(this)
        persistence = PlaybackPersistenceRepository(db.queueDao(), db.playbackStateDao())

        val exo = PlayerHolder.getOrCreatePlayer(this).also { player = it }
        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        exo.addListener(persistListener)

        val sessionActivity = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        mediaSession = PlayerHolder.getOrCreateSession(this, exo).also { session ->
            sessionActivity?.let { session.setSessionActivity(it) }
        }

        serviceScope.launch {
            restoreIfNeeded(exo)
        }
        startPersistLoop()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        persistNow()
        persistJob?.cancel()
        player?.removeListener(persistListener)
        // Do not release PlayerHolder here if we want quick resume across service restarts;
        // MediaSessionService lifecycle will recreate. Release only when process dies.
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        PlayerHolder.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startPersistLoop() {
        persistJob?.cancel()
        persistJob = serviceScope.launch {
            while (isActive) {
                persistNow()
                delay(5_000)
            }
        }
    }

    private fun persistNow() {
        val p = player ?: return
        serviceScope.launch(Dispatchers.IO) {
            val queue = buildList {
                for (i in 0 until p.mediaItemCount) {
                    val item = p.getMediaItemAt(i)
                    val meta = item.mediaMetadata
                    add(
                        MediaItemModel(
                            id = item.mediaId,
                            uri = item.localConfiguration?.uri?.toString().orEmpty(),
                            title = meta.title?.toString() ?: "Unknown",
                            artist = meta.artist?.toString() ?: "Unknown artist",
                            album = meta.albumTitle?.toString().orEmpty(),
                            durationMs = 0L,
                            artworkUri = meta.artworkUri?.toString(),
                            kind = MediaKind.MUSIC,
                        ),
                    )
                }
            }
            persistence.saveQueue(queue)
            val current = p.currentMediaItem?.let { item ->
                val meta = item.mediaMetadata
                MediaItemModel(
                    id = item.mediaId,
                    uri = item.localConfiguration?.uri?.toString().orEmpty(),
                    title = meta.title?.toString() ?: "Unknown",
                    artist = meta.artist?.toString() ?: "Unknown artist",
                    album = meta.albumTitle?.toString().orEmpty(),
                    durationMs = p.duration.coerceAtLeast(0L),
                    artworkUri = meta.artworkUri?.toString(),
                    kind = MediaKind.MUSIC,
                )
            }
            persistence.savePlaybackState(
                item = current,
                positionMs = p.currentPosition,
                durationMs = p.duration.coerceAtLeast(0L),
                wasPlaying = p.isPlaying || p.playWhenReady,
                shuffle = p.shuffleModeEnabled,
                repeatMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
            )
        }
    }

    private suspend fun restoreIfNeeded(exo: ExoPlayer) {
        if (restored || exo.mediaItemCount > 0) {
            restored = true
            return
        }
        val queue = persistence.loadQueue()
        val state = persistence.loadState()
        if (queue.isEmpty()) {
            restored = true
            return
        }
        val startIndex = queue.indexOfFirst { it.id == state?.mediaId }.coerceAtLeast(0)
        val position = state?.positionMs?.coerceAtLeast(0L) ?: 0L
        exo.setMediaItems(
            queue.map { it.toMediaItem() },
            startIndex,
            position,
        )
        exo.shuffleModeEnabled = state?.shuffle ?: false
        exo.repeatMode = when (state?.repeatMode) {
            RepeatMode.ONE.name -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL.name -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        exo.prepare()
        // Crash-safe: restore position but do not auto-play after unexpected death.
        exo.playWhenReady = false
        restored = true
    }
}
