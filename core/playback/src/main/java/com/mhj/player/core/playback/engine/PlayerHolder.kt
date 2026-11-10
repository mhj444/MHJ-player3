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
            player ?: ExoPlayer.Builder(
                context.applicationContext,
                // Phone decoders first (best quality, hardware), FFmpeg for formats the phone lacks;
                // if a hardware decoder fails, try the next one instead of stopping.
                androidx.media3.exoplayer.DefaultRenderersFactory(context.applicationContext)
                    .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    .setEnableDecoderFallback(true),
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    context.applicationContext,
                    androidx.media3.extractor.DefaultExtractorsFactory()
                        // Accurate seeking in MP3 / AAC files without an index.
                        .setConstantBitrateSeekingEnabled(true)
                        .setConstantBitrateSeekingAlwaysEnabled(true),
                ),
            )
                .setHandleAudioBecomingNoisy(true)
                // Keep the processor awake while playing with the screen off (no stutter or gaps).
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
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
