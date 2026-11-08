package com.mhj.player.core.playback.engine

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

/**
 * Process-wide ExoPlayer + MediaSession holder owned by the foreground service.
 * UI never touches ExoPlayer directly — only via MediaController / PlaybackController.
 */
object PlayerHolder {
    @Volatile
    private var player: ExoPlayer? = null

    @Volatile
    private var session: MediaSession? = null

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        player?.let { return it }
        return synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext)
                .setHandleAudioBecomingNoisy(true)
                .setSeekBackIncrementMs(10_000)
                .setSeekForwardIncrementMs(10_000)
                .build()
                .also { player = it }
        }
    }

    fun getOrCreateSession(context: Context, player: ExoPlayer): MediaSession {
        session?.let { return it }
        return synchronized(this) {
            session ?: MediaSession.Builder(context.applicationContext, player)
                .setId("mhj_player_session")
                .build()
                .also { session = it }
        }
    }

    fun playerOrNull(): ExoPlayer? = player

    fun sessionOrNull(): MediaSession? = session

    fun release() {
        synchronized(this) {
            session?.release()
            session = null
            player?.release()
            player = null
        }
    }
}
