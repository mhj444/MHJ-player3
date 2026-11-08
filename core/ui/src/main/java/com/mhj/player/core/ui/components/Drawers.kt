package com.mhj.player.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TopMenuDrawer(
    visible: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    OverlaySheet(
        visible = visible,
        fromTop = true,
        onDismiss = onDismiss,
        modifier = modifier,
        hazeState = hazeState,
    ) {
        BrandLogoHeader()
        Spacer(Modifier.height(16.dp))
        Text(
            "Library",
            color = MhjTheme.colors.onBackground,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        MenuCard(
            icon = Icons.Outlined.LibraryMusic,
            title = "Browse",
            subtitle = "Music & video",
            onClick = onDismiss,
        )
        Spacer(Modifier.height(10.dp))
        MenuCard(
            icon = if (settings.theme == AppTheme.MIDNIGHT_GOLD) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
            title = "Theme",
            subtitle = if (settings.theme == AppTheme.MIDNIGHT_GOLD) "Midnight Gold" else "Porcelain White",
            onClick = onToggleTheme,
        )
    }
}

@Composable
fun BottomAudioDrawer(
    visible: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onDialogueClarity: (Boolean) -> Unit,
    onNightMode: (Boolean) -> Unit,
    onSpeedClick: () -> Unit,
    onEqClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var showDials by remember { mutableStateOf(false) }

    OverlaySheet(
        visible = visible,
        fromTop = false,
        onDismiss = onDismiss,
        modifier = modifier,
        hazeState = hazeState,
    ) {
        if (!showDials) {
            Text(
                "Audio Processing",
                color = MhjTheme.colors.primary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow("Dialogue Clarity Boost", settings.dialogueClarity, onDialogueClarity)
            ToggleRow("Night Mode Audio", settings.nightModeAudio, onNightMode)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridTile(Modifier.weight(1f), Icons.Outlined.Repeat, "A-B", "Loop") { }
                GridTile(Modifier.weight(1f), Icons.Outlined.Speed, "Speed", String.format("%.1fx", settings.playbackSpeed)) {
                    showDials = true
                    onSpeedClick()
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GridTile(Modifier.weight(1f), Icons.Outlined.GraphicEq, "Equalizer", "Open", onEqClick)
                GridTile(
                    Modifier.weight(1f),
                    Icons.Outlined.Timer,
                    "Timer",
                    if (settings.sleepTimerMinutes == 0) "Off" else "${settings.sleepTimerMinutes}m",
                ) {
                    showDials = true
                    onTimerClick()
                }
            }
        } else {
            SpeedTimerDials(
                settings = settings,
                onBack = { showDials = false },
                onSpeedClick = onSpeedClick,
                onTimerClick = onTimerClick,
            )
        }
    }
}

@Composable
private fun SpeedTimerDials(
    settings: AppSettings,
    onBack: () -> Unit,
    onSpeedClick: () -> Unit,
    onTimerClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Text(
        "Playback Speed",
        color = c.primary,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        modifier = Modifier.clickable(onClick = onBack),
    )
    Spacer(Modifier.height(8.dp))
    DialCard(
        centerLabel = String.format("%.1fx", settings.playbackSpeed),
        progress = ((settings.playbackSpeed - 0.25f) / 2.75f).coerceIn(0f, 1f),
        ticks = listOf("0.5x", "1.0x", "1.5x", "2.0x", "3.0x"),
        onClick = onSpeedClick,
    )
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmbossCircle(onClick = onSpeedClick, diameter = 40.dp) {
            Text("−", color = c.primary, fontSize = 20.sp)
        }
        Text("1.0x", color = c.muted, fontSize = 13.sp)
        EmbossCircle(onClick = onSpeedClick, diameter = 40.dp) {
            Text("+", color = c.primary, fontSize = 20.sp)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Sleep Timer",
        color = c.primary,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
    Spacer(Modifier.height(8.dp))
    DialCard(
        centerLabel = if (settings.sleepTimerMinutes == 0) "Off" else "${settings.sleepTimerMinutes} min",
        progress = (settings.sleepTimerMinutes / 60f).coerceIn(0f, 1f).let { if (it == 0f) 0.08f else it },
        ticks = emptyList(),
        onClick = onTimerClick,
    )
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillChip(Modifier.weight(1f), Icons.Outlined.AllInclusive, "Off", settings.sleepTimerMinutes == 0, onTimerClick)
        PillChip(Modifier.weight(1f), Icons.Outlined.NightsStay, "At end", false, onTimerClick)
        PillChip(Modifier.weight(1f), Icons.Outlined.Timer, "Custom", settings.sleepTimerMinutes > 0, onTimerClick)
    }
}

@Composable
private fun DialCard(
    centerLabel: String,
    progress: Float,
    ticks: List<String>,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .softGoldHalo(24.dp, bloom = 20.dp, intensity = 0.5f)
            .softEmboss(24.dp, c.lightShadow, c.darkShadow, 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .softGoldHairline(24.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Canvas speed / sleep dial — shiny gold arc + needle (board tech note)
        Canvas(Modifier.size(148.dp)) {
            val stroke = 10.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f - stroke
            // Track well
            drawArc(
                color = c.controlFill.copy(alpha = 0.85f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Gold progress with light halo underlay
            drawArc(
                brush = Brush.radialGradient(
                    listOf(MhjColors.GoldHi.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius + stroke,
                ),
                startAngle = 140f,
                sweepAngle = 260f * progress,
                useCenter = false,
                style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(MhjColors.GoldLo, MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo),
                    center = Offset(cx, cy),
                ),
                startAngle = 140f,
                sweepAngle = 260f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            ticks.forEachIndexed { i, _ ->
                val t = i / (ticks.size - 1f).coerceAtLeast(1f)
                val ang = Math.toRadians((140.0 + 260.0 * t))
                val rOuter = radius + stroke * 0.15f
                val rInner = rOuter - 10.dp.toPx()
                drawLine(
                    color = MhjColors.GoldHi.copy(alpha = 0.55f),
                    start = Offset(cx + cos(ang).toFloat() * rInner, cy + sin(ang).toFloat() * rInner),
                    end = Offset(cx + cos(ang).toFloat() * rOuter, cy + sin(ang).toFloat() * rOuter),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // Needle
            val nang = Math.toRadians((140.0 + 260.0 * progress.coerceIn(0f, 1f)))
            val nx = cx + cos(nang).toFloat() * (radius - 6.dp.toPx())
            val ny = cy + sin(nang).toFloat() * (radius - 6.dp.toPx())
            drawLine(
                brush = Brush.linearGradient(listOf(MhjColors.GoldHi, MhjColors.GoldLo)),
                start = Offset(cx, cy),
                end = Offset(nx, ny),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.GoldLo)),
                radius = 7.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
        Text(
            centerLabel,
            color = c.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 52.dp),
        )
    }
}

@Composable
private fun OverlaySheet(
    visible: Boolean,
    fromTop: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    val c = MhjTheme.colors
    val shape = RoundedCornerShape(
        topStart = if (fromTop) 0.dp else 32.dp,
        topEnd = if (fromTop) 0.dp else 32.dp,
        bottomStart = if (fromTop) 32.dp else 0.dp,
        bottomEnd = if (fromTop) 32.dp else 0.dp,
    )
    val frostStyle = HazeStyle(
        backgroundColor = c.elevated.copy(alpha = 0.55f),
        tints = listOf(
            HazeTint(c.glass.copy(alpha = if (c.isDark) 0.45f else 0.55f)),
            HazeTint(MhjColors.Gold.copy(alpha = 0.06f)),
        ),
        blurRadius = 22.dp,
        noiseFactor = HazeDefaults.noiseFactor,
    )
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(160)) + slideInVertically(tween(280)) { if (fromTop) -it / 2 else it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(220)) { if (fromTop) -it / 2 else it / 2 },
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.background.copy(alpha = 0.28f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(if (fromTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .softEmboss(32.dp, c.lightShadow, c.darkShadow, 18.dp)
                    .clip(shape)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(state = hazeState, style = frostStyle)
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        c.glass.copy(alpha = 0.92f),
                                        c.elevated.copy(alpha = 0.90f),
                                    ),
                                ),
                            )
                        },
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                c.glass.copy(alpha = if (hazeState != null) 0.42f else 0.96f),
                                c.elevated.copy(alpha = if (hazeState != null) 0.38f else 0.94f),
                            ),
                        ),
                    )
                    .softGoldHairline(32.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                content = body,
            )
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val c = MhjTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.controlFill),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = c.primary) }
        Column {
            Text(title, color = c.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MhjTheme.colors.onBackground, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MhjColors.GoldHi,
                checkedTrackColor = MhjColors.Gold.copy(alpha = 0.55f),
                uncheckedThumbColor = MhjTheme.colors.muted,
                uncheckedTrackColor = MhjTheme.colors.recess,
            ),
        )
    }
}

@Composable
private fun GridTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    EmbossTile(onClick = onClick, modifier = modifier.height(108.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MhjTheme.colors.primary, modifier = Modifier.size(26.dp))
            Text(label, color = MhjTheme.colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(value, color = MhjTheme.colors.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PillChip(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Row(
        modifier = modifier
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) c.controlFill
                else c.elevated,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) c.primary else c.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, color = if (selected) c.primary else c.muted, fontSize = 12.sp)
    }
}
