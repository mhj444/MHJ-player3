package com.mhj.player.feature.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.components.BrandLogoHeader
import com.mhj.player.core.ui.components.EqualizerVisualizer
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlin.math.roundToLong

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
    val dark = c.isDark
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
        Spacer(Modifier.height(2.dp))
        // Official pack header icons (EQ / cast) flanking brand logo — no reconstructed strip orbs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderChrome(onClick = onOpenTopMenu, dark = dark, left = true)
            BrandLogoHeader(modifier = Modifier.weight(1f))
            HeaderChrome(onClick = { }, dark = dark, left = false)
        }

        Spacer(Modifier.height(if (dark) 4.dp else 10.dp))

        // Album + thin multi-line gold-glass frame (no chunky outer halo)
        Box(
            modifier = Modifier
                .fillMaxWidth(if (dark) 0.90f else 0.86f)
                .aspectRatio(1f)
                .then(
                    if (!dark) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    listOf(Color(0x22C4A46A), Color.Transparent),
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(48.dp.toPx()),
                            )
                        }
                    } else {
                        Modifier // Midnight: frame bitmap carries thin gold lines; no radial glow tile
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.demo_album_art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(if (dark) 0.88f else 0.84f)
                    .clip(RoundedCornerShape(if (dark) 22.dp else 30.dp)),
                contentScale = ContentScale.Crop,
            )
            Image(
                painter = painterResource(
                    if (dark) R.drawable.chrome_mid_album_frame else R.drawable.chrome_porc_album_frame,
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Spacer(Modifier.height(if (dark) 10.dp else 14.dp))

        Text(
            text = title,
            color = if (dark) Color(0xFFF5F0E6) else c.onBackground,
            fontSize = if (dark) 20.sp else 22.sp,
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
            color = if (dark) MhjColors.Gold.copy(alpha = 0.92f) else c.muted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(if (dark) 6.dp else 8.dp))
        EqualizerVisualizer(
            active = state.isPlaying,
            modifier = Modifier.fillMaxWidth(0.94f),
        )

        Spacer(Modifier.height(4.dp))
        GoldSeekBar(
            progress = if (duration > 0) position.toFloat() / duration else 0f,
            onSeekFraction = { frac -> if (duration > 0) onSeek((frac * duration).roundToLong()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(position), color = c.muted, fontSize = 11.sp)
            Text(formatTime(duration), color = c.muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(6.dp))

        if (dark) {
            MidnightTransport(
                state = state,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
            )
        } else {
            PorcelainTransport(
                state = state,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
            )
        }

        Spacer(Modifier.weight(1f))

        BottomNav(
            dark = dark,
            onHome = onOpenBottomMenu,
            onLibrary = onOpenTopMenu,
        )
    }
}

@Composable
private fun HeaderChrome(onClick: () -> Unit, dark: Boolean, left: Boolean) {
    val res = when {
        dark && left -> R.drawable.icon_mid_equalizer
        dark && !left -> R.drawable.icon_mid_cast
        !dark && left -> R.drawable.icon_porc_equalizer
        else -> R.drawable.icon_porc_cast
    }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(if (dark) 36.dp else 40.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

/** Midnight transport: official MHJ icon pack bitmaps (PNG-512). */
@Composable
private fun MidnightTransport(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MidGlyph(R.drawable.icon_mid_shuffle, onShuffle, 44.dp)
        MidGlyph(R.drawable.icon_mid_prev, onPrevious, 48.dp)
        Image(
            painter = painterResource(
                if (state.isPlaying) R.drawable.icon_mid_pause else R.drawable.icon_mid_play,
            ),
            contentDescription = "Play pause",
            modifier = Modifier
                .size(72.dp)
                .clickable(onClick = onPlayPause),
            contentScale = ContentScale.Fit,
        )
        MidGlyph(R.drawable.icon_mid_next, onNext, 48.dp)
        MidGlyph(R.drawable.icon_mid_repeat, onRepeat, 44.dp)
    }
}

@Composable
private fun MidGlyph(res: Int, onClick: () -> Unit, size: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

/** Porcelain: cream embossed circle bitmaps; play is same family (not solid gold). */
@Composable
private fun PorcelainTransport(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PorcelainOrb(R.drawable.icon_porc_shuffle, onShuffle, 48.dp)
        PorcelainOrb(R.drawable.icon_porc_prev, onPrevious, 52.dp)
        Image(
            painter = painterResource(
                if (state.isPlaying) R.drawable.icon_porc_pause else R.drawable.icon_porc_play,
            ),
            contentDescription = "Play pause",
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onPlayPause),
            contentScale = ContentScale.Fit,
        )
        PorcelainOrb(R.drawable.icon_porc_next, onNext, 52.dp)
        PorcelainOrb(R.drawable.icon_porc_repeat, onRepeat, 48.dp)
    }
}

@Composable
private fun PorcelainOrb(res: Int, onClick: () -> Unit, size: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun BottomNav(dark: Boolean, onHome: () -> Unit, onLibrary: () -> Unit) {
    val c = MhjTheme.colors
    if (dark) {
        // Board-cropped / board-lit glow glyphs — bitmap chrome, not Material vectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onHome)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_mid_home_active),
                    contentDescription = "Home",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit,
                )
                Text("Home", color = MhjColors.GoldHi, fontSize = 11.sp, fontFamily = FontFamily.Serif)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onLibrary)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_mid_library),
                    contentDescription = "Library",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit,
                )
                Text("Library", color = MhjColors.Gold.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = FontFamily.Serif)
            }
        }
    } else {
        // Frosted porcelain nav — bitmap home/library marks from boards/feedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .softEmboss(28.dp, c.lightShadow, c.darkShadow, 10.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xE8F7F0E6),
                            Color(0xCCEFE4D4),
                        ),
                    ),
                ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.icon_porc_home_active),
                        contentDescription = "Home",
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onHome),
                        contentScale = ContentScale.Fit,
                    )
                    Text("Home", color = c.primary, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.icon_porc_library),
                        contentDescription = "Library",
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onLibrary),
                        contentScale = ContentScale.Fit,
                    )
                    Text("Library", color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

/** Board-style thin gold seek with glowing circular thumb (not Material bulk). */
@Composable
private fun GoldSeekBar(
    progress: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = progress.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onSeekFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeekFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2f
            val stroke = 2.2.dp.toPx()
            drawLine(
                color = MhjColors.Gold.copy(alpha = 0.28f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val x = size.width * p
            drawLine(
                brush = Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi)),
                start = Offset(0f, y),
                end = Offset(x, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = MhjColors.GoldGlow.copy(alpha = 0.55f),
                radius = 12.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.Gold)),
                radius = 6.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
