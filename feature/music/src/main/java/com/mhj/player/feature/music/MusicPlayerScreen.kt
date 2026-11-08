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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mhj.player.core.ui.components.EmbossCircle
import com.mhj.player.core.ui.components.EqualizerVisualizer
import com.mhj.player.core.ui.components.softEmboss
import com.mhj.player.core.ui.components.softGoldHalo
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
            .padding(horizontal = if (dark) 20.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))

        // Header: gold-rim orb wells + official icons flanking brand logo (board)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderOrb(
                onClick = onOpenTopMenu,
                dark = dark,
                iconRes = if (dark) R.drawable.icon_mid_equalizer else R.drawable.icon_porc_equalizer,
            )
            BrandLogoHeader(modifier = Modifier.weight(1f))
            HeaderOrb(
                onClick = { },
                dark = dark,
                iconRes = if (dark) R.drawable.icon_mid_cast else R.drawable.icon_porc_cast,
            )
        }

        Spacer(Modifier.height(if (dark) 8.dp else 14.dp))

        // Album: board-cropped multi-line gold frame + soft gold halo
        Box(
            modifier = Modifier
                .fillMaxWidth(if (dark) 0.88f else 0.84f)
                .aspectRatio(1f)
                .softGoldHalo(
                    radius = if (dark) 28.dp else 36.dp,
                    bloom = if (dark) 22.dp else 18.dp,
                    intensity = if (dark) 0.48f else 0.38f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.demo_album_art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(if (dark) 0.86f else 0.82f)
                    .clip(RoundedCornerShape(if (dark) 26.dp else 34.dp)),
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

        Spacer(Modifier.height(if (dark) 14.dp else 16.dp))

        Text(
            text = title,
            color = if (dark) Color(0xFFF7F2E8) else c.onBackground,
            fontSize = if (dark) 22.sp else 22.sp,
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
            color = if (dark) MhjColors.Gold.copy(alpha = 0.95f) else c.muted,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(if (dark) 10.dp else 12.dp))
        EqualizerVisualizer(
            active = state.isPlaying,
            modifier = Modifier.fillMaxWidth(0.92f),
        )

        Spacer(Modifier.height(6.dp))
        GoldSeekBar(
            progress = if (duration > 0) position.toFloat() / duration else 0f,
            onSeekFraction = { frac -> if (duration > 0) onSeek((frac * duration).roundToLong()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(position), color = c.muted, fontSize = 11.sp)
            Text(formatTime(duration), color = c.muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(if (dark) 10.dp else 12.dp))

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

/** Board header: gold-rim glass orb well + official pack glyph. */
@Composable
private fun HeaderOrb(onClick: () -> Unit, dark: Boolean, iconRes: Int) {
    if (dark) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chrome_mid_orb_well),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        EmbossCircle(onClick = onClick, diameter = 48.dp, ringGold = true) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Midnight transport: official glyphs; center = feedback gold-glass ring + pack play/pause. */
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
        MidGlyph(R.drawable.icon_mid_shuffle, onShuffle, 42.dp)
        MidGlyph(R.drawable.icon_mid_prev, onPrevious, 46.dp)
        // Board gold-glass center: soft halo + dual gold rim orb + official play/pause glyph
        Box(
            modifier = Modifier
                .size(86.dp)
                .softGoldHalo(43.dp, bloom = 18.dp, intensity = 0.5f),
            contentAlignment = Alignment.Center,
        ) {
            EmbossCircle(onClick = onPlayPause, diameter = 78.dp, ringGold = true) {
                Image(
                    painter = painterResource(
                        if (state.isPlaying) R.drawable.icon_mid_pause else R.drawable.icon_mid_play,
                    ),
                    contentDescription = "Play pause",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        MidGlyph(R.drawable.icon_mid_next, onNext, 46.dp)
        MidGlyph(R.drawable.icon_mid_repeat, onRepeat, 42.dp)
    }
}

@Composable
private fun MidGlyph(res: Int, onClick: () -> Unit, size: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .softGoldHalo(size / 2, bloom = 8.dp, intensity = 0.28f)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

/** Porcelain: cream embossed orbs + official pack glyphs; center gold-glass ring. */
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
        EmbossCircle(onClick = onPlayPause, diameter = 72.dp, ringGold = true) {
            Image(
                painter = painterResource(
                    if (state.isPlaying) R.drawable.icon_porc_pause else R.drawable.icon_porc_play,
                ),
                contentDescription = "Play pause",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
            )
        }
        PorcelainOrb(R.drawable.icon_porc_next, onNext, 52.dp)
        PorcelainOrb(R.drawable.icon_porc_repeat, onRepeat, 48.dp)
    }
}

@Composable
private fun PorcelainOrb(res: Int, onClick: () -> Unit, size: Dp) {
    EmbossCircle(onClick = onClick, diameter = size, ringGold = true) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(size * 0.46f),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun BottomNav(dark: Boolean, onHome: () -> Unit, onLibrary: () -> Unit) {
    val c = MhjTheme.colors
    if (dark) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onHome)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_mid_home_active),
                    contentDescription = "Home",
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(4.dp))
                Text("Home", color = MhjColors.GoldHi, fontSize = 11.sp, fontFamily = FontFamily.Serif)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onLibrary)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_mid_library),
                    contentDescription = "Library",
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(4.dp))
                Text("Library", color = MhjColors.Gold.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = FontFamily.Serif)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .softEmboss(28.dp, c.lightShadow, c.darkShadow, 10.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xE8F7F0E6), Color(0xCCEFE4D4)),
                    ),
                ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onHome),
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_porc_home_active),
                        contentDescription = "Home",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text("Home", color = c.primary, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onLibrary),
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_porc_library),
                        contentDescription = "Library",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text("Library", color = c.muted, fontSize = 11.sp, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun GoldSeekBar(
    progress: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = progress.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = modifier
            .height(26.dp)
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
            val stroke = 2.0.dp.toPx()
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
                color = MhjColors.GoldGlow.copy(alpha = 0.5f),
                radius = 11.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.Gold)),
                radius = 5.5.dp.toPx(),
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
