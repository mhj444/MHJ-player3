package com.mhj.player.feature.music

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.ui.components.BrandLogoHeader
import com.mhj.player.core.ui.components.Emboss
import com.mhj.player.core.ui.components.Fa
import com.mhj.player.core.ui.components.RaisedIconButton
import com.mhj.player.core.ui.components.artistLabel
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import com.mhj.player.core.ui.components.raised
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

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
    onCast: () -> Unit = {},
    onOpenBottomMenu: () -> Unit = {},
    showRemaining: Boolean = false,
    onToggleRemaining: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    speed: Float = 1f,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(state, Modifier.fillMaxHeight(0.86f).aspectRatio(1f))
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Header(onOpenTopMenu, onCast, compact = true)
                    Spacer(Modifier.height(10.dp))
                    Controls(state, onPlayPause, onSeek, onPrevious, onNext, onShuffle, onRepeat, showRemaining, onToggleRemaining, isFavorite, onToggleFavorite, onOpenBottomMenu, speed)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Header(onOpenTopMenu, onCast, compact = false)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth * 0.86f, maxHeight - 28.dp, 380.dp).coerceAtLeast(120.dp)
                    Artwork(state, Modifier.size(side))
                }
                Controls(state, onPlayPause, onSeek, onPrevious, onNext, onShuffle, onRepeat, showRemaining, onToggleRemaining, isFavorite, onToggleFavorite, onOpenBottomMenu, speed)
            }
        }
    }
}

/** The MHJ logo is the menu button: tap it (or swipe down from the top) to open the library. */
@Composable
private fun Header(onOpenTopMenu: () -> Unit, onCast: () -> Unit, compact: Boolean) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenTopMenu)
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (compact) {
            Text("MHJ Player", color = MhjColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        } else {
            BrandLogoHeader(Modifier.width(220.dp))
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.size(width = 28.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi, MhjColors.GoldLo))),
        )
    }
}

/** Cover picture from the file itself, or a drawn gold disc when there is none. */
@Composable
private fun Artwork(state: PlayerUiState, modifier: Modifier) {
    val ctx = LocalContext.current
    val uri = state.current?.uri
    val art by produceState<ImageBitmap?>(null, uri, state.current?.artworkUri) {
        value = null
        value = withContext(Dispatchers.IO) { loadArt(ctx, uri, state.current?.artworkUri) }
    }
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .raised(30.dp, 14.dp, Emboss.body, Emboss.light, Emboss.dark)
            .clip(shape)
            .background(Emboss.face())
            .border(1.5.dp, Emboss.rim(0.7f), shape)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        val inner = RoundedCornerShape(22.dp)
        val a = art
        if (a != null) {
            Image(a, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(inner))
        } else {
            DrawnCover(state.isPlaying, Modifier.fillMaxSize().clip(inner))
        }
    }
}

private fun loadArt(ctx: android.content.Context, uri: String?, artworkUri: String?): ImageBitmap? {
    fun scaled(b: Bitmap?): ImageBitmap? = b?.let {
        val max = 900
        val s = if (it.width > max || it.height > max) Bitmap.createScaledBitmap(it, max, max * it.height / it.width.coerceAtLeast(1), true) else it
        s.asImageBitmap()
    }
    if (!artworkUri.isNullOrBlank() && !artworkUri.startsWith("http")) {
        runCatching { ctx.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { BitmapFactory.decodeStream(it) } }.getOrNull()?.let { return scaled(it) }
    }
    if (uri.isNullOrBlank() || uri.startsWith("http")) return null
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, Uri.parse(uri))
        r.embeddedPicture?.let { bytes ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
            scaled(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts))
        }
    } catch (_: Throwable) { null } finally { runCatching { r.release() } }
}

@Composable
private fun DrawnCover(playing: Boolean, modifier: Modifier) {
    val dark = MhjTheme.colors.isDark
    val turn = rememberInfiniteTransition(label = "disc")
    val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "a")
    Box(
        modifier.background(
            if (dark) Brush.radialGradient(listOf(Color(0xFF2A2418), Color(0xFF121110), Color(0xFF070707)))
            else Brush.radialGradient(listOf(Color(0xFFFFF8EC), Color(0xFFF1E6D4), Color(0xFFE4D5BE)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize(0.82f)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2, size.height / 2)
            // A record: dark disc with fine gold grooves; a highlight that turns while playing.
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1C1A16), Color(0xFF0B0B0B)), c, r), r, c)
            for (i in 1..14) drawCircle(MhjColors.Gold.copy(alpha = 0.05f + (i % 3) * 0.025f), r * (0.36f + i * 0.044f), c, style = Stroke(1f))
            val a = if (playing) angle else 35f
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, MhjColors.GoldHi.copy(alpha = 0.22f), Color.Transparent), c),
                a, 70f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
            )
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, MhjColors.GoldHi.copy(alpha = 0.14f), Color.Transparent), c),
                a + 180f, 70f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2),
            )
            drawCircle(Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo), c, r * 0.32f), r * 0.30f, c)
            drawCircle(Color(0xFF0B0B0B), r * 0.035f, c)
        }
        Icon(Icons.Outlined.MusicNote, null, tint = Color(0xFF2A2210).copy(alpha = 0.75f), modifier = Modifier.size(34.dp).padding(bottom = 0.dp))
    }
}

@Composable
private fun ColumnScope.Controls(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    showRemaining: Boolean,
    onToggleRemaining: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenBottomMenu: () -> Unit,
    speed: Float,
) {
    val c = MhjTheme.colors
    val item = state.current
    // Title + heart.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                item?.title ?: "آهنگی پخش نمی‌شود",
                color = c.onBackground, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                item?.let { listOf(artistLabel(it.artist), it.album).filter { s -> s.isNotBlank() && s != "<unknown>" }.joinToString(" — ") }
                    ?: "روی لوگو بزنید و یک آهنگ انتخاب کنید",
                color = c.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        RaisedIconButton(
            if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            if (isFavorite) "برداشتن از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
            onToggleFavorite, size = 46.dp, iconSize = 22.dp, enabled = item != null,
            tint = if (isFavorite) Color(0xFFE0475B) else null,
        )
    }
    Spacer(Modifier.height(14.dp))
    Visualizer(state.isPlaying, Modifier.fillMaxWidth().height(60.dp))
    Spacer(Modifier.height(8.dp))
    SeekBar(state.positionMs, state.durationMs, onSeek, showRemaining, onToggleRemaining)
    Spacer(Modifier.height(16.dp))
    // Buttons: always left-to-right (previous on the left), like every player.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RaisedIconButton(Icons.Outlined.Shuffle, "پخش درهم", onShuffle, size = 46.dp, iconSize = 21.dp, active = state.shuffle)
            RaisedIconButton(Icons.Outlined.SkipPrevious, "قبلی", onPrevious, size = 58.dp, iconSize = 30.dp)
            PlayButton(state.isPlaying, state.isBuffering, onPlayPause)
            RaisedIconButton(Icons.Outlined.SkipNext, "بعدی", onNext, size = 58.dp, iconSize = 30.dp)
            RaisedIconButton(
                if (state.repeatMode == com.mhj.player.core.model.RepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                when (state.repeatMode) {
                    com.mhj.player.core.model.RepeatMode.OFF -> "تکرار خاموش"
                    com.mhj.player.core.model.RepeatMode.ONE -> "تکرار یک آهنگ"
                    com.mhj.player.core.model.RepeatMode.ALL -> "تکرار همه"
                },
                onRepeat, size = 46.dp, iconSize = 21.dp, active = state.repeatMode != com.mhj.player.core.model.RepeatMode.OFF,
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    // Opens the sound panel (same as swiping up from the bottom).
    Row(
        Modifier
            .raised(20.dp, 4.dp, Emboss.body, Emboss.light, Emboss.dark)
            .clip(RoundedCornerShape(20.dp))
            .background(Emboss.face())
            .border(1.dp, Emboss.rim(0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenBottomMenu)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .align(Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.GraphicEq, null, tint = MhjColors.Gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "پردازش صدا" + if (kotlin.math.abs(speed - 1f) > 0.01f) "  ·  " + Fa.speed(speed) else "",
            color = c.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.KeyboardArrowUp, null, tint = MhjColors.Gold, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(6.dp))
}

/** The big play button: black, with a gold sign and a soft gold glow around it. */
@Composable
private fun PlayButton(playing: Boolean, buffering: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val size: Dp = 82.dp
    Box(Modifier.size(size + 26.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(MhjColors.Gold.copy(alpha = if (playing) 0.45f else 0.3f), MhjColors.Gold.copy(alpha = 0.08f), Color.Transparent)),
                radius = this.size.minDimension / 2,
            )
        }
        Box(
            Modifier
                .size(size)
                .scale(if (pressed) 0.96f else 1f)
                .raised(size / 2, if (pressed) 2.dp else 10.dp, Color(0xFF0E0E10), Emboss.light, Emboss.dark)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF28282C), Color(0xFF0B0B0C), Color(0xFF050506))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)), CircleShape)
                .clickable(src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (playing) "توقف" else "پخش",
                tint = MhjColors.GoldHi,
                modifier = Modifier.size(42.dp),
            )
            if (buffering) {
                val t = rememberInfiniteTransition(label = "buf")
                val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "a")
                Canvas(Modifier.size(size - 6.dp)) {
                    drawArc(MhjColors.Gold.copy(alpha = 0.8f), a, 80f, false, style = Stroke(2.5f.dp.toPx()))
                }
            }
        }
    }
}

/** Glowing gold waves that move while music plays and lie almost flat when paused. */
@Composable
private fun Visualizer(active: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "vis")
    val phase by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val breath by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val level by androidx.compose.animation.core.animateFloatAsState(if (active) 1f else 0.12f, tween(600), label = "l")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2
        data class Wave(val amp: Float, val speed: Float, val color: Color, val width: Float)
        val waves = listOf(
            Wave(0.45f, 2.6f, MhjColors.Gold.copy(alpha = 0.35f), 1.2f),
            Wave(0.7f, 1.7f, MhjColors.Gold.copy(alpha = 0.6f), 1.6f),
            Wave(1f, 1f, MhjColors.GoldHi, 2.2f),
        )
        waves.forEachIndexed { k, wv ->
            val path = androidx.compose.ui.graphics.Path()
            var x = 0f
            while (x <= w) {
                val f = x / w
                val s1 = sin(f * PI).toFloat()
                val env = s1 * s1 * kotlin.math.sqrt(s1.coerceAtLeast(0f))
                val y = mid + sin(f * PI * 5 + phase * wv.speed + k * 1.3f).toFloat() * h * 0.42f * wv.amp * env * level * (if (k == 2) breath else 1f)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 3f
            }
            drawPath(path, wv.color.copy(alpha = wv.color.alpha * 0.3f), style = Stroke(wv.width.dp.toPx() * 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(path, wv.color, style = Stroke(wv.width.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

/**
 * Gold position bar. Drag or tap to jump. The time on the right shows the length of the track;
 * tap it to see the time left instead (and tap again to go back).
 */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, showRemaining: Boolean, onToggleRemaining: () -> Unit) {
    val c = MhjTheme.colors
    val dur = durationMs.coerceAtLeast(0L)
    var drag by remember { mutableStateOf<Float?>(null) }
    val frac = drag ?: if (dur > 0) (positionMs.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val seek by rememberUpdatedState(onSeek)
    val durNow by rememberUpdatedState(dur)
    // The part not yet played stays visible: a sunken grey groove.
    val track = if (c.isDark) Color(0xFF2C2C31) else Color(0xFFD9CFC1)
    val shownPos = if (drag != null) (drag!! * dur).toLong() else positionMs.coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth()) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .pointerInput(Unit) {
                        val pad = 12.dp.toPx()
                        fun at(x: Float) = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (durNow <= 0) return@awaitEachGesture
                            down.consume()
                            drag = at(down.position.x)
                            while (true) {
                                val ev = awaitPointerEvent()
                                val p = ev.changes.firstOrNull() ?: break
                                if (!p.pressed) break
                                p.consume()
                                drag = at(p.position.x)
                            }
                            drag?.let { seek((it * durNow).toLong()) }
                            drag = null
                        }
                    },
            ) {
                val pad = 12.dp.toPx()
                val th = 6.dp.toPx()
                val cy = size.height / 2
                val w = size.width - 2 * pad
                drawRoundRect(track, Offset(pad, cy - th / 2), Size(w, th), CornerRadius(th / 2))
                drawRoundRect(Color.Black.copy(alpha = if (c.isDark) 0.7f else 0.18f), Offset(pad, cy - th / 2), Size(w, 2f), CornerRadius(1f))
                drawRoundRect(Color.White.copy(alpha = if (c.isDark) 0.06f else 0.9f), Offset(pad, cy + th / 2), Size(w, 1.2f), CornerRadius(1f))
                val x = pad + w * frac
                if (frac > 0f) drawRoundRect(
                    Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.Gold, MhjColors.GoldHi), pad, x.coerceAtLeast(pad + 1f)),
                    Offset(pad, cy - th / 2 + 0.5f), Size(x - pad, th - 1f), CornerRadius(th / 2),
                )
                val r = (if (drag != null) 12 else 9).dp.toPx()
                drawCircle(Color.Black.copy(alpha = 0.45f), r * 1.05f, Offset(x + r * 0.15f, cy + r * 0.3f))
                drawCircle(Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.Gold, MhjColors.GoldLo), Offset(x - r * 0.3f, cy - r * 0.35f), r * 1.6f), r, Offset(x, cy))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Fa.time(shownPos), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (dur <= 0) "—" else if (showRemaining) "−" + Fa.time(dur - shownPos) else Fa.time(dur),
                    color = if (showRemaining) MhjColors.Gold else c.muted,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleRemaining)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
