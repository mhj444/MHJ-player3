package com.mhj.player.feature.music

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.model.RepeatMode
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.components.BrandLogoHeader
import com.mhj.player.core.ui.components.EmbossCircle
import com.mhj.player.core.ui.components.EqualizerVisualizer
import com.mhj.player.core.ui.components.FrostPanel
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.components.softGoldHairline
import com.mhj.player.core.ui.components.softInset
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
    val c = MhjTheme.colors
    val title = state.current?.title ?: "The Last Kingdom"
    val artist = state.current?.artist ?: "Ramin Djawadi"
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderOrb(onClick = onOpenTopMenu) {
                Icon(Icons.Outlined.GraphicEq, null, tint = c.primary, modifier = Modifier.size(17.dp))
            }
            BrandLogoHeader(modifier = Modifier.weight(1f))
            HeaderOrb(onClick = { }) {
                Icon(Icons.Outlined.Cast, null, tint = c.primary.copy(alpha = 0.75f), modifier = Modifier.size(17.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Board album frame: soft emboss well + gold hairline + art
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .drawBehind {
                    // soft gold bloom behind frame (board glow) — keep subtle
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(c.glow.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.58f,
                        ),
                        radius = size.minDimension * 0.58f,
                    )
                }
                .softEmboss(30.dp, c.lightShadow, c.darkShadow, 10.dp)
                .softInset(30.dp, c.lightShadow, c.darkShadow)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(c.elevated, c.recess)))
                .softGoldHairline(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.demo_album_art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.4.dp, MhjColors.GoldMetal, RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            color = if (c.isDark) Color.White else c.onBackground,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = artist,
            color = if (c.isDark) MhjColors.GoldHi.copy(alpha = 0.85f) else c.muted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(8.dp))
        EqualizerVisualizer(
            active = state.isPlaying,
            modifier = Modifier.fillMaxWidth(0.9f),
        )

        Spacer(Modifier.height(2.dp))
        Slider(
            value = if (duration > 0) position.toFloat() / duration else 0f,
            onValueChange = { if (duration > 0) onSeek((it * duration).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MhjColors.GoldHi,
                activeTrackColor = MhjColors.GoldTrack,
                inactiveTrackColor = c.controlFill,
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(position), color = c.muted, fontSize = 11.sp)
            Text(formatTime(duration), color = c.muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(10.dp))

        // Board transport: five circular glass/embossed controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportOrb(Icons.Filled.Shuffle, state.shuffle, onShuffle, 46.dp)
            TransportOrb(Icons.Filled.SkipPrevious, true, onPrevious, 50.dp, iconSize = 22.dp)
            EmbossCircle(
                onClick = onPlayPause,
                diameter = 74.dp,
                filledGold = !c.isDark,
                ringGold = true,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play pause",
                    tint = if (c.isDark) MhjColors.GoldHi else Color(0xFF2A2118),
                    modifier = Modifier.size(34.dp),
                )
            }
            TransportOrb(Icons.Filled.SkipNext, true, onNext, 50.dp, iconSize = 22.dp)
            TransportOrb(
                if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                state.repeatMode != RepeatMode.OFF,
                onRepeat,
                46.dp,
            )
        }

        Spacer(Modifier.weight(1f))

        FrostPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            radius = 28.dp,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavItem(Icons.Filled.Home, "Home", true, onOpenBottomMenu)
                NavItem(Icons.Outlined.Folder, "Library", false, onOpenTopMenu)
            }
        }
    }
}

@Composable
private fun HeaderOrb(onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = MhjTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .softEmboss(21.dp, c.lightShadow, c.darkShadow, 9.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(c.lightShadow.copy(alpha = 0.7f), c.darkShadow.copy(alpha = 0.2f)),
                ),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TransportOrb(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    diameter: Dp,
    iconSize: Dp = 18.dp,
) {
    EmbossCircle(
        onClick = onClick,
        diameter = diameter,
        ringGold = active,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) MhjTheme.colors.primary else MhjTheme.colors.muted,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier
                        .softEmboss(14.dp, c.lightShadow, c.darkShadow, 6.dp)
                        .background(c.controlFill, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 4.dp),
    ) {
        Icon(icon, label, tint = if (selected) c.primary else c.muted)
        Text(
            label,
            color = if (selected) c.primary else c.muted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
