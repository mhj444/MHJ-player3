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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
                iconRes = if (dark) R.drawable.icon_mid_library else R.drawable.icon_porc_library,
            )
            BrandLogoHeader(modifier = Modifier.weight(1f))
            HeaderOrb(
                onClick = { },
                dark = dark,
                iconRes = if (dark) R.drawable.icon_mid_cast else R.drawable.icon_porc_cast,
            )
        }

        Spacer(Modifier.height(if (dark) 8.dp else 14.dp))

        // Clean single gold frame around cover (no torn bitmap crops / no pale wash)
        val artCorner = if (dark) 26.dp else 34.dp
        Box(
            modifier = Modifier
                .fillMaxWidth(if (dark) 0.86f else 0.82f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.demo_album_art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(artCorner))
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(
                            listOf(MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo),
                        ),
                        shape = RoundedCornerShape(artCorner),
                    ),
                contentScale = ContentScale.Crop,
            )
            // Outer hairline twin for thin dual-line look without glitch artifacts
            Canvas(Modifier.fillMaxSize()) {
                val pad = 4.dp.toPx()
                val stroke = 1.1.dp.toPx()
                val radius = artCorner.toPx() + 4.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            MhjColors.GoldHi.copy(alpha = 0.75f),
                            MhjColors.Gold.copy(alpha = 0.55f),
                            MhjColors.GoldLo.copy(alpha = 0.7f),
                        ),
                    ),
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2, size.height - pad * 2),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke),
                )
            }
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
        )
    }
}

/** Header orb: thin gold rim only — no emboss/softHalo wash behind logo buttons. */
@Composable
private fun HeaderOrb(onClick: () -> Unit, dark: Boolean, iconRes: Int) {
    Box(
        modifier = Modifier
            .size(if (dark) 42.dp else 46.dp)
            .clip(CircleShape)
            .background(
                if (dark) Color(0xFF1A1612) else Color(0xFFF3EADF),
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(MhjColors.GoldHi.copy(alpha = 0.9f), MhjColors.GoldLo.copy(alpha = 0.75f)),
                ),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(if (dark) 20.dp else 22.dp),
            contentScale = ContentScale.Fit,
        )
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

/** Bottom nav: Home only (Library lives on top-left). */
@Composable
private fun BottomNav(dark: Boolean, onHome: () -> Unit) {
    val c = MhjTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (dark) 10.dp else 14.dp, top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(onClick = onHome)
                .padding(horizontal = 28.dp, vertical = 8.dp),
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.icon_mid_home_active else R.drawable.icon_porc_home_active,
                ),
                contentDescription = "Home",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Home",
                color = if (dark) MhjColors.GoldHi else c.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
            )
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
