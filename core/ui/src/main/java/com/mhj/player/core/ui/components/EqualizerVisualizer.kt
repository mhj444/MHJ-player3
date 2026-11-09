package com.mhj.player.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Static gold bar decoration under the title — not a fake self-animating equalizer.
 * Heights are fixed from a sine envelope (no infinite transition).
 */
@Composable
fun EqualizerVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    val colors = MhjTheme.colors
    val intensity = if (active) 1f else 0.45f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        val w = size.width
        val h = size.height
        val gap = w / (barCount * 2.55f)
        val barW = gap * 1.15f
        val total = barCount * (barW + gap) - gap
        val startX = (w - total) / 2f
        val baseY = h * 0.98f

        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = sin(t * PI).toFloat().coerceIn(0.22f, 1f)
            // Deterministic static pattern (no time-based animation)
            val wave = (0.55f + 0.35f * sin(i * 0.55f).toFloat()).coerceIn(0.28f, 1f)
            val barH = h * 0.96f * wave * envelope * intensity
            val x = startX + i * (barW + gap)
            val y = baseY - barH
            drawRoundRect(
                color = MhjColors.GoldGlow.copy(alpha = 0.28f),
                topLeft = Offset(x - 2f, y - 2f),
                size = Size(barW + 4f, barH + 4f),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        MhjColors.GoldHi,
                        colors.visualizer,
                        MhjColors.GoldLo,
                    ),
                    startY = y,
                    endY = y + barH,
                ),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
