package com.mhj.player.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground media playback service with MediaSession (lock-screen / notification controls).
 * Periodically persists position + queue for crash-safe restore.
 */
@UnstableApi
class MhjPlaybackService : MediaSessionService() {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Playback service coroutine failed", throwable)
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler)
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var persistJob: Job? = null
    private lateinit var persistence: PlaybackPersistenceRepository
    private var restored = false
    private val audio by lazy { com.mhj.player.core.playback.engine.AudioEngine(this) }
    private val settingsStore by lazy { com.mhj.player.core.data.datastore.SettingsDataStore(this) }
    private var sleepJob: Job? = null
    @Volatile private var sleepAtEnd = false

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
        runCatching {
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
            exo.addListener(sleepEndListener)

            // Our own audio session from the start, so the sound effects attach once and stay.
            runCatching {
                val sid = androidx.media3.common.util.Util.generateAudioSessionIdV21(this)
                exo.setAudioSessionId(sid)
                audio.attach(sid)
            }.onFailure { Log.e(TAG, "audio session", it) }
            observeSettings(exo)

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

            // Gold-on-dark MediaStyle notification (API-limited colorization)
            setMediaNotificationProvider(MhjMediaNotificationProvider(this))

            serviceScope.launch {
                restoreIfNeeded(exo)
            }
            startPersistLoop()
        }.onFailure { error ->
            Log.e(TAG, "Failed to start playback service", error)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /** Speed / pitch, sound effects and the sleep timer follow the saved settings. */
    private fun observeSettings(exo: ExoPlayer) {
        serviceScope.launch {
            settingsStore.settings.collect { s ->
                val pitch = if (s.keepNaturalPitch) 1f else s.playbackSpeed
                val want = androidx.media3.common.PlaybackParameters(s.playbackSpeed, pitch)
                if (exo.playbackParameters != want) exo.playbackParameters = want
                if (exo.audioSessionId > 0) audio.attach(exo.audioSessionId)
                audio.apply(s)
                sleepAtEnd = s.sleepAtEnd
                scheduleSleep(exo, s.sleepAtMs)
            }
        }
    }

    private var sleepTarget = 0L
    private fun scheduleSleep(exo: ExoPlayer, at: Long) {
        if (at == sleepTarget) return
        sleepTarget = at
        sleepJob?.cancel()
        if (at <= 0L) return
        sleepJob = serviceScope.launch {
            val fadeMs = 8_000L
            val wait = at - System.currentTimeMillis() - fadeMs
            if (wait > 0) delay(wait)
            // Fade out gently, pause, then put the volume back for next time.
            val start = exo.volume
            val steps = 40
            for (i in 1..steps) {
                exo.volume = start * (1f - i / steps.toFloat())
                delay(fadeMs / steps)
            }
            exo.pause()
            exo.volume = start
            settingsStore.clearSleep()
        }
    }

    /** "Stop at the end of this track": pause when the current item finishes. */
    private val sleepEndListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (sleepAtEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                player?.pause()
                serviceScope.launch { settingsStore.clearSleep() }
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (sleepAtEnd && playbackState == Player.STATE_ENDED) serviceScope.launch { settingsStore.clearSleep() }
        }
    }

    override fun onDestroy() {
        runCatching { audio.release() }
        runCatching { persistNow() }
        persistJob?.cancel()
        player?.removeListener(persistListener)
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

    /**
     * Snapshot ExoPlayer on the application (main) thread, then write Room on IO.
     * Accessing the player off-main previously crashed the process on first persist.
     */
    private fun persistNow() {
        val p = player ?: return
        if (!::persistence.isInitialized) return
        serviceScope.launch {
            val snapshot = runCatching { captureSnapshot(p) }.getOrElse { error ->
                Log.e(TAG, "Failed to snapshot player state", error)
                return@launch
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    persistence.saveQueue(snapshot.queue)
                    persistence.savePlaybackState(
                        item = snapshot.current,
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        wasPlaying = snapshot.wasPlaying,
                        shuffle = snapshot.shuffle,
                        repeatMode = snapshot.repeatMode,
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Failed to persist playback state", error)
                }
            }
        }
    }

    private fun captureSnapshot(p: ExoPlayer): PlaybackSnapshot {
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
                        kind = kindOf(meta),
                    ),
                )
            }
        }
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
                kind = kindOf(meta),
            )
        }
        return PlaybackSnapshot(
            queue = queue,
            current = current,
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

    private suspend fun restoreIfNeeded(exo: ExoPlayer) {
        if (restored || exo.mediaItemCount > 0) {
            restored = true
            return
        }
        val (queue, state) = withContext(Dispatchers.IO) {
            persistence.loadQueue() to persistence.loadState()
        }
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

    /** Keep music / video when the queue is saved (it was always saved as music). */
    private fun kindOf(meta: androidx.media3.common.MediaMetadata): MediaKind =
        meta.extras?.getString(com.mhj.player.core.playback.engine.MEDIA_KIND_EXTRA)
            ?.let { runCatching { MediaKind.valueOf(it) }.getOrNull() } ?: MediaKind.MUSIC

    private data class PlaybackSnapshot(
        val queue: List<MediaItemModel>,
        val current: MediaItemModel?,
        val positionMs: Long,
        val durationMs: Long,
        val wasPlaying: Boolean,
        val shuffle: Boolean,
        val repeatMode: RepeatMode,
    )

    companion object {
        private const val TAG = "MhjPlaybackService"
    }
}
