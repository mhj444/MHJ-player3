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

/** Board-style mid EQ: discrete gold bars with dotted baseline. */
@Composable
fun EqualizerVisualizer(
    active: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
) {
    val colors = MhjTheme.colors
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 1600 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eqPhase",
    )
    val intensity = if (active) 1f else 0.28f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
    ) {
        val w = size.width
        val h = size.height
        val gap = w / (barCount * 2.6f)
        val barW = gap * 1.05f
        val total = barCount * (barW + gap) - gap
        val startX = (w - total) / 2f
        val midY = h * 0.55f
        val dotR = 1.1.dp.toPx()

        val dots = barCount * 2
        for (i in 0 until dots) {
            val x = startX + (i / (dots - 1f)) * total
            drawCircle(
                color = colors.visualizer.copy(alpha = 0.35f),
                radius = dotR,
                center = Offset(x, midY + h * 0.28f),
            )
        }

        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = sin(t * PI).toFloat().coerceIn(0.18f, 1f)
            val wave = (
                sin(phase + i * 0.48f) * 0.42f +
                    sin(phase * 1.7f + i * 0.29f) * 0.32f +
                    0.38f
                ).coerceIn(0.14f, 1f)
            val barH = h * 0.78f * wave * envelope * intensity
            val x = startX + i * (barW + gap)
            val y = midY - barH / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        MhjColors.GoldHi.copy(alpha = 0.95f),
                        colors.visualizer.copy(alpha = 0.85f),
                        MhjColors.GoldLo.copy(alpha = 0.55f),
                    ),
                ),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
