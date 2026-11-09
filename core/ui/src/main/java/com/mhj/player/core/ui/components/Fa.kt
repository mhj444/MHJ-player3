package com.mhj.player.core.ui.components

import java.util.Locale

/** Small text helpers: Persian digits and clock-style times. */
object Fa {
    private const val DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    fun digits(s: String): String = if (com.mhj.player.core.model.L.en) s else buildString(s.length) {
        for (ch in s) append(if (ch in '0'..'9') DIGITS[ch - '0'] else if (ch == '.') '٫' else ch)
    }

    fun num(n: Int): String = digits(n.toString())

    /** 3:05 or 1:02:09 (Persian digits). */
    fun time(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) / 1000)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val t = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
        return digits(t)
    }

    /** 1.25× style speed. */
    fun speed(x: Float): String {
        val t = if (kotlin.math.abs(x - Math.round(x * 10) / 10f) < 0.001f) String.format(Locale.US, "%.1f", x)
        else String.format(Locale.US, "%.2f", x)
        return digits(t) + "×"
    }

    /** +3.5 / −2 dB */
    fun db(v: Float): String {
        val r = Math.round(v * 2) / 2f
        val t = if (r == r.toInt().toFloat()) r.toInt().toString() else String.format(Locale.US, "%.1f", r)
        return when {
            r > 0f -> "+" + digits(t)
            r < 0f -> "−" + digits(t.removePrefix("-"))
            else -> digits("0")
        }
    }

    fun minutes(m: Int): String = if (com.mhj.player.core.model.L.en) when {
        m <= 0 -> "Off"
        m % 60 == 0 -> "${m / 60} h"
        m > 60 -> "${m / 60} h ${m % 60} min"
        else -> "$m min"
    } else when {
        m <= 0 -> "خاموش"
        m % 60 == 0 -> "${num(m / 60)} ساعت"
        m > 60 -> "${num(m / 60)} ساعت و ${num(m % 60)} دقیقه"
        else -> "${num(m)} دقیقه"
    }
}
