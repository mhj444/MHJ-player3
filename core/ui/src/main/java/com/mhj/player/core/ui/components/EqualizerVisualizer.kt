package com.mhj.player.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Mid-screen equalizer / waveform visualizer under track info.
 * Phase 1 uses a driven animation; hooks ready for real FFT later.
 */
@Composable
fun EqualizerVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    val colors = MhjTheme.colors
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 1800 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eqPhase",
    )
    val intensity = if (active) 1f else 0.22f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        val w = size.width
        val h = size.height
        val gap = w / (barCount * 2.4f)
        val barWidth = gap * 1.15f
        val totalBarsWidth = barCount * (barWidth + gap) - gap
        val startX = (w - totalBarsWidth) / 2f

        // Soft baseline waveform stroke behind bars
        val pathStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        var prev = Offset.Zero
        for (i in 0..barCount) {
            val t = i / barCount.toFloat()
            val x = startX + t * totalBarsWidth
            val wave = sin(phase + t * 8f) * 0.28f + sin(phase * 1.7f + t * 3.2f) * 0.18f
            val y = h / 2f - wave * h * 0.45f * intensity
            val point = Offset(x, y)
            if (i > 0) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(colors.visualizer.copy(alpha = 0.25f), colors.visualizer.copy(alpha = 0.55f)),
                    ),
                    start = prev,
                    end = point,
                    strokeWidth = pathStroke.width,
                    cap = StrokeCap.Round,
                )
            }
            prev = point
        }

        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = sin(t * PI).toFloat().coerceIn(0.15f, 1f)
            val wave = (
                sin(phase + i * 0.55f) * 0.45f +
                    sin(phase * 1.9f + i * 0.33f) * 0.35f +
                    0.35f
                ).coerceIn(0.12f, 1f)
            val barH = h * wave * envelope * intensity
            val x = startX + i * (barWidth + gap)
            val y = (h - barH) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.visualizer.copy(alpha = 0.95f),
                        colors.visualizer.copy(alpha = 0.35f),
                    ),
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
