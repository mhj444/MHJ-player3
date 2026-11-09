package com.mhj.player.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.Eq
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.roundToInt

private fun freqLabel(hz: Int): String = if (hz >= 1000) Fa.num(hz / 1000) + "k" else Fa.num(hz)

/**
 * Ten up/down sliders on one raised board, bass on the left. Drag a slider up or down; the gold
 * curve shows the shape of the sound. Values snap to half a decibel.
 */
@Composable
fun EqBoard(
    gains: List<Float>,
    enabled: Boolean,
    onGain: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp,
) {
    val c = MhjTheme.colors
    var local by remember { mutableStateOf(gains) }
    var active by remember { mutableIntStateOf(-1) }
    LaunchedEffect(gains) { if (active < 0) local = gains }
    val cur by rememberUpdatedState(local)
    val send by rememberUpdatedState(onGain)
    val trackColor = if (c.isDark) Color(0xFF0A0A0B) else Color(0xFFE2D8CA)
    val max = Eq.MAX_DB

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier.alpha(if (enabled) 1f else 0.45f)) {
            Text(
                if (active >= 0) "${freqLabel(Eq.FREQS[active])} هرتز   ${Fa.db(local.getOrElse(active) { 0f })} dB" else "بالا و پایین بکشید",
                color = if (active >= 0) MhjColors.Gold else c.muted,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val padY = 18.dp.toPx()
                        fun bandAt(x: Float) = ((x / size.width) * Eq.BANDS).toInt().coerceIn(0, Eq.BANDS - 1)
                        fun dbAt(y: Float): Float {
                            val t = ((y - padY) / (size.height - 2 * padY)).coerceIn(0f, 1f)
                            val v = max - t * 2 * max
                            return (v * 2).roundToInt() / 2f
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val b = bandAt(down.position.x)
                            active = b
                            fun set(v: Float) {
                                if (cur.getOrElse(b) { 0f } == v) return
                                local = cur.toMutableList().also { it[b] = v }
                                send(b, v)
                            }
                            set(dbAt(down.position.y))
                            while (true) {
                                val ev = awaitPointerEvent()
                                val p = ev.changes.firstOrNull() ?: break
                                if (!p.pressed) break
                                p.consume()
                                set(dbAt(p.position.y))
                            }
                            active = -1
                        }
                    },
            ) {
                val padY = 18.dp.toPx()
                val colW = size.width / Eq.BANDS
                val h = size.height - 2 * padY
                fun yOf(v: Float) = padY + (max - v) / (2 * max) * h
                // Guide lines: +12, +6, 0, −6, −12.
                listOf(max, max / 2, 0f, -max / 2, -max).forEach { v ->
                    drawLine(
                        MhjColors.Gold.copy(alpha = if (v == 0f) 0.45f else 0.12f),
                        Offset(0f, yOf(v)), Offset(size.width, yOf(v)),
                        strokeWidth = if (v == 0f) 1.4f.dp.toPx() else 1f,
                    )
                }
                val tw = 7.dp.toPx()
                val pts = ArrayList<Offset>(Eq.BANDS)
                for (i in 0 until Eq.BANDS) {
                    val x = colW * (i + 0.5f)
                    // Sunken slot.
                    drawRoundRect(trackColor, Offset(x - tw / 2, padY), Size(tw, h), CornerRadius(tw / 2))
                    drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(x - tw / 2, padY), Size(tw, h), CornerRadius(tw / 2), style = Stroke(1f))
                    val v = local.getOrElse(i) { 0f }
                    val y = yOf(v)
                    val y0 = yOf(0f)
                    if (kotlin.math.abs(y - y0) > 1f) {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo)),
                            Offset(x - tw / 2 + 1f, minOf(y, y0)), Size(tw - 2f, kotlin.math.abs(y - y0)), CornerRadius(tw / 2),
                        )
                    }
                    pts += Offset(x, y)
                }
                // The curve.
                drawPath(smoothPath(pts), MhjColors.GoldHi.copy(alpha = 0.55f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                pts.forEachIndexed { i, p -> knob(p, (if (i == active) 13 else 10).dp.toPx()) }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Eq.FREQS.forEach { hz ->
                    Text(freqLabel(hz), color = c.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** A sideways slider with a sunken track and a raised gold knob. */
@Composable
fun GoldSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 0.5f,
    onRelease: () -> Unit = {},
) {
    val c = MhjTheme.colors
    var local by remember { mutableStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!dragging) local = value }
    val cur by rememberUpdatedState(local)
    val send by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onRelease)
    val trackColor = if (c.isDark) Color(0xFF0A0A0B) else Color(0xFFE2D8CA)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(range, step) {
                    val pad = 16.dp.toPx()
                    fun at(x: Float): Float {
                        val t = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                        val v = range.start + t * (range.endInclusive - range.start)
                        return (Math.round(v / step) * step).coerceIn(range.start, range.endInclusive)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        dragging = true
                        fun set(v: Float) { if (v != cur) { local = v; send(v) } }
                        set(at(down.position.x))
                        while (true) {
                            val ev = awaitPointerEvent()
                            val p = ev.changes.firstOrNull() ?: break
                            if (!p.pressed) break
                            p.consume()
                            set(at(p.position.x))
                        }
                        dragging = false
                        done()
                    }
                },
        ) {
            val pad = 16.dp.toPx()
            val th = 7.dp.toPx()
            val cy = size.height / 2
            val w = size.width - 2 * pad
            drawRoundRect(trackColor, Offset(pad, cy - th / 2), Size(w, th), CornerRadius(th / 2))
            val t = (local - range.start) / (range.endInclusive - range.start)
            val zeroT = ((0f - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            val x = pad + w * t
            val x0 = pad + w * zeroT
            drawRoundRect(
                Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi)),
                Offset(minOf(x, x0), cy - th / 2 + 1f), Size(kotlin.math.abs(x - x0), th - 2f), CornerRadius(th / 2),
            )
            knob(Offset(x, cy), 11.dp.toPx())
        }
    }
}
