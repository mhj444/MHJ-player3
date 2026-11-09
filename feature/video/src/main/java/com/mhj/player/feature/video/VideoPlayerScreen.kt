package com.mhj.player.feature.video

import android.view.ContextThemeWrapper
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun VideoPlayerScreen(
    state: PlayerUiState,
    player: Player?,
    active: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onOpenBottomMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MhjTheme.colors
    val context = LocalContext.current
    val playerView = remember {
        PlayerView(ContextThemeWrapper(context, com.mhj.player.feature.video.R.style.MhjPlayerView)).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    // Detach immediately when leaving video — prevents Surface/Texture bleed into music
    DisposableEffect(player, active) {
        playerView.player = if (active) player else null
        onDispose {
            playerView.player = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (active) {
            AndroidView(
                factory = { playerView },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!active || player == null || state.current == null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Video space", color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Swipe left to return to music.",
                    color = colors.muted,
                    fontSize = 14.sp,
                )
            }
        }

        if (active) {
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TransportChip(onClick = onPlayPause) {
                        Image(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.icon_mid_pause else R.drawable.icon_mid_play,
                            ),
                            contentDescription = "Play pause",
                            modifier = Modifier.size(22.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    TransportChip(onClick = { onSeekBy(-10_000) }) {
                        Image(
                            painter = painterResource(R.drawable.icon_mid_prev),
                            contentDescription = "Seek back",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    val duration = state.durationMs.coerceAtLeast(0L)
                    val position = state.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
                    Column(Modifier.weight(1f)) {
                        Slider(
                            value = if (duration > 0) position.toFloat() / duration else 0f,
                            onValueChange = { frac -> if (duration > 0) onSeek((frac * duration).toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = MhjColors.GoldHi,
                                activeTrackColor = MhjColors.Gold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(position), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(formatTime(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    TransportChip(onClick = onOpenBottomMenu) {
                        Image(
                            painter = painterResource(R.drawable.icon_mid_equalizer),
                            contentDescription = "Audio",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportChip(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .softEmboss(21.dp, Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.55f), 8.dp)
            .clip(CircleShape)
            .background(Color(0xCC1A1612))
            .border(1.dp, Brush.linearGradient(listOf(MhjColors.GoldHi, MhjColors.GoldLo)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = (ms / 1000L).toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
