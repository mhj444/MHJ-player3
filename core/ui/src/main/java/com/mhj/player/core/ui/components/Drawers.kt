package com.mhj.player.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun TopMenuDrawer(
    visible: Boolean,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayDrawer(
        visible = visible,
        fromTop = true,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        BrandLogoHeader(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Library",
            color = MhjTheme.colors.onBackground,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(14.dp))
        MenuRow(
            icon = Icons.Outlined.LibraryMusic,
            title = "Browse",
            subtitle = "Music & video",
            onClick = onDismiss,
        )
        MenuRow(
            icon = if (settings.theme == AppTheme.MIDNIGHT_GOLD) {
                Icons.Outlined.WbSunny
            } else {
                Icons.Outlined.DarkMode
            },
            title = "Theme",
            subtitle = when (settings.theme) {
                AppTheme.MIDNIGHT_GOLD -> "Midnight Gold"
                AppTheme.PORCELAIN_WHITE -> "Porcelain White"
            },
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
) {
    OverlayDrawer(
        visible = visible,
        fromTop = false,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = "Audio Processing",
            color = MhjTheme.colors.onBackground,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(14.dp))
        ToggleRow(
            label = "Dialogue Clarity Boost",
            checked = settings.dialogueClarity,
            onCheckedChange = onDialogueClarity,
        )
        ToggleRow(
            label = "Night Mode Audio",
            checked = settings.nightModeAudio,
            onCheckedChange = onNightMode,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                label = "Speed",
                value = String.format("%.1fx", settings.playbackSpeed),
                onClick = onSpeedClick,
            )
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.GraphicEq,
                label = "Equalizer",
                value = "Open",
                onClick = onEqClick,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                label = "Sleep",
                value = if (settings.sleepTimerMinutes == 0) "Off" else "${settings.sleepTimerMinutes}m",
                onClick = onTimerClick,
            )
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.NightsStay,
                label = "Night",
                value = if (settings.nightModeAudio) "On" else "Off",
                onClick = { onNightMode(!settings.nightModeAudio) },
            )
        }
    }
}

@Composable
private fun OverlayDrawer(
    visible: Boolean,
    fromTop: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    drawerBody: @Composable ColumnScope.() -> Unit,
) {
    val colors = MhjTheme.colors
    val sheetShape = RoundedCornerShape(
        topStart = if (fromTop) 0.dp else 30.dp,
        topEnd = if (fromTop) 0.dp else 30.dp,
        bottomStart = if (fromTop) 30.dp else 0.dp,
        bottomEnd = if (fromTop) 30.dp else 0.dp,
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            animationSpec = tween(280),
            initialOffsetY = { full -> if (fromTop) -full / 2 else full / 2 },
        ),
        exit = fadeOut(animationSpec = tween(160)) + slideOutVertically(
            animationSpec = tween(220),
            targetOffsetY = { full -> if (fromTop) -full / 2 else full / 2 },
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.48f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(if (fromTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .neumorphRaised(
                        shapeRadius = 30.dp,
                        light = colors.lightShadow,
                        dark = colors.darkShadow,
                        elevation = 18.dp,
                    )
                    .clip(sheetShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.glass.copy(alpha = 0.96f),
                                colors.elevated.copy(alpha = 0.92f),
                            ),
                        ),
                    )
                    .goldRim(radius = 30.dp, alpha = 0.55f)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                content = drawerBody,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MhjTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .neumorphRaised(
                shapeRadius = 18.dp,
                light = colors.lightShadow,
                dark = colors.darkShadow,
                elevation = 7.dp,
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(colors.elevated, colors.surface)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.controlFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MhjTheme.colors.onBackground, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MhjColors.GoldBright,
                checkedTrackColor = MhjColors.GoldOrange.copy(alpha = 0.55f),
                uncheckedThumbColor = MhjTheme.colors.muted,
                uncheckedTrackColor = MhjTheme.colors.recess,
            ),
        )
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmbossedTile(
        onClick = onClick,
        modifier = modifier.height(112.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MhjTheme.colors.controlFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = MhjTheme.colors.primary)
            }
            Text(text = label, color = MhjTheme.colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = MhjTheme.colors.muted, fontSize = 12.sp)
        }
    }
}
