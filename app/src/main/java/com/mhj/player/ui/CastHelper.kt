package com.mhj.player.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings

object CastHelper {
    /** Opens the system cast / wireless display picker. */
    fun openCastChooser(context: Context) {
        val candidates = listOf(
            Intent("android.settings.CAST_SETTINGS"),
            Intent(Settings.ACTION_CAST_SETTINGS),
            Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        )
        for (intent in candidates) {
            val ok = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (ok) return
        }
    }
}
