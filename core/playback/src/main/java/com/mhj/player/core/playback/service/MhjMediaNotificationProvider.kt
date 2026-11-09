package com.mhj.player.core.playback.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.mhj.player.core.playback.R

/**
 * Gold-on-dark media notification branding within Media3's MediaStyle limits.
 */
@UnstableApi
class MhjMediaNotificationProvider(
    private val appContext: Context,
) : DefaultMediaNotificationProvider(
    appContext,
    { NOTIFICATION_ID },
    CHANNEL_ID,
    R.string.mhj_playback_channel_name,
) {
    init {
        setSmallIcon(R.drawable.ic_stat_mhj_playback)
        ensureChannel()
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory,
    ): IntArray {
        ensureChannel()
        builder
            .setColor(GOLD)
            .setColorized(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setSmallIcon(R.drawable.ic_stat_mhj_playback)
        return super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.mhj_playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "MHJ Player playback"
                setShowBadge(false)
                enableLights(true)
                lightColor = GOLD
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "mhj_playback_gold"
        const val NOTIFICATION_ID = 0x4D484A
        const val GOLD = 0xFFD4AF37.toInt()
    }
}
