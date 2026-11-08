package com.mhj.player.feature.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun VideoPlayerScreen(
    state: PlayerUiState,
    player: Player?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onOpenBottomMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MhjTheme.colors
    val context = LocalContext.current
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    DisposableEffect(player) {
        playerView.player = player
        onDispose { playerView.player = null }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())

        if (player == null || state.current == null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Video space", color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Swipe right to return to music.",
                    color = colors.muted,
                    fontSize = 14.sp,
                )
            }
        }

        // Landscape video chrome — frosted glass bar (board)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))),
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .softEmboss(26.dp, Color(0x55FFFFFF), Color(0x99000000), 14.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC2A241C), Color(0xE614110E)),
                        ),
                    )
                    .border(
                        width = 1.1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MhjColors.GoldHi.copy(alpha = 0.4f),
                                Color(0x33FFFFFF),
                                MhjColors.GoldLo.copy(alpha = 0.3f),
                            ),
                        ),
                        shape = RoundedCornerShape(26.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                GlassOrb(42.dp, onPlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play pause",
                        tint = MhjColors.GoldHi,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                GlassOrb(36.dp, { onSeekBy(-10_000) }) {
                    Icon(Icons.Outlined.Replay10, null, tint = MhjColors.Gold, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                GlassOrb(36.dp, { onSeekBy(10_000) }) {
                    Icon(Icons.Outlined.Forward10, null, tint = MhjColors.Gold, modifier = Modifier.size(18.dp))
                }

                val duration = state.durationMs.coerceAtLeast(0L)
                val position = state.positionMs.coerceAtLeast(0L)
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { if (duration > 0) onSeek((it * duration).toLong()) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MhjColors.GoldHi,
                        activeTrackColor = MhjColors.GoldTrack,
                        inactiveTrackColor = Color(0x44D4AF37),
                    ),
                )
                Text(
                    "${formatTime(position)} / ${formatTime(duration)}",
                    color = MhjColors.GoldHi.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(6.dp))
                GlassOrb(34.dp, onOpenBottomMenu) {
                    Icon(Icons.Outlined.ClosedCaption, null, tint = MhjColors.Gold, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                GlassOrb(34.dp, onOpenBottomMenu) {
                    Icon(Icons.Outlined.Headphones, null, tint = MhjColors.Gold, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "1.0x",
                    color = MhjColors.Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenBottomMenu)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Fullscreen, null, tint = MhjColors.Gold, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun GlassOrb(size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3A342C), Color(0xFF1A1612))))
            .border(1.3.dp, MhjColors.GoldMetal, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
