package com.mhj.player.feature.video

import com.mhj.player.core.model.L

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.mhj.player.core.ui.components.Fa
import com.mhj.player.core.ui.components.RaisedPill
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
    inPip: Boolean = false,
    onEnterPip: () -> Unit = {},
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
    var seekDrag by remember { mutableStateOf<Float?>(null) }
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
        showHud(GestureHud.Volume, L.pick("صدا ", "Volume ") + Fa.num((volumeFraction * 100).roundToInt()) + L.pick("٪", "%"))
    }

    fun applyBrightnessDelta(deltaY: Float, height: Float) {
        val window = activity?.window ?: return
        val delta = (-deltaY / height).coerceIn(-1f, 1f)
        brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1f)
        val lp = window.attributes
        lp.screenBrightness = brightnessFraction
        window.attributes = lp
        showHud(GestureHud.Brightness, L.pick("روشنایی ", "Brightness ") + Fa.num((brightnessFraction * 100).roundToInt()) + L.pick("٪", "%"))
    }

    fun applySeekDelta(deltaX: Float, width: Float) {
        // Finger left→right (positive dx) seeks forward
        val seconds = ((deltaX / width) * 90f) // ~90s full-width
        seekAccumMs += (seconds * 1000f).toLong()
        showHud(GestureHud.Seek, (if (seekAccumMs >= 0) L.pick("جلو ", "Forward ") else L.pick("عقب ", "Back ")) + Fa.time(kotlin.math.abs(seekAccumMs)))
    }

    LaunchedEffect(hud) {
        if (hud == GestureHud.None) return@LaunchedEffect
        delay(900)
        hud = GestureHud.None
    }

    // Video controls always run left-to-right (back on the left, forward on the right).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
        if (hasVideo && !inPip) {
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
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC14110E))
                    .border(1.dp, MhjColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        AnimatedVisibility(
            visible = !inPip && (chromeVisible || !hasVideo),
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
                    state.current?.takeIf { hasVideo }?.title ?: L.t("ویدیو"),
                    color = MhjColors.GoldHi,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (hasVideo) {
                    VideoChip(onClick = { onEnterPip() }) {
                        Icon(Icons.Outlined.PictureInPictureAlt, L.t("پنجرهٔ کوچک"), tint = MhjColors.GoldHi, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                VideoChip(
                    onClick = {
                        bumpHide.value.invoke()
                        onPickVideo()
                    },
                ) {
                    Icon(Icons.Outlined.VideoLibrary, L.t("باز کردن ویدیو"), tint = MhjColors.GoldHi, modifier = Modifier.size(21.dp))
                }
            }
        }

        if (!hasVideo) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(L.t("فضای ویدیو"), color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    L.t("یک ویدیو انتخاب کنید یا از کتابخانه بزنید"),
                    color = colors.muted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                RaisedPill(L.t("باز کردن ویدیو")) {
                    bumpHide.value.invoke()
                    onPickVideo()
                }
            }
        }

        AnimatedVisibility(
            visible = !inPip && hasVideo && chromeVisible,
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
                        Icon(if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (state.isPlaying) L.t("توقف") else L.t("پخش"), tint = MhjColors.GoldHi, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    VideoChip(
                        onClick = {
                            bumpHide.value.invoke()
                            onSeekBy(-10_000)
                        },
                    ) {
                        Icon(Icons.Outlined.Replay10, L.t("۱۰ ثانیه عقب"), tint = MhjColors.GoldHi, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    val duration = state.durationMs.coerceAtLeast(0L)
                    val position = state.positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
                    Column(Modifier.weight(1f)) {
                        Slider(
                            value = seekDrag ?: if (duration > 0) position.toFloat() / duration else 0f,
                            onValueChange = { frac ->
                                bumpHide.value.invoke()
                                seekDrag = frac
                            },
                            onValueChangeFinished = {
                                seekDrag?.let { if (duration > 0) onSeek((it * duration).toLong()) }
                                seekDrag = null
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
                            Text(Fa.time(seekDrag?.let { (it * duration).toLong() } ?: position), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(Fa.time(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    VideoChip(
                        onClick = {
                            bumpHide.value.invoke()
                            onSeekBy(10_000)
                        },
                    ) {
                        Icon(Icons.Outlined.Forward10, L.t("۱۰ ثانیه جلو"), tint = MhjColors.GoldHi, modifier = Modifier.size(22.dp))
                    }
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

