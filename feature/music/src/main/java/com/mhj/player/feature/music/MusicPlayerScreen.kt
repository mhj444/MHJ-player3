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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
        if (dark) {
            // Board-cropped header strip (logo + arcs + orb wells) with tap targets
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.chrome_mid_header_strip),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth,
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clickable(onClick = onOpenTopMenu),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(56.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderChrome(onClick = onOpenTopMenu, dark = dark, left = true)
                BrandLogoHeader(modifier = Modifier.weight(1f))
                HeaderChrome(onClick = { }, dark = dark, left = false)
            }
        }

        Spacer(Modifier.height(if (dark) 4.dp else 10.dp))

        // Album + board gold-glass frame bitmap over art
        Box(
            modifier = Modifier
                .fillMaxWidth(if (dark) 0.90f else 0.86f)
                .aspectRatio(1f)
                .then(
                    if (!dark) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    listOf(Color(0x33C4A46A), Color.Transparent),
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(48.dp.toPx()),
                            )
                        }
                    } else {
                        Modifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0x55D4AF37), Color.Transparent),
                                ),
                                radius = size.minDimension * 0.58f,
                            )
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.demo_album_art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(if (dark) 0.84f else 0.84f)
                    .clip(RoundedCornerShape(if (dark) 26.dp else 30.dp)),
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
        dark && left -> R.drawable.chrome_mid_orb_eq
        dark && !left -> R.drawable.chrome_mid_orb_cast
        !dark && left -> R.drawable.chrome_porc_tile_eq
        else -> R.drawable.chrome_porc_tile_cast
    }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .size(if (dark) 44.dp else 48.dp)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

/** Midnight: bitmap glass play disc + glowing embossed transport glyphs. */
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
        MidGlyph(R.drawable.chrome_mid_shuffle, onShuffle, 52.dp)
        MidGlyph(R.drawable.chrome_mid_prev, onPrevious, 58.dp)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable(onClick = onPlayPause)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xCCD4AF37), Color(0x55D4AF37), Color.Transparent),
                        ),
                        radius = size.minDimension * 0.78f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chrome_mid_play),
                contentDescription = "Play pause",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (!state.isPlaying) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MhjColors.GoldHi,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        MidGlyph(R.drawable.chrome_mid_next, onNext, 58.dp)
        MidGlyph(R.drawable.chrome_mid_repeat, onRepeat, 52.dp)
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
        PorcelainOrb(R.drawable.chrome_porc_shuffle, onShuffle, 50.dp)
        PorcelainOrb(R.drawable.chrome_porc_prev, onPrevious, 54.dp)
        Box(
            modifier = Modifier
                .size(78.dp)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.chrome_porc_play),
                contentDescription = "Play pause",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            // Bitmap already has pause bars; overlay play arrow when paused
            if (!state.isPlaying) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MhjColors.GoldLo,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        PorcelainOrb(R.drawable.chrome_porc_next, onNext, 54.dp)
        PorcelainOrb(R.drawable.chrome_porc_repeat, onRepeat, 50.dp)
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
            Image(
                painter = painterResource(R.drawable.chrome_mid_nav_home),
                contentDescription = "Home",
                modifier = Modifier
                    .height(72.dp)
                    .clickable(onClick = onHome),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(R.drawable.chrome_mid_nav_library),
                contentDescription = "Library",
                modifier = Modifier
                    .height(72.dp)
                    .clickable(onClick = onLibrary),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        // Frosted glass bar + deeply recessed tiles
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
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RecessedNavTile(Icons.Filled.Home, "Home", true, onHome)
                RecessedNavTile(Icons.Outlined.Folder, "Library", false, onLibrary)
            }
        }
    }
}

@Composable
private fun IlluminatedNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .drawBehind {
                if (selected) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0x55D4AF37), Color.Transparent),
                        ),
                        radius = size.maxDimension * 0.55f,
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Icon(
            icon,
            label,
            tint = if (selected) MhjColors.GoldHi else MhjColors.Gold.copy(alpha = 0.72f),
            modifier = Modifier.size(26.dp),
        )
        Text(
            label,
            color = if (selected) MhjColors.GoldHi else MhjColors.Gold.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun RecessedNavTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(108.dp)
            .softEmboss(
                16.dp,
                // inverted for deep recess: dark TL, light BR
                light = c.darkShadow.copy(alpha = 0.35f),
                dark = c.lightShadow.copy(alpha = 0.65f),
                elevation = 8.dp,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) Color(0xD8E6D8C4) else Color(0x66EFE6D8),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Icon(icon, label, tint = if (selected) c.primary else c.muted, modifier = Modifier.size(22.dp))
        Text(label, color = if (selected) c.primary else c.muted, fontSize = 11.sp, fontFamily = FontFamily.Serif)
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
