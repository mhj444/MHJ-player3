package com.mhj.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

/**
 * Neumorphic raised surface: dual soft shadows + drawn highlight/shade rims
 * so depth still reads on software renderers where blur shadows are weak.
 */
fun Modifier.neumorphRaised(
    shapeRadius: Dp = 24.dp,
    light: Color,
    dark: Color,
    elevation: Dp = 10.dp,
): Modifier = this
    .drawBehind {
        val r = shapeRadius.toPx()
        val e = elevation.toPx()
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val fw = paint.asFrameworkPaint()
            fw.isAntiAlias = true
            fw.setShadowLayer(e * 1.4f, e * 0.65f, e * 0.85f, dark.copy(alpha = 0.85f).toArgb())
            fw.color = Color.Transparent.toArgb()
            canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
            fw.setShadowLayer(e * 1.1f, -e * 0.55f, -e * 0.55f, light.copy(alpha = 0.7f).toArgb())
            canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
            fw.setShadowLayer(0f, 0f, 0f, 0)
        }
        // Explicit rim lighting (GPU-independent)
        val stroke = 2.2.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(light.copy(alpha = 0.75f), Color.Transparent, dark.copy(alpha = 0.55f)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = stroke),
        )
    }

fun Modifier.goldRim(radius: Dp = 28.dp, alpha: Float = 0.85f): Modifier = border(
    width = 1.6.dp,
    brush = Brush.linearGradient(
        listOf(
            MhjColors.GoldBright.copy(alpha = alpha),
            MhjColors.Gold.copy(alpha = alpha),
            MhjColors.GoldDeep.copy(alpha = alpha * 0.9f),
            MhjColors.GoldBright.copy(alpha = alpha * 0.65f),
        ),
    ),
    shape = RoundedCornerShape(radius),
)

fun Modifier.innerGoldGlow(radius: Dp = 28.dp): Modifier = drawWithContent {
    drawContent()
    val r = radius.toPx()
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(MhjColors.Gold.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width / 2f, size.height * 0.28f),
            radius = size.minDimension * 0.7f,
        ),
        cornerRadius = CornerRadius(r, r),
        size = Size(size.width, size.height),
    )
}

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val colors = MhjTheme.colors
    val brush = if (colors.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF241A0A),
                Color(0xFF0C0C0C),
                Color(0xFF000000),
                Color(0xFF120C04),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFDF9),
                Color(0xFFF6F0E7),
                Color(0xFFEADCCB),
                Color(0xFFF3EBE1),
            ),
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
            .drawBehind {
                val glow = if (colors.isDark) Color(0x55D4AF37) else Color(0x33B8860B)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.30f),
                        radius = size.minDimension * 0.62f,
                    ),
                    radius = size.minDimension * 0.62f,
                    center = Offset(size.width / 2f, size.height * 0.30f),
                )
            },
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MhjTheme.colors
    Box(
        modifier = modifier
            .neumorphRaised(
                shapeRadius = radius,
                light = colors.lightShadow,
                dark = colors.darkShadow,
                elevation = 12.dp,
            )
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.elevated.copy(alpha = 0.95f),
                        colors.glass.copy(alpha = 0.88f),
                        colors.surface.copy(alpha = 0.92f),
                    ),
                ),
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colors.lightShadow.copy(alpha = 0.75f),
                        colors.border.copy(alpha = 0.9f),
                        colors.darkShadow.copy(alpha = 0.4f),
                    ),
                ),
                shape = RoundedCornerShape(radius),
            ),
        content = content,
    )
}

@Composable
fun EmbossedCircle(
    onClick: () -> Unit,
    diameter: Dp,
    filledGold: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MhjTheme.colors
    Box(
        modifier = modifier
            .size(diameter)
            .neumorphRaised(
                shapeRadius = diameter / 2,
                light = colors.lightShadow,
                dark = colors.darkShadow,
                elevation = if (filledGold) 16.dp else 11.dp,
            )
            .clip(CircleShape)
            .background(
                if (filledGold) {
                    Brush.verticalGradient(
                        listOf(
                            MhjColors.GoldBright,
                            MhjColors.Gold,
                            MhjColors.GoldDeep,
                            MhjColors.Gold,
                        ),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            colors.elevated,
                            colors.surface,
                            colors.recess,
                        ),
                    )
                },
            )
            .border(
                width = 1.3.dp,
                brush = if (filledGold) {
                    Brush.linearGradient(listOf(MhjColors.GoldBright, MhjColors.GoldDeep))
                } else {
                    Brush.linearGradient(
                        listOf(
                            colors.lightShadow.copy(alpha = 0.8f),
                            colors.border,
                            colors.darkShadow.copy(alpha = 0.45f),
                        ),
                    )
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun EmbossedTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MhjTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .neumorphRaised(
                shapeRadius = 20.dp,
                light = colors.lightShadow,
                dark = colors.darkShadow,
                elevation = 10.dp,
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(colors.elevated, colors.surface, colors.recess.copy(alpha = 0.9f)),
                ),
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    listOf(colors.lightShadow.copy(alpha = 0.7f), colors.border),
                ),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
