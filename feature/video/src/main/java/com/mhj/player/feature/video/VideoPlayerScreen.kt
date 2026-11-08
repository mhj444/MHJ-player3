package com.mhj.player.feature.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize(),
        )

        if (player == null || state.current == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Video space",
                    color = colors.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Swipe right to return to music.\nOpen a video from Library in Phase 2.",
                    color = colors.muted,
                    fontSize = 14.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.glass.copy(alpha = 0.72f))
                    .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play pause",
                        tint = Color.Black,
                    )
                }
                IconButton(onClick = { onSeekBy(-10_000) }) {
                    Icon(Icons.Outlined.Replay10, contentDescription = "Back 10", tint = colors.primary)
                }
                IconButton(onClick = { onSeekBy(10_000) }) {
                    Icon(Icons.Outlined.Forward10, contentDescription = "Forward 10", tint = colors.primary)
                }

                val duration = state.durationMs.coerceAtLeast(0L)
                val position = state.positionMs.coerceAtLeast(0L)
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { if (duration > 0) onSeek((it * duration).toLong()) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.controlFill,
                    ),
                )
                Text(
                    text = "${formatTime(position)} / ${formatTime(duration)}",
                    color = colors.primarySoft,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(onClick = onOpenBottomMenu) {
                    Icon(Icons.Outlined.ClosedCaption, contentDescription = "CC", tint = colors.primary)
                }
                IconButton(onClick = onOpenBottomMenu) {
                    Icon(Icons.Outlined.Headphones, contentDescription = "Audio", tint = colors.primary)
                }
                Text(
                    text = "1.0x",
                    color = colors.primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable(onClick = onOpenBottomMenu)
                        .padding(horizontal = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = colors.primary)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
