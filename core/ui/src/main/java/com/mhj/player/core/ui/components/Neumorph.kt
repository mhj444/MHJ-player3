package com.mhj.player.core.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Soft dual-shadow emboss — no harsh black strokes / no blown-out white halos. */
fun Modifier.softEmboss(
    radius: Dp = 24.dp,
    light: Color,
    dark: Color,
    elevation: Dp = 12.dp,
): Modifier = drawBehind {
    val r = radius.toPx()
    val e = elevation.toPx().coerceAtMost(22.dp.toPx())
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fw = paint.asFrameworkPaint()
        fw.isAntiAlias = true
        // soft dark (bottom-right) — stronger for board-like depth
        fw.setShadowLayer(
            e * 1.35f,
            e * 0.42f,
            e * 0.50f,
            dark.copy(alpha = (dark.alpha * 0.58f).coerceIn(0.16f, 0.48f)).toArgb(),
        )
        fw.color = Color.Transparent.toArgb()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        // soft light (top-left)
        fw.setShadowLayer(
            e * 1.05f,
            -e * 0.34f,
            -e * 0.34f,
            light.copy(alpha = (light.alpha * 0.70f).coerceIn(0.12f, 0.55f)).toArgb(),
        )
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        fw.setShadowLayer(0f, 0f, 0f, 0)
    }
}

fun Modifier.softInset(
    radius: Dp = 24.dp,
    light: Color,
    dark: Color,
): Modifier = drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        color = dark.copy(alpha = 0.35f),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 3.dp.toPx()),
        topLeft = Offset(1.5.dp.toPx(), 2.dp.toPx()),
        size = size.copy(width = size.width - 3.dp.toPx(), height = size.height - 3.dp.toPx()),
    )
    drawRoundRect(
        color = light.copy(alpha = 0.55f),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 2.dp.toPx()),
        topLeft = Offset((-0.5f).dp.toPx(), (-0.5f).dp.toPx()),
        size = size.copy(width = size.width + 1.dp.toPx(), height = size.height + 1.dp.toPx()),
    )
}

fun Modifier.softGoldHairline(radius: Dp = 28.dp): Modifier = border(
    width = 1.1.dp,
    brush = Brush.linearGradient(
        listOf(
            MhjColors.GoldHi.copy(alpha = 0.55f),
            MhjColors.Gold.copy(alpha = 0.35f),
            MhjColors.GoldLo.copy(alpha = 0.45f),
        ),
    ),
    shape = RoundedCornerShape(radius),
)

/**
 * Soft gold light halo — SoftLight bloom + colored (never black) glow shadow.
 * Avoids setShadowLayer black mattes that crust bitmap edges.
 */
fun Modifier.softGoldHalo(
    radius: Dp = 28.dp,
    bloom: Dp = 18.dp,
    intensity: Float = 0.55f,
): Modifier = drawBehind {
    val center = Offset(size.width / 2f, size.height / 2f)
    val rad = size.minDimension * 0.72f
    val gold = Color(0xFFE8C460)
    // SoftLight pool (no dark fringe)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                gold.copy(alpha = (0.55f * intensity).coerceIn(0.08f, 0.7f)),
                gold.copy(alpha = (0.22f * intensity).coerceIn(0.04f, 0.35f)),
                Color.Transparent,
            ),
            center = center,
            radius = rad,
        ),
        radius = rad,
        center = center,
        blendMode = BlendMode.Softlight,
    )
    // Additive-ish colored glow (gold RGB only)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                gold.copy(alpha = (0.32f * intensity).coerceIn(0.06f, 0.45f)),
                gold.copy(alpha = (0.10f * intensity).coerceIn(0.02f, 0.18f)),
                Color.Transparent,
            ),
            center = center,
            radius = rad * 0.92f,
        ),
        radius = rad * 0.92f,
        center = center,
        blendMode = BlendMode.Plus,
    )
}

@Composable
fun BoardBackground(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val dark = MhjTheme.colors.isDark
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier),
    ) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.bg_midnight_board else R.drawable.bg_porcelain_board,
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Soft gold bloom behind album only — backgrounds are chrome-free
        if (dark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0x28D4AF37), Color.Transparent),
                                center = Offset(size.width / 2f, size.height * 0.28f),
                                radius = size.minDimension * 0.48f,
                            ),
                            center = Offset(size.width / 2f, size.height * 0.28f),
                            radius = size.minDimension * 0.48f,
                        )
                    },
            )
        }
    }
}

@Composable
fun FrostPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = MhjTheme.colors
    Box(
        modifier = modifier
            .softEmboss(radius, c.lightShadow, c.darkShadow, 14.dp)
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        c.elevated.copy(alpha = if (c.isDark) 0.88f else 0.92f),
                        c.glass.copy(alpha = if (c.isDark) 0.78f else 0.86f),
                    ),
                ),
            )
            .softGoldHairline(radius),
        content = content,
    )
}

@Composable
fun EmbossCircle(
    onClick: () -> Unit,
    diameter: Dp,
    filledGold: Boolean = false,
    ringGold: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = MhjTheme.colors
    Box(
        modifier = modifier
            .size(diameter)
            .softEmboss(diameter / 2, c.lightShadow, c.darkShadow, if (filledGold || ringGold) 16.dp else 10.dp)
            .clip(CircleShape)
            .background(
                when {
                    filledGold -> MhjColors.GoldMetalV
                    else -> Brush.verticalGradient(listOf(c.elevated, c.surface, c.recess))
                },
            )
            .then(
                if (ringGold || filledGold) {
                    Modifier.border(width = 1.6.dp, brush = MhjColors.GoldRing, shape = CircleShape)
                } else {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(c.lightShadow.copy(alpha = 0.7f), c.darkShadow.copy(alpha = 0.25f)),
                        ),
                        shape = CircleShape,
                    )
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun EmbossTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = MhjTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .softEmboss(22.dp, c.lightShadow, c.darkShadow, 11.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .softGoldHairline(22.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// Back-compat aliases used by older call sites
fun Modifier.neumorphRaised(
    shapeRadius: Dp = 24.dp,
    light: Color,
    dark: Color,
    elevation: Dp = 10.dp,
): Modifier = softEmboss(shapeRadius, light, dark, elevation)

fun Modifier.goldRim(radius: Dp = 28.dp, alpha: Float = 0.75f): Modifier = softGoldHairline(radius)

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) = BoardBackground(modifier)

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) = FrostPanel(modifier, radius, content)

@Composable
fun EmbossedCircle(
    onClick: () -> Unit,
    diameter: Dp,
    filledGold: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = EmbossCircle(onClick, diameter, filledGold, ringGold = filledGold, modifier, content)

@Composable
fun EmbossedTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = EmbossTile(onClick, modifier, content)
