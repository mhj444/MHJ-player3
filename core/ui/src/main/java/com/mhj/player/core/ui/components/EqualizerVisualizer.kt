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
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.PI
import kotlin.math.sin

/** Board EQ: tall solid gold bars (not dotted dashes). */
@Composable
fun EqualizerVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 42,
) {
    val colors = MhjTheme.colors
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 1400 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eqPhase",
    )
    val intensity = if (active) 1f else 0.55f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val w = size.width
        val h = size.height
        val gap = w / (barCount * 2.35f)
        val barW = gap * 1.05f
        val total = barCount * (barW + gap) - gap
        val startX = (w - total) / 2f
        val baseY = h * 0.96f

        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = sin(t * PI).toFloat().coerceIn(0.18f, 1f)
            val wave = (
                sin(phase + i * 0.42f) * 0.48f +
                    sin(phase * 1.55f + i * 0.27f) * 0.32f +
                    0.45f
                ).coerceIn(0.22f, 1f)
            val barH = h * 0.94f * wave * envelope * intensity
            val x = startX + i * (barW + gap)
            val y = baseY - barH
            // soft glow behind each tall solid gold bar
            drawRoundRect(
                color = MhjColors.GoldGlow.copy(alpha = 0.28f),
                topLeft = Offset(x - 1.5f, y - 1.5f),
                size = Size(barW + 3f, barH + 3f),
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
