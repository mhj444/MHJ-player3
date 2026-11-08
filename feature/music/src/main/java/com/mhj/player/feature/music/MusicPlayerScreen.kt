package com.mhj.player.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.model.RepeatMode
import com.mhj.player.core.ui.components.BrandLogoHeader
import com.mhj.player.core.ui.components.EmbossedCircle
import com.mhj.player.core.ui.components.EqualizerVisualizer
import com.mhj.player.core.ui.components.GlassPanel
import com.mhj.player.core.ui.components.goldRim
import com.mhj.player.core.ui.components.innerGoldGlow
import com.mhj.player.core.ui.components.neumorphRaised
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun MusicPlayerScreen(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenTopMenu: () -> Unit,
    onOpenBottomMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MhjTheme.colors
    val title = state.current?.title ?: "MHJ Player"
    val artist = state.current?.artist ?: "Ready when you are"
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenTopMenu) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Menu", tint = colors.primary)
                }
                BrandLogoHeader(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* Cast — Phase 5 */ }) {
                    Icon(
                        Icons.Outlined.Cast,
                        contentDescription = "Cast",
                        tint = colors.primary.copy(alpha = 0.55f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Embossed album well + gold-rimmed art frame
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .neumorphRaised(
                        shapeRadius = 30.dp,
                        light = colors.lightShadow,
                        dark = colors.darkShadow,
                        elevation = 16.dp,
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.elevated, colors.surface, colors.recess),
                        ),
                    )
                    .goldRim(radius = 30.dp, alpha = 0.95f)
                    .innerGoldGlow(radius = 30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .neumorphRaised(
                            shapeRadius = 22.dp,
                            light = colors.lightShadow,
                            dark = colors.darkShadow,
                            elevation = 8.dp,
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (colors.isDark) Color(0xFF3A2E18) else Color(0xFFF0E6D8),
                                    if (colors.isDark) Color(0xFF14100A) else Color(0xFFD9CFC0),
                                ),
                            ),
                        )
                        .border(
                            width = 1.4.dp,
                            brush = MhjColors.MetallicGold,
                            shape = RoundedCornerShape(22.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = colors.primary.copy(alpha = 0.55f),
                        modifier = Modifier.size(78.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = title,
                color = colors.onBackground,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = artist,
                color = colors.muted,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(12.dp))
            EqualizerVisualizer(
                active = state.isPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 6.dp),
            )

            Spacer(Modifier.height(6.dp))
            Slider(
                value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                onValueChange = { ratio ->
                    if (duration > 0) onSeek((ratio * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MhjColors.GoldBright,
                    activeTrackColor = MhjColors.Gold,
                    inactiveTrackColor = colors.controlFill,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(position), color = colors.muted, fontSize = 12.sp)
                Text(formatTime(duration), color = colors.muted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmbossedCircle(
                    onClick = onShuffle,
                    diameter = 46.dp,
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffle) colors.primary else colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                EmbossedCircle(onClick = onPrevious, diameter = 54.dp) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                EmbossedCircle(onClick = onPlayPause, diameter = 74.dp, filledGold = true) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play pause",
                        tint = if (colors.isDark) Color(0xFF111111) else Color(0xFF2A2116),
                        modifier = Modifier.size(36.dp),
                    )
                }
                EmbossedCircle(onClick = onNext, diameter = 54.dp) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                EmbossedCircle(onClick = onRepeat, diameter = 46.dp) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) colors.primary else colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            radius = 24.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavChip(icon = Icons.Filled.Home, label = "Home", selected = true, onClick = {})
                NavChip(
                    icon = Icons.Outlined.Folder,
                    label = "Library",
                    selected = false,
                    onClick = onOpenTopMenu,
                )
            }
        }
    }
}

@Composable
private fun NavChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MhjTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier.background(colors.controlFill, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) colors.primary else colors.muted)
        Text(
            text = label,
            color = if (selected) colors.primary else colors.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
