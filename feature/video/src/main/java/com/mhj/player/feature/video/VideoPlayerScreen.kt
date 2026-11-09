package com.mhj.player.feature.video

import android.app.Activity
import android.content.res.Configuration
import android.media.AudioManager
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class GestureHud { None, Seek, Volume, Brightness }

@Composable
fun VideoPlayerScreen(
    state: PlayerUiState,
    player: Player?,
    active: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MhjTheme.colors
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val audioManager = remember {
        context.getSystemService(AudioManager::class.java)
    }
    val activity = context as? Activity
    val playerView = remember {
        PlayerView(ContextThemeWrapper(context, com.mhj.player.feature.video.R.style.MhjPlayerView)).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    val hasVideo = active && state.current?.kind == MediaKind.VIDEO
    var chromeVisible by remember { mutableStateOf(true) }
    var hideGeneration by remember { mutableLongStateOf(0L) }
    var hud by remember { mutableStateOf(GestureHud.None) }
    var hudLabel by remember { mutableStateOf("") }
    var seekAccumMs by remember { mutableLongStateOf(0L) }
    var volumeFraction by remember { mutableFloatStateOf(0f) }
    var brightnessFraction by remember { mutableFloatStateOf(0.5f) }

    val playing = state.isPlaying
    val bumpHide = rememberUpdatedState {
        hideGeneration += 1
        chromeVisible = true
    }

    DisposableEffect(player, hasVideo) {
        playerView.player = if (hasVideo) player else null
        onDispose { playerView.player = null }
    }

    LaunchedEffect(hasVideo, playing, hideGeneration) {
        if (!hasVideo) {
            chromeVisible = true
            return@LaunchedEffect
        }
        if (!playing) {
            chromeVisible = true
            return@LaunchedEffect
        }
        delay(3_000)
        chromeVisible = false
    }

    LaunchedEffect(Unit) {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        volumeFraction = cur.toFloat() / maxVol
        val lp = activity?.window?.attributes
        brightnessFraction = when {
            lp == null -> 0.5f
            lp.screenBrightness < 0f -> 0.5f
            else -> lp.screenBrightness.coerceIn(0.01f, 1f)
        }
    }

    fun showHud(kind: GestureHud, label: String) {
        hud = kind
        hudLabel = label
    }

    fun applyVolumeDelta(deltaY: Float, height: Float) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val delta = (-deltaY / height).coerceIn(-1f, 1f)
        volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (volumeFraction * max).roundToInt().coerceIn(0, max),
            0,
        )
        showHud(GestureHud.Volume, "صدا ${(volumeFraction * 100).roundToInt()}٪")
    }

    fun applyBrightnessDelta(deltaY: Float, height: Float) {
        val window = activity?.window ?: return
        val delta = (-deltaY / height).coerceIn(-1f, 1f)
        brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1f)
        val lp = window.attributes
        lp.screenBrightness = brightnessFraction
        window.attributes = lp
        showHud(GestureHud.Brightness, "روشنایی ${(brightnessFraction * 100).roundToInt()}٪")
    }

    fun applySeekDelta(deltaX: Float, width: Float) {
        // Finger left→right (positive dx) seeks forward
        val seconds = ((deltaX / width) * 90f) // ~90s full-width
        seekAccumMs += (seconds * 1000f).toLong()
        val labelSec = seekAccumMs / 1000.0
        val sign = if (labelSec >= 0) "+" else ""
        showHud(GestureHud.Seek, "جابه‌جایی $sign${"%.1f".format(labelSec)}s")
    }

    LaunchedEffect(hud) {
        if (hud == GestureHud.None) return@LaunchedEffect
        delay(900)
        hud = GestureHud.None
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasVideo) {
            AndroidView(
                factory = { playerView },
                update = { view ->
                    view.player = if (hasVideo) player else null
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gesture layer — landscape owns seek/volume/brightness; tap toggles chrome
        if (hasVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(landscape, playing) {
                        detectTapGestures(
                            onTap = {
                                chromeVisible = !chromeVisible
                                if (chromeVisible) bumpHide.value.invoke()
                            },
                        )
                    }
                    .then(
                        if (landscape) {
                            Modifier.pointerInput(Unit) {
                                var total = Offset.Zero
                                var axis: GestureHud? = null
                                detectDragGestures(
                                    onDragStart = {
                                        total = Offset.Zero
                                        axis = null
                                        seekAccumMs = 0L
                                    },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        total += drag
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        if (axis == null && (abs(total.x) > 24f || abs(total.y) > 24f)) {
                                            axis = if (abs(total.x) > abs(total.y)) {
                                                GestureHud.Seek
                                            } else if (change.position.x > w * 0.5f) {
                                                GestureHud.Volume
                                            } else {
                                                GestureHud.Brightness
                                            }
                                        }
                                        when (axis) {
                                            GestureHud.Seek -> applySeekDelta(drag.x, w)
                                            GestureHud.Volume -> applyVolumeDelta(drag.y, h)
                                            GestureHud.Brightness -> applyBrightnessDelta(drag.y, h)
                                            else -> Unit
                                        }
                                        bumpHide.value.invoke()
                                    },
                                    onDragEnd = {
                                        if (axis == GestureHud.Seek && seekAccumMs != 0L) {
                                            onSeekBy(seekAccumMs)
                                        }
                                        seekAccumMs = 0L
                                        axis = null
                                    },
                                    onDragCancel = {
                                        seekAccumMs = 0L
                                        axis = null
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        AnimatedVisibility(
            visible = hud != GestureHud.None,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = hudLabel,
                color = MhjColors.GoldHi,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC14110E))
                    .border(1.dp, MhjColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible || !hasVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Video",
                    color = MhjColors.GoldHi,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
                VideoChip(
                    onClick = {
                        bumpHide.value.invoke()
                        onPickVideo()
                    },
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_mid_library),
                        contentDescription = "Open video",
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        if (!hasVideo) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Video space", color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick a video file to play",
                    color = colors.muted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Open video",
                    color = MhjColors.GoldHi,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MhjColors.Gold, RoundedCornerShape(16.dp))
                        .clickable {
                            bumpHide.value.invoke()
                            onPickVideo()
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = hasVideo && chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .pointerInput(Unit) {
                        // Touching chrome resets auto-hide without toggling
                        detectTapGestures { bumpHide.value.invoke() }
                    },
            ) {
                Text(
                    state.current?.title.orEmpty(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    VideoChip(
                        onClick = {
                            bumpHide.value.invoke()
                            onPlayPause()
                        },
                    ) {
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
                    VideoChip(
                        onClick = {
                            bumpHide.value.invoke()
                            onSeekBy(-10_000)
                        },
                    ) {
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
                            onValueChange = { frac ->
                                bumpHide.value.invoke()
                                if (duration > 0) onSeek((frac * duration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MhjColors.GoldHi,
                                activeTrackColor = MhjColors.Gold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(formatTime(position), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(formatTime(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    VideoChip(
                        onClick = {
                            bumpHide.value.invoke()
                            onPickVideo()
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.icon_mid_library),
                            contentDescription = "Open video",
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
private fun VideoChip(onClick: () -> Unit, content: @Composable () -> Unit) {
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
    return "%d:%02d".format(total / 60, total % 60)
}
