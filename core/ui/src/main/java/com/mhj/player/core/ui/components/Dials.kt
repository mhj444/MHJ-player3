package com.mhj.player.core.ui.components

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A round dial with a hand you can drag. Angles are screen angles: 0° = 3 o'clock, clockwise.
 * [wrap] = a full circle (the timer): the hand cannot jump from the end back to the start.
 * The value is 0..1 along the arc; [labels] are (position 0..1, text) printed around it.
 */
@Composable
fun ArcDial(
    fraction: Float,
    onChange: (Float) -> Unit,
    onRelease: () -> Unit,
    startDeg: Float,
    sweepDeg: Float,
    wrap: Boolean,
    labels: List<Pair<Float, String>>,
    minorTicks: Int,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit,
) {
    val c = MhjTheme.colors
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val typeface = remember { runCatching { ResourcesCompat.getFont(ctx, R.font.vazir_semibold) }.getOrNull() }
    val frac by rememberUpdatedState(fraction.coerceIn(0f, 1f))
    val change by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)
    val labelColor = c.muted.toArgb()
    val trackColor = c.groove

    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        val side = maxWidth
        // The raised face of the dial.
        Box(
            Modifier
                .size(side * 0.86f)
                .raised(side * 0.43f, 12.dp, Emboss.body, Emboss.light, Emboss.dark)
                .clip(CircleShape)
                .background(Emboss.face())
                .border(1.dp, Emboss.rim(0.5f), CircleShape),
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(startDeg, sweepDeg, wrap) {
                    fun toFraction(p: Offset, prev: Float): Float {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        var a = Math.toDegrees(atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())).toFloat()
                        if (a < 0) a += 360f
                        var rel = (a - startDeg + 720f) % 360f
                        if (!wrap && rel > sweepDeg) rel = if (rel - sweepDeg < (360f - sweepDeg) / 2f) sweepDeg else 0f
                        var f = (rel / sweepDeg).coerceIn(0f, 1f)
                        if (wrap) {
                            // Don't jump across 12 o'clock.
                            if (prev > 0.8f && f < 0.2f) f = 1f
                            if (prev < 0.2f && f > 0.8f) f = 0f
                        }
                        return f
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        // The middle of the dial is not part of the hand.
                        if (hypot(down.position.x - cx, down.position.y - cy) < size.width * 0.16f) return@awaitEachGesture
                        down.consume()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        var cur = toFraction(down.position, frac)
                        change(cur)
                        while (true) {
                            val ev = awaitPointerEvent()
                            val p = ev.changes.firstOrNull() ?: break
                            if (!p.pressed) break
                            if (p.positionChange() != Offset.Zero) {
                                p.consume()
                                cur = toFraction(p.position, cur)
                                change(cur)
                            }
                        }
                        release()
                    }
                },
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.36f
            val stroke = size.minDimension * 0.045f
            val tl = Offset(cx - radius, cy - radius)
            val arcSize = Size(radius * 2, radius * 2)
            // Sunken track.
            drawArc(trackColor, startDeg, sweepDeg, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(Color.Black.copy(alpha = 0.25f), startDeg, sweepDeg, false, tl.plus(Offset(0f, 1.5f)), arcSize, style = Stroke(stroke * 0.35f, cap = StrokeCap.Round))
            // Gold part.
            if (frac > 0.001f) {
                drawArc(
                    Brush.sweepGradient(listOf(MhjColors.GoldLo, MhjColors.Gold, MhjColors.GoldHi, MhjColors.GoldLo), Offset(cx, cy)),
                    startDeg, sweepDeg * frac, false, tl, arcSize, style = Stroke(stroke * 0.8f, cap = StrokeCap.Round),
                )
            }
            // Ticks.
            val total = minorTicks.coerceAtLeast(1)
            for (i in 0..total) {
                if (wrap && i == total) break
                val t = i / total.toFloat()
                val ang = Math.toRadians((startDeg + sweepDeg * t).toDouble())
                val major = labels.any { kotlin.math.abs(it.first - t) < 0.001f }
                val r1 = radius + stroke * 0.9f
                val r2 = r1 + if (major) stroke * 0.9f else stroke * 0.45f
                drawLine(
                    MhjColors.Gold.copy(alpha = if (major) 0.9f else 0.4f),
                    Offset(cx + cos(ang).toFloat() * r1, cy + sin(ang).toFloat() * r1),
                    Offset(cx + cos(ang).toFloat() * r2, cy + sin(ang).toFloat() * r2),
                    strokeWidth = if (major) 2.2f.dp.toPx() else 1.2f.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // Labels.
            drawIntoCanvas { cv ->
                val p = NativePaint().apply {
                    isAntiAlias = true
                    color = labelColor
                    textSize = size.minDimension * 0.045f
                    textAlign = NativePaint.Align.CENTER
                    typeface?.let { this.typeface = it }
                }
                val lr = radius + stroke * 2.6f
                labels.forEach { (t, s) ->
                    val ang = Math.toRadians((startDeg + sweepDeg * t).toDouble())
                    val x = cx + cos(ang).toFloat() * lr
                    val y = cy + sin(ang).toFloat() * lr + p.textSize * 0.35f
                    cv.nativeCanvas.drawText(s, x, y, p)
                }
            }
            // Hand.
            val ang = Math.toRadians((startDeg + sweepDeg * frac).toDouble())
            val tip = Offset(cx + cos(ang).toFloat() * radius, cy + sin(ang).toFloat() * radius)
            val tail = Offset(cx + cos(ang).toFloat() * radius * 0.30f, cy + sin(ang).toFloat() * radius * 0.30f)
            drawLine(Color.Black.copy(alpha = 0.35f), tail.plus(Offset(2f, 3f)), tip.plus(Offset(2f, 3f)), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Brush.linearGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi), tail, tip), tail, tip, strokeWidth = 3.5f.dp.toPx(), cap = StrokeCap.Round)
            knob(tip, stroke * 0.95f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
    }
}

/** A raised gold knob with a soft shadow. */
internal fun DrawScope.knob(at: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = 0.45f), r * 1.08f, at.plus(Offset(r * 0.18f, r * 0.28f)))
    drawCircle(Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo), at.plus(Offset(-r * 0.35f, -r * 0.4f)), r * 1.6f), r, at)
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.35f, at.plus(Offset(-r * 0.3f, -r * 0.35f)))
}

/** Smooth line through points (for the equalizer curve). */
internal fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val mx = (p0.x + p1.x) / 2f
        path.cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
    }
    return path
}
