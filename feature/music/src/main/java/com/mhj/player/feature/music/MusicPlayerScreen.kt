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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.model.RepeatMode
import com.mhj.player.core.ui.components.BrandLogoHeader
import com.mhj.player.core.ui.components.EqualizerVisualizer
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
    val title = state.current?.title ?: "Nothing playing"
    val artist = state.current?.artist ?: "Pick a track from Library"
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenTopMenu) {
                Icon(Icons.Outlined.Settings, contentDescription = "Menu", tint = colors.primary)
            }
            BrandLogoHeader()
            IconButton(onClick = { /* Cast hook — Phase 5 */ }) {
                Icon(Icons.Outlined.Cast, contentDescription = "Cast", tint = colors.primary.copy(alpha = 0.55f))
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = colors.primary.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(28.dp))
                .border(1.5.dp, colors.border, RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.elevated, colors.surface, colors.background),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = colors.primary.copy(alpha = 0.55f),
                modifier = Modifier.size(72.dp),
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 22.sp,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(14.dp))
        EqualizerVisualizer(
            active = state.isPlaying,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(10.dp))
        Slider(
            value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            onValueChange = { ratio ->
                if (duration > 0) onSeek((ratio * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
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

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportIcon(
                onClick = onShuffle,
                tint = if (state.shuffle) colors.primary else colors.muted,
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = it)
            }
            TransportIcon(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = it, modifier = Modifier.size(34.dp))
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play pause",
                    tint = colors.background,
                    modifier = Modifier.size(36.dp),
                )
            }
            TransportIcon(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = it, modifier = Modifier.size(34.dp))
            }
            TransportIcon(
                onClick = onRepeat,
                tint = if (state.repeatMode != RepeatMode.OFF) colors.primary else colors.muted,
            ) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = it,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.elevated.copy(alpha = 0.55f))
                .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavChip(icon = Icons.Filled.Home, label = "Home", selected = true, onClick = {})
            NavChip(icon = Icons.Outlined.Folder, label = "Library", selected = false, onClick = onOpenTopMenu)
        }

        Text(
            text = "Swipe left for video · swipe edges for menus",
            color = colors.muted.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .clickable(onClick = onOpenBottomMenu),
        )
    }
}

@Composable
private fun TransportIcon(
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MhjTheme.colors.primary,
    content: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
) {
    IconButton(onClick = onClick) { content(tint) }
}

@Composable
private fun NavChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MhjTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) colors.primary else colors.muted,
        )
        Text(
            text = label,
            color = if (selected) colors.primary else colors.muted,
            fontSize = 11.sp,
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
