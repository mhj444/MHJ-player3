package com.mhj.player.core.playback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.mhj.player.core.playback.R

/**
 * Pushes gold-on-dark MediaStyle branding as far as the platform allows.
 *
 * Honest limit: many OEMs (and Android 13+ shade media cards) ignore colorized
 * media notifications and keep a white system card. We still set a dark colorized
 * channel, brand small icon, and dark large icon.
 */
@UnstableApi
class MhjMediaNotificationProvider(
    private val appContext: Context,
) : MediaNotification.Provider {

    private val delegate = DefaultMediaNotificationProvider(
        appContext,
        { NOTIFICATION_ID },
        CHANNEL_ID,
        R.string.mhj_playback_channel_name,
    ).also { it.setSmallIcon(R.drawable.ic_stat_mhj_playback) }

    private val largeIcon: Bitmap by lazy { brandLargeIcon() }

    init {
        ensureChannel(recreate = true)
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        ensureChannel(recreate = false)
        val built = delegate.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
        ) { updated ->
            onNotificationChangedCallback.onNotificationChanged(
                MediaNotification(updated.notificationId, brand(updated.notification)),
            )
        }
        return MediaNotification(built.notificationId, brand(built.notification))
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle,
    ): Boolean = delegate.handleCustomCommand(session, action, extras)

    private fun brand(source: Notification): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = Notification.Builder.recoverBuilder(appContext, source)
            builder
                .setSmallIcon(R.drawable.ic_stat_mhj_playback)
                .setColor(DARK_SURFACE)
            // Keep the song's own cover; when it has none, show the MHJ logo.
            if (source.getLargeIcon() == null) builder.setLargeIcon(largeIcon)
            builder
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setColorized(true)
                builder.setChannelId(CHANNEL_ID)
            }
            return builder.build().also { applyExtras(it) }
        }
        return source.also {
            it.color = DARK_SURFACE
            applyExtras(it)
        }
    }

    private fun applyExtras(notification: Notification) {
        notification.extras.putInt("android.colorized", 1)
        notification.extras.putInt("android.appInfoAccentColor", GOLD)
        notification.extras.putBoolean("android.media.isSessionNotification", true)
        notification.color = DARK_SURFACE
    }

    private fun brandLargeIcon(): Bitmap =
        runCatching { android.graphics.BitmapFactory.decodeResource(appContext.resources, R.drawable.notif_logo) }.getOrNull()
            ?: Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).also { it.eraseColor(DARK_SURFACE) }

    private fun ensureChannel(recreate: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (recreate) {
            nm.deleteNotificationChannel("mhj_playback_gold")
            nm.deleteNotificationChannel("mhj_playback_gold_dark_v3")
            runCatching { nm.deleteNotificationChannel(DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID) }
        }
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.mhj_playback_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "MHJ Player · gold on dark"
                setShowBadge(false)
                enableLights(true)
                lightColor = GOLD
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "mhj_playback_gold_dark_v4"
        const val NOTIFICATION_ID = 0x4D484A
        const val GOLD = 0xFFC9A96A.toInt()
        const val DARK_SURFACE = 0xFF14110E.toInt()
    }
}
