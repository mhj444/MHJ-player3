package com.mhj.player.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

/*
 * The "raised" look, drawn in code (no pictures): every card and button sits slightly above the
 * page — a soft light edge on the top-left, a soft shadow on the bottom-right, a gold hairline —
 * and sinks in a little when pressed. Works the same in the dark (gold) and light (porcelain) themes.
 */

/** Soft double shadow + body colour for a raised shape. [depth] 0 = flat / pressed in. */
fun Modifier.raised(
    radius: Dp,
    depth: Dp,
    body: Color,
    light: Color,
    dark: Color,
): Modifier = drawBehind {
    val r = radius.toPx().coerceAtMost(size.minDimension / 2f)
    val d = depth.toPx()
    if (d <= 0.2f) return@drawBehind
    drawIntoCanvas { c ->
        val p = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = body.toArgb()
        }
        p.setShadowLayer(d * 1.8f, d * 0.9f, d * 1.1f, dark.toArgb())
        c.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        p.setShadowLayer(d * 1.5f, -d * 0.7f, -d * 0.8f, light.toArgb())
        c.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
    }
}

/** Colours of the raised look for the current theme. */
object Emboss {
    val body: Color @Composable get() = MhjTheme.colors.elevated
    val light: Color @Composable get() = if (MhjTheme.colors.isDark) Color(0x1FFFF1C8) else Color(0xE6FFFFFF)
    val dark: Color @Composable get() = if (MhjTheme.colors.isDark) Color(0xD9000000) else Color(0x4D8B7355)

    /** Top-lit face: a little lighter at the top, a little darker at the bottom. */
    @Composable
    fun face(pressed: Boolean = false): Brush {
        val c = MhjTheme.colors
        return if (c.isDark) {
            if (pressed) Brush.verticalGradient(listOf(Color(0xFF0E0E10), Color(0xFF1A1A1D)))
            else Brush.verticalGradient(listOf(Color(0xFF26262A), Color(0xFF161618)))
        } else {
            if (pressed) Brush.verticalGradient(listOf(Color(0xFFE8DFD3), Color(0xFFF6F0E8)))
            else Brush.verticalGradient(listOf(Color(0xFFFFFDF9), Color(0xFFF0E8DD)))
        }
    }

    /** Gold edge: brighter on top, fading on the sides. */
    @Composable
    fun rim(alpha: Float = 0.55f): Brush = Brush.verticalGradient(
        listOf(MhjColors.GoldHi.copy(alpha = alpha), MhjColors.Gold.copy(alpha = alpha * 0.35f), MhjColors.GoldLo.copy(alpha = alpha * 0.7f)),
    )
}

/** A raised card. */
@Composable
fun RaisedCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    depth: Dp = 7.dp,
    rimAlpha: Float = 0.45f,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val d by animateDpAsState(if (pressed) 1.dp else depth, label = "depth")
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .raised(radius, d, Emboss.body, Emboss.light, Emboss.dark)
            .clip(shape)
            .background(Emboss.face(pressed))
            .border(1.dp, Emboss.rim(rimAlpha), shape)
            .then(if (onClick != null) Modifier.clickable(src, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
/** A round raised button with a gold icon; sinks in when pressed, glows gold when [active]. */
@Composable
fun RaisedIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val d by animateDpAsState(if (pressed) 1.dp else (size.value / 9f).dp, label = "d")
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, label = "s")
    val c = MhjTheme.colors
    Box(
        modifier
            .size(size)
            .scale(s)
            .raised(size / 2, d, Emboss.body, Emboss.light, Emboss.dark)
            .clip(CircleShape)
            .background(Emboss.face(pressed))
            .border(if (active) 1.5.dp else 1.dp, if (active) Brush.verticalGradient(listOf(MhjColors.GoldHi, MhjColors.Gold)) else Emboss.rim(0.5f), CircleShape)
            .then(
                Modifier.combinedClickable(src, indication = null, enabled = enabled, onLongClick = onLongClick, onClick = onClick)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(MhjColors.Gold.copy(alpha = 0.22f), Color.Transparent)))
        }
        Icon(
            icon, description,
            tint = when {
                !enabled -> c.muted.copy(alpha = 0.5f)
                active || c.isDark -> MhjColors.GoldHi
                else -> MhjColors.GoldLo
            },
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Gold switch with a raised knob in a sunken track. */
@Composable
fun GoldSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val x by animateFloatAsState(if (checked) 1f else 0f, label = "x")
    val c = MhjTheme.colors
    Box(
        modifier
            .size(width = 54.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (checked) Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.Gold))
                else Brush.verticalGradient(listOf(if (c.isDark) Color(0xFF0A0A0B) else Color(0xFFE2D8CA), c.elevated))
            )
            .border(1.dp, Emboss.rim(0.4f), RoundedCornerShape(15.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .padding(start = (24 * x).dp)
                .size(24.dp)
                .raised(12.dp, 3.dp, Emboss.body, Emboss.light, Emboss.dark)
                .clip(CircleShape)
                .background(Brush.verticalGradient(if (checked) listOf(MhjColors.GoldHi, MhjColors.Gold) else listOf(Color(0xFF3A3A3E), Color(0xFF222225)).let { if (c.isDark) it else listOf(Color.White, Color(0xFFEDE4D8)) }))
        )
    }
}

/** Row on a raised card: icon, title + small line, and something on the end (switch / button). */
@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val c = MhjTheme.colors
    RaisedCard(modifier.fillMaxWidth(), radius = 20.dp, depth = 6.dp, padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MhjColors.Gold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = c.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = c.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Square tile in the audio grid: icon, name, current value. */
@Composable
fun FeatureTile(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    RaisedCard(modifier, radius = 20.dp, depth = 6.dp, padding = 10.dp, rimAlpha = if (active) 0.9f else 0.4f, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (active || c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = c.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(value, color = if (active) MhjColors.Gold else c.muted, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Small raised pill button (text). */
@Composable
fun RaisedPill(text: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val c = MhjTheme.colors
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .raised(16.dp, if (pressed || selected) 1.dp else 4.dp, Emboss.body, Emboss.light, Emboss.dark)
            .clip(shape)
            .background(if (selected) Brush.verticalGradient(listOf(MhjColors.Gold.copy(alpha = 0.32f), MhjColors.Gold.copy(alpha = 0.16f))) else Emboss.face(pressed))
            .border(1.dp, if (selected) Brush.verticalGradient(listOf(MhjColors.GoldHi, MhjColors.Gold)) else Emboss.rim(0.35f), shape)
            .clickable(src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) (if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo) else c.onBackground, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

/** A page title inside a sheet, with a back button on the start side. */
@Composable
fun SheetTitle(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val c = MhjTheme.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            RaisedIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "برگشت", onBack, size = 40.dp, iconSize = 20.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        trailing()
    }
}

/** Page background drawn in code: deep gradient with a faint gold glow (no picture). */
@Composable
fun DrawnBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val dark = MhjTheme.colors.isDark
    Box(
        modifier
            .fillMaxSize()
            .background(
                if (dark) Brush.verticalGradient(listOf(Color(0xFF141416), Color(0xFF0B0B0C), Color(0xFF060606)))
                else Brush.verticalGradient(listOf(Color(0xFFFBF7F1), Color(0xFFF2EBE1), Color(0xFFE9E0D3)))
            )
            .drawBehind {
                val glow = if (dark) Color(0x22D4AF37) else Color(0x1FC9A227)
                drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.30f), radius = size.minDimension * 0.75f),
                    radius = size.minDimension * 0.75f, center = Offset(size.width * 0.5f, size.height * 0.30f))
                if (dark) {
                    // A few soft gold specks, always in the same places.
                    val specks = listOf(0.12f to 0.08f, 0.86f to 0.14f, 0.07f to 0.52f, 0.93f to 0.61f, 0.22f to 0.88f, 0.78f to 0.92f, 0.5f to 0.04f)
                    specks.forEachIndexed { i, (fx, fy) ->
                        val r = size.minDimension * (0.012f + (i % 3) * 0.006f)
                        drawCircle(Brush.radialGradient(listOf(Color(0x40E8C547), Color.Transparent), center = Offset(size.width * fx, size.height * fy), radius = r * 3),
                            radius = r * 3, center = Offset(size.width * fx, size.height * fy))
                    }
                }
            },
        content = content,
    )
}
