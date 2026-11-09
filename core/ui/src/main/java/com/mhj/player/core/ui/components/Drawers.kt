package com.mhj.player.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.model.LibraryFilter
import com.mhj.player.core.model.LibrarySort
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlaylistModel
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class TopPage { ROOT, LIBRARY, PLAYLISTS, FAVORITES, THEME, PLAYLIST_DETAIL }
private enum class AudioPage { ROOT, SPEED, TIMER, EQUALIZER }

@Composable
fun TopMenuDrawer(
    visible: Boolean,
    settings: AppSettings,
    playlists: List<PlaylistModel>,
    libraryItems: List<MediaItemModel>,
    playlistTracks: List<MediaItemModel>,
    activePlaylist: PlaylistModel?,
    filter: LibraryFilter,
    selectedIds: Set<String>,
    scanning: Boolean,
    onDismiss: () -> Unit,
    onToggleTheme: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onPlayPlaylist: (Long) -> Unit,
    onUpdateFilter: ((LibraryFilter) -> LibraryFilter) -> Unit,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onPlayItem: (MediaItemModel) -> Unit,
    onScanDevice: () -> Unit,
    onImportFolder: () -> Unit,
    onAddMusic: () -> Unit = {},
    onShareItem: (MediaItemModel) -> Unit = {},
    onDeleteItemAsk: (MediaItemModel) -> Unit = {},
    onRenameItemAsk: (MediaItemModel) -> Unit = {},
    onAddItemToPlaylistAsk: (MediaItemModel) -> Unit = {},
    onRemoveFromPlaylist: (Long, String) -> Unit = { _, _ -> },
    onReorderPlaylist: (Long, List<String>) -> Unit = { _, _ -> },
    onSharePlaylistTrack: (MediaItemModel) -> Unit = {},
    onAddPlaylistTrackToPlaylistAsk: (MediaItemModel) -> Unit = {},
    selectionMode: Boolean = false,
    onEnterSelection: (String) -> Unit = {},
    onShareSelected: () -> Unit = {},
    onAddSelectedToPlaylist: () -> Unit = {},
    onDeleteSelectedAsk: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var page by remember { mutableStateOf(TopPage.ROOT) }
    var playlistReturn by remember { mutableStateOf(TopPage.PLAYLISTS) }
    LaunchedEffect(visible) {
        if (!visible) {
            page = TopPage.ROOT
            onClearSelection()
        }
    }

    BackHandler(enabled = visible) {
        when {
            selectionMode -> onClearSelection()
            page == TopPage.ROOT -> onDismiss()
            page == TopPage.PLAYLIST_DETAIL -> page = playlistReturn
            else -> page = TopPage.ROOT
        }
    }

    OverlaySheet(
        visible = visible,
        fromTop = true,
        tall = page == TopPage.LIBRARY || page == TopPage.PLAYLIST_DETAIL,
        onDismiss = onDismiss,
        modifier = modifier,
        hazeState = hazeState,
    ) {
        when (page) {
            TopPage.ROOT -> {
                BrandLogoHeader()
                Spacer(Modifier.height(14.dp))
                MenuNavRow("Library", "کتابخانه", if (MhjTheme.colors.isDark) R.drawable.icon_mid_library else R.drawable.icon_porc_library) {
                    page = TopPage.LIBRARY
                }
                Spacer(Modifier.height(8.dp))
                MenuNavRow("Playlists", "پلی‌لیست‌ها", if (MhjTheme.colors.isDark) R.drawable.icon_mid_audio_wave else R.drawable.icon_porc_audio_wave) {
                    page = TopPage.PLAYLISTS
                }
                Spacer(Modifier.height(8.dp))
                MenuNavRow("Favorites", "علاقه‌مندی‌ها", if (MhjTheme.colors.isDark) R.drawable.icon_mid_home else R.drawable.icon_porc_home) {
                    playlistReturn = TopPage.ROOT
                    onOpenFavorites()
                    page = TopPage.PLAYLIST_DETAIL
                }
                Spacer(Modifier.height(8.dp))
                MenuNavRow("Theme", "تم", if (MhjTheme.colors.isDark) R.drawable.icon_mid_night else R.drawable.icon_porc_night) {
                    page = TopPage.THEME
                }
            }
            TopPage.LIBRARY -> LibraryPage(
                items = libraryItems,
                filter = filter,
                selectedIds = selectedIds,
                selectionMode = selectionMode,
                scanning = scanning,
                onBack = {
                    if (selectionMode) onClearSelection() else page = TopPage.ROOT
                },
                onUpdateFilter = onUpdateFilter,
                onToggleSelect = onToggleSelect,
                onEnterSelection = onEnterSelection,
                onClearSelection = onClearSelection,
                onPlayItem = onPlayItem,
                onScanDevice = onScanDevice,
                onImportFolder = onImportFolder,
                onAddMusic = onAddMusic,
                onShareItem = onShareItem,
                onDeleteItemAsk = onDeleteItemAsk,
                onRenameItemAsk = onRenameItemAsk,
                onAddItemToPlaylistAsk = onAddItemToPlaylistAsk,
                onShareSelected = onShareSelected,
                onAddSelectedToPlaylist = onAddSelectedToPlaylist,
                onDeleteSelectedAsk = onDeleteSelectedAsk,
            )
            TopPage.PLAYLISTS -> PlaylistsPage(
                playlists = playlists.filter {
                    !(it.name.equals("Favorites", ignoreCase = true) && !it.isSmart)
                },
                onBack = { page = TopPage.ROOT },
                onCreatePlaylist = onCreatePlaylist,
                onOpenPlaylist = { id ->
                    playlistReturn = TopPage.PLAYLISTS
                    onOpenPlaylist(id)
                    page = TopPage.PLAYLIST_DETAIL
                },
            )
            TopPage.FAVORITES -> {
                // Favorites opens PLAYLIST_DETAIL via onOpenFavorites from ROOT
                page = TopPage.PLAYLIST_DETAIL
            }
            TopPage.PLAYLIST_DETAIL -> PlaylistDetailPage(
                playlist = activePlaylist,
                tracks = playlistTracks,
                onBack = { page = playlistReturn },
                onPlayAll = { id -> onPlayPlaylist(id) },
                onPlayItem = onPlayItem,
                onRemove = onRemoveFromPlaylist,
                onReorder = onReorderPlaylist,
                onShare = onSharePlaylistTrack,
                onAddToPlaylistAsk = onAddPlaylistTrackToPlaylistAsk,
            )
            TopPage.THEME -> {
                PageHeader("Theme", "تم") { page = TopPage.ROOT }
                Spacer(Modifier.height(12.dp))
                MenuCard(
                    iconRes = if (settings.theme == AppTheme.MIDNIGHT_GOLD) {
                        if (MhjTheme.colors.isDark) R.drawable.icon_mid_home else R.drawable.icon_porc_home
                    } else {
                        if (MhjTheme.colors.isDark) R.drawable.icon_mid_night else R.drawable.icon_porc_night
                    },
                    title = if (settings.theme == AppTheme.MIDNIGHT_GOLD) "Midnight Gold" else "Porcelain White",
                    subtitle = "Tap to switch theme",
                    onClick = onToggleTheme,
                )
            }
        }
    }
}

@Composable
fun BottomAudioDrawer(
    visible: Boolean,
    settings: AppSettings,
    abLabel: String = "A-B",
    abActive: Boolean = false,
    transcriptRunning: Boolean = false,
    transcriptDownloading: Boolean = false,
    transcriptProgress: Float = 0f,
    transcriptStatusFa: String = "تبدیل گفتار رسانه به متن",
    transcriptErrorFa: String? = null,
    isFavorite: Boolean = false,
    onDismiss: () -> Unit,
    onToggleTranscript: () -> Unit = {},
    onOpenTranscriptFolder: () -> Unit = {},
    onNightMode: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedNudge: (Float) -> Unit,
    onAbClick: () -> Unit = {},
    onOpenSystemEq: () -> Unit = {},
    onEnsureEqualizer: () -> Unit = {},
    onEqBandChange: (Int, Float) -> Unit = { _, _ -> },
    onEqPreset: (String) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onTimerChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var page by remember { mutableStateOf(AudioPage.ROOT) }
    LaunchedEffect(visible) {
        if (!visible) page = AudioPage.ROOT
    }
    val dark = MhjTheme.colors.isDark
    val speedLabel = String.format("%.1fx", settings.playbackSpeed)
    val timerLabel = if (settings.sleepTimerMinutes == 0) "Off" else "${settings.sleepTimerMinutes}m"

    BackHandler(enabled = visible) {
        if (page != AudioPage.ROOT) page = AudioPage.ROOT else onDismiss()
    }

    OverlaySheet(
        visible = visible,
        fromTop = false,
        tall = page == AudioPage.EQUALIZER,
        onDismiss = {
            page = AudioPage.ROOT
            onDismiss()
        },
        modifier = modifier,
        hazeState = hazeState,
    ) {
        when (page) {
            AudioPage.ROOT -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Audio Processing",
                            color = MhjTheme.colors.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        )
                        Text("پردازش صدا", color = MhjTheme.colors.muted, fontSize = 12.sp)
                    }
                    Image(
                        painter = painterResource(
                            if (dark) R.drawable.icon_mid_equalizer else R.drawable.icon_porc_equalizer,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(16.dp))
                TranscriptRow(
                    dark = dark,
                    running = transcriptRunning,
                    downloading = transcriptDownloading,
                    progress = transcriptProgress,
                    statusFa = transcriptStatusFa,
                    errorFa = transcriptErrorFa,
                    onToggle = onToggleTranscript,
                    onOpenFolder = onOpenTranscriptFolder,
                )
                Spacer(Modifier.height(10.dp))
                GlassToggleCard(
                    iconRes = if (dark) R.drawable.icon_mid_night else R.drawable.icon_porc_night,
                    label = "Night Mode Audio",
                    subtitle = "حالت شب",
                    checked = settings.nightModeAudio,
                    onChange = onNightMode,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OfficialGridTile(
                        Modifier.weight(1f),
                        if (dark) R.drawable.icon_mid_ab_loop else R.drawable.icon_porc_ab_loop,
                        abLabel,
                        if (abActive) "On" else "Loop",
                        onAbClick,
                    )
                    OfficialGridTile(
                        Modifier.weight(1f),
                        if (dark) R.drawable.icon_mid_speed else R.drawable.icon_porc_speed,
                        "Speed",
                        speedLabel,
                    ) { page = AudioPage.SPEED }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OfficialGridTile(
                        Modifier.weight(1f),
                        if (dark) R.drawable.icon_mid_equalizer else R.drawable.icon_porc_equalizer,
                        "Equalizer",
                        "Bands",
                    ) {
                        onEnsureEqualizer()
                        page = AudioPage.EQUALIZER
                    }
                    FavoriteHeartButton(
                        favorited = isFavorite,
                        onClick = onToggleFavorite,
                    )
                    OfficialGridTile(
                        Modifier.weight(1f),
                        if (dark) R.drawable.icon_mid_timer else R.drawable.icon_porc_timer,
                        "Timer",
                        timerLabel,
                    ) { page = AudioPage.TIMER }
                }
            }
            AudioPage.SPEED -> SpeedOnlyDial(
                settings = settings,
                onBack = { page = AudioPage.ROOT },
                onSpeedChange = onSpeedChange,
                onSpeedNudge = onSpeedNudge,
            )
            AudioPage.TIMER -> TimerOnlyDial(
                settings = settings,
                onBack = { page = AudioPage.ROOT },
                onTimerChange = onTimerChange,
            )
            AudioPage.EQUALIZER -> EqualizerPage(
                settings = settings,
                onBack = { page = AudioPage.ROOT },
                onBandChange = onEqBandChange,
                onPreset = onEqPreset,
                onOpenSystemEq = onOpenSystemEq,
            )
        }
    }
}

@Composable
private fun FavoriteHeartButton(
    favorited: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .softEmboss(16.dp, MhjTheme.colors.lightShadow, MhjTheme.colors.darkShadow, 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MhjTheme.colors.elevated)
            .softGoldHairline(16.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (favorited) "♥" else "♡",
            color = if (favorited) Color(0xFFE53935) else MhjColors.GoldHi,
            fontSize = 28.sp,
        )
    }
}

@Composable
private fun ColumnScope.EqualizerPage(
    settings: AppSettings,
    onBack: () -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onPreset: (String) -> Unit,
    onOpenSystemEq: () -> Unit,
) {
    val bands = settings.eqBands.let { if (it.size >= 5) it else listOf(0f, 0f, 0f, 0f, 0f) }
    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    PageHeader("Equalizer", "اکولایزر", onBack)
    Spacer(Modifier.height(6.dp))
    Text(
        "Live session · android.media.audiofx.Equalizer",
        color = MhjTheme.colors.muted,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Flat", "Bass", "Treble", "Vocal").forEach { name ->
            ActionChip(name) { onPreset(name) }
        }
    }
    Spacer(Modifier.height(8.dp))
    ActionChip("System EQ panel") { onOpenSystemEq() }
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        bands.forEachIndexed { index, value ->
            Text(labels[index], color = MhjTheme.colors.onBackground, fontSize = 13.sp)
            Slider(
                value = value,
                onValueChange = { onBandChange(index, it) },
                valueRange = -1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MhjColors.GoldHi,
                    activeTrackColor = MhjColors.Gold,
                    inactiveTrackColor = MhjTheme.colors.controlFill,
                ),
            )
        }
    }
}

@Composable
private fun SpeedOnlyDial(
    settings: AppSettings,
    onBack: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedNudge: (Float) -> Unit,
) {
    val c = MhjTheme.colors
    val dark = c.isDark
    val speedLabel = String.format("%.1fx", settings.playbackSpeed)
    val progress = ((settings.playbackSpeed - 0.25f) / 2.75f).coerceIn(0f, 1f)
    PageHeader("Playback Speed", "سرعت پخش", onBack)
    Spacer(Modifier.height(8.dp))
    // Official speed glyph (needle sits on the embossed semicircle in the asset)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .softEmboss(24.dp, c.lightShadow, c.darkShadow, 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .softGoldHairline(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (dark) R.drawable.icon_mid_speed else R.drawable.icon_porc_speed),
            contentDescription = "Speed",
            modifier = Modifier.size(112.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            speedLabel,
            color = c.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
    Spacer(Modifier.height(10.dp))
    Slider(
        value = progress,
        onValueChange = { p ->
            val speed = (0.25f + p * 2.75f).let { (it * 20f).roundToInt() / 20f }
            onSpeedChange(speed.coerceIn(0.25f, 3.0f))
        },
        colors = SliderDefaults.colors(
            thumbColor = MhjColors.GoldHi,
            activeTrackColor = MhjColors.Gold,
            inactiveTrackColor = c.controlFill,
        ),
    )
    Spacer(Modifier.height(4.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmbossCircle(onClick = { onSpeedNudge(-0.25f) }, diameter = 44.dp) {
            Image(
                painter = painterResource(if (dark) R.drawable.icon_mid_minus else R.drawable.icon_porc_minus),
                contentDescription = "Slower",
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(speedLabel, color = c.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        EmbossCircle(onClick = { onSpeedNudge(0.25f) }, diameter = 44.dp) {
            Image(
                painter = painterResource(if (dark) R.drawable.icon_mid_plus else R.drawable.icon_porc_plus),
                contentDescription = "Faster",
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun TimerOnlyDial(
    settings: AppSettings,
    onBack: () -> Unit,
    onTimerChange: (Int) -> Unit,
) {
    val c = MhjTheme.colors
    val dark = c.isDark
    PageHeader("Sleep Timer", "تایمر خواب", onBack)
    Spacer(Modifier.height(8.dp))
    DialCard(
        centerLabel = if (settings.sleepTimerMinutes == 0) "Off" else "${settings.sleepTimerMinutes} min",
        progress = (settings.sleepTimerMinutes / 60f).coerceIn(0f, 1f),
        ticks = emptyList(),
        onProgressChange = { p -> onTimerChange((p * 60f).roundToInt().coerceIn(0, 60)) },
    )
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BitmapPillChip(
            Modifier.weight(1f),
            if (dark) R.drawable.icon_mid_timer_off else R.drawable.icon_porc_timer_off,
            "Off",
            settings.sleepTimerMinutes == 0,
        ) { onTimerChange(0) }
        BitmapPillChip(
            Modifier.weight(1f),
            if (dark) R.drawable.icon_mid_stop_at_end else R.drawable.icon_porc_stop_at_end,
            "At end",
            false,
        ) { onTimerChange(0) }
        BitmapPillChip(
            Modifier.weight(1f),
            if (dark) R.drawable.icon_mid_custom_time else R.drawable.icon_porc_custom_time,
            "Custom",
            settings.sleepTimerMinutes > 0,
        ) { onTimerChange(30) }
    }
}

@Composable
private fun ColumnScope.LibraryPage(
    items: List<MediaItemModel>,
    filter: LibraryFilter,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    scanning: Boolean,
    onBack: () -> Unit,
    onUpdateFilter: ((LibraryFilter) -> LibraryFilter) -> Unit,
    onToggleSelect: (String) -> Unit,
    onEnterSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onPlayItem: (MediaItemModel) -> Unit,
    onScanDevice: () -> Unit,
    onImportFolder: () -> Unit,
    onAddMusic: () -> Unit,
    onShareItem: (MediaItemModel) -> Unit,
    onDeleteItemAsk: (MediaItemModel) -> Unit,
    onRenameItemAsk: (MediaItemModel) -> Unit,
    onAddItemToPlaylistAsk: (MediaItemModel) -> Unit,
    onShareSelected: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
    onDeleteSelectedAsk: () -> Unit,
) {
    PageHeader("Library", "کتابخانه", onBack)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionChip("Add music", onClick = onAddMusic)
        ActionChip(if (scanning) "Scanning…" else "Scan device", enabled = !scanning, onClick = onScanDevice)
        ActionChip("Import folder", onClick = onImportFolder)
    }
    Spacer(Modifier.height(8.dp))
    BasicTextField(
        value = filter.query,
        onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
        singleLine = true,
        textStyle = TextStyle(color = MhjTheme.colors.onBackground, fontSize = 14.sp),
        cursorBrush = SolidColor(MhjColors.Gold),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MhjTheme.colors.controlFill)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (filter.query.isEmpty()) {
                Text("Search name, artist, album, folder…", color = MhjTheme.colors.muted, fontSize = 13.sp)
            }
            inner()
        },
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip("All", filter.kind == null) { onUpdateFilter { it.copy(kind = null) } }
        FilterChip("Music", filter.kind == MediaKind.MUSIC) { onUpdateFilter { it.copy(kind = MediaKind.MUSIC) } }
        FilterChip("Video", filter.kind == MediaKind.VIDEO) { onUpdateFilter { it.copy(kind = MediaKind.VIDEO) } }
        FilterChip("Played", filter.playedOnly == true) {
            onUpdateFilter { it.copy(playedOnly = if (it.playedOnly == true) null else true) }
        }
        FilterChip("Unplayed", filter.playedOnly == false) {
            onUpdateFilter { it.copy(playedOnly = if (it.playedOnly == false) null else false) }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LibrarySort.entries.forEach { sort ->
            FilterChip(sort.name.lowercase().replace('_', ' '), filter.sort == sort) {
                onUpdateFilter { it.copy(sort = sort) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = true),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = if (selectionMode) 64.dp else 0.dp,
            ),
        ) {
            items(items, key = { it.id }) { item ->
                val selected = item.id in selectedIds
                var menuOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) MhjTheme.colors.controlFill else MhjTheme.colors.elevated)
                        .border(
                            1.dp,
                            if (selected) MhjColors.Gold.copy(alpha = 0.7f) else Color.Transparent,
                            RoundedCornerShape(14.dp),
                        )
                        .pointerInput(selectionMode, item.id) {
                            detectTapGestures(
                                onLongPress = { onEnterSelection(item.id) },
                                onTap = {
                                    if (selectionMode) onToggleSelect(item.id) else onPlayItem(item)
                                },
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectionMode) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected) MhjColors.Gold else Color.Transparent)
                                .border(1.dp, MhjColors.Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            color = MhjTheme.colors.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.artist} · ${item.kind.name.lowercase()} · ${item.format.ifEmpty { "?" }}",
                            color = MhjTheme.colors.muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!selectionMode) {
                        Box {
                            Text(
                                "⋮",
                                color = MhjColors.GoldHi,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clickable { menuOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            androidx.compose.material3.DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("اشتراک‌گذاری") },
                                    onClick = { menuOpen = false; onShareItem(item) },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("حذف از دستگاه") },
                                    onClick = { menuOpen = false; onDeleteItemAsk(item) },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("افزودن به پلی‌لیست") },
                                    onClick = { menuOpen = false; onAddItemToPlaylistAsk(item) },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("تغییر نام") },
                                    onClick = { menuOpen = false; onRenameItemAsk(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (selectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE1A1A20))
                    .softGoldHairline(16.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "اشتراک (${selectedIds.size})",
                    color = MhjColors.GoldHi,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onShareSelected),
                )
                Text(
                    "پلی‌لیست",
                    color = MhjColors.GoldHi,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onAddSelectedToPlaylist),
                )
                Text(
                    "حذف",
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onDeleteSelectedAsk),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PlaylistsPage(
    playlists: List<PlaylistModel>,
    onBack: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    title: String = "Playlists",
    subtitle: String = "پلی‌لیست‌ها",
) {
    var newName by remember { mutableStateOf("") }
    PageHeader(title, subtitle, onBack)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = newName,
            onValueChange = { newName = it },
            singleLine = true,
            textStyle = TextStyle(color = MhjTheme.colors.onBackground, fontSize = 14.sp),
            cursorBrush = SolidColor(MhjColors.Gold),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MhjTheme.colors.controlFill)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (newName.isEmpty()) Text("New playlist name…", color = MhjTheme.colors.muted, fontSize = 13.sp)
                inner()
            },
        )
        Text(
            "Create",
            color = MhjColors.GoldHi,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onCreatePlaylist(newName)
                    newName = ""
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
    Spacer(Modifier.height(10.dp))
    if (playlists.isEmpty()) {
        Text("No playlists yet", color = MhjTheme.colors.muted, fontSize = 13.sp)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                MenuCard(
                    iconRes = if (MhjTheme.colors.isDark) R.drawable.icon_mid_library else R.drawable.icon_porc_library,
                    title = playlist.name,
                    subtitle = if (playlist.isSmart) "Smart · Open" else "Open",
                    onClick = { onOpenPlaylist(playlist.id) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PlaylistDetailPage(
    playlist: PlaylistModel?,
    tracks: List<MediaItemModel>,
    onBack: () -> Unit,
    onPlayAll: (Long) -> Unit,
    onPlayItem: (MediaItemModel) -> Unit,
    onRemove: (Long, String) -> Unit,
    onReorder: (Long, List<String>) -> Unit,
    onShare: (MediaItemModel) -> Unit,
    onAddToPlaylistAsk: (MediaItemModel) -> Unit,
) {
    val id = playlist?.id ?: return
    val canReorder = playlist?.isSmart != true
    var order by remember(playlist?.id) { mutableStateOf(tracks.map { it.id }) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tracks.map { it.id }, draggingIndex) {
        // Don't clobber mid-drag; only sync when membership changes
        if (draggingIndex < 0 &&
            (order.toSet() != tracks.map { it.id }.toSet() || order.size != tracks.size)
        ) {
            order = tracks.map { it.id }
        }
    }
    val orderedTracks = remember(order, tracks) {
        val map = tracks.associateBy { it.id }
        order.mapNotNull { map[it] }
    }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 64.dp.toPx() }

    PageHeader(
        playlist?.name ?: "Playlist",
        if (playlist?.isSmart == true) "پیش‌فرض" else if (playlist?.name.equals("Favorites", true) == true) "علاقه‌مندی‌ها" else "پلی‌لیست",
    ) { onBack() }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ActionChip("Play all") { onPlayAll(id) }
        if (canReorder) {
            Text("Long-press ☰ and drag", color = MhjTheme.colors.muted, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    if (orderedTracks.isEmpty()) {
        Text("No songs in this playlist", color = MhjTheme.colors.muted, fontSize = 13.sp)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            userScrollEnabled = draggingIndex < 0,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(orderedTracks, key = { _, t -> t.id }) { index, item ->
                var menuOpen by remember { mutableStateOf(false) }
                val dragging = draggingIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 4f else 0f)
                        .graphicsLayer {
                            if (dragging) {
                                translationY = dragOffsetY
                                scaleX = 1.05f
                                scaleY = 1.05f
                                shadowElevation = 24f
                                alpha = 0.97f
                            }
                        }
                        .shadow(
                            elevation = if (dragging) 12.dp else 0.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = MhjColors.Gold.copy(alpha = 0.55f),
                            spotColor = MhjColors.Gold.copy(alpha = 0.45f),
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (dragging) MhjTheme.colors.controlFill else MhjTheme.colors.elevated)
                        .border(
                            width = if (dragging) 1.5.dp else 0.dp,
                            color = if (dragging) MhjColors.Gold else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canReorder) {
                        // Dedicated drag handle — long-press here lifts the row (avoids clickable conflict)
                        Text(
                            "☰",
                            color = MhjColors.GoldHi,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .pointerInput(order, index, rowHeightPx) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            if (draggingIndex >= 0) onReorder(id, order)
                                            draggingIndex = -1
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            onReorder(id, order)
                                            draggingIndex = -1
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val from = draggingIndex
                                            if (from < 0) return@detectDragGesturesAfterLongPress
                                            val shift = when {
                                                dragOffsetY > rowHeightPx * 0.5f -> 1
                                                dragOffsetY < -rowHeightPx * 0.5f -> -1
                                                else -> 0
                                            }
                                            if (shift != 0) {
                                                val to = (from + shift).coerceIn(0, order.lastIndex)
                                                if (to != from) {
                                                    val mutable = order.toMutableList()
                                                    val moved = mutable.removeAt(from)
                                                    mutable.add(to, moved)
                                                    order = mutable
                                                    draggingIndex = to
                                                    dragOffsetY -= shift * rowHeightPx
                                                }
                                            }
                                        },
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(enabled = !dragging) { onPlayItem(item) },
                    ) {
                        Text(item.title, color = MhjTheme.colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, color = MhjTheme.colors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box {
                        Text(
                            "⋮",
                            color = MhjColors.GoldHi,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clickable { menuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("افزودن به پلی‌لیست") },
                                onClick = { menuOpen = false; onAddToPlaylistAsk(item) },
                            )
                            if (canReorder || playlist?.name.equals("Favorites", true) == true) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("حذف از این پلی‌لیست") },
                                    onClick = {
                                        menuOpen = false
                                        onRemove(id, item.id)
                                    },
                                )
                            }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("اشتراک‌گذاری") },
                                onClick = { menuOpen = false; onShare(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "←",
            color = MhjColors.GoldHi,
            fontSize = 20.sp,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(4.dp),
        )
        Column {
            Text(title, color = MhjTheme.colors.onBackground, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(subtitle, color = MhjTheme.colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MenuNavRow(title: String, subtitle: String, iconRes: Int, onClick: () -> Unit) {
    MenuCard(iconRes = iconRes, title = title, subtitle = subtitle, onClick = onClick)
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) MhjColors.GoldHi else MhjTheme.colors.muted,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MhjTheme.colors.controlFill else MhjTheme.colors.elevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionChip(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        color = if (enabled) MhjColors.GoldHi else MhjTheme.colors.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MhjTheme.colors.controlFill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun DialCard(
    centerLabel: String,
    progress: Float,
    ticks: List<String>,
    onProgressChange: (Float) -> Unit,
) {
    val c = MhjTheme.colors
    // Top semicircle gauge (classic speedometer): 180° → 0° counterclockwise over the top.
    val startDeg = 180.0
    val sweepDeg = -180.0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .softEmboss(24.dp, c.lightShadow, c.darkShadow, 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .softGoldHairline(24.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    // Pivot slightly above vertical center so needle sits on the upper arc
                    val cy = size.height * 0.62f
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                    if (deg < 0) deg += 360.0
                    // Map upper half (180→270→360/0) onto 0..1
                    val p = when {
                        deg in 180.0..360.0 -> (deg - 180.0) / 180.0
                        deg in 0.0..0.01 -> 1.0
                        else -> if (deg < 90) 1.0 else 0.0
                    }
                    onProgressChange(p.toFloat().coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(148.dp)) {
            val stroke = 10.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height * 0.72f
            val radius = size.minDimension * 0.42f
            drawArc(
                color = c.controlFill.copy(alpha = 0.85f),
                startAngle = startDeg.toFloat(),
                sweepAngle = sweepDeg.toFloat(),
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = MhjColors.Gold,
                startAngle = startDeg.toFloat(),
                sweepAngle = (sweepDeg * progress.coerceIn(0f, 1f)).toFloat(),
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            ticks.forEachIndexed { i, _ ->
                val t = i / (ticks.size - 1f).coerceAtLeast(1f)
                val ang = Math.toRadians(startDeg + sweepDeg * t)
                val rOuter = radius + stroke * 0.2f
                val rInner = radius - 12.dp.toPx()
                drawLine(
                    color = MhjColors.GoldHi.copy(alpha = 0.55f),
                    start = Offset(cx + cos(ang).toFloat() * rInner, cy + sin(ang).toFloat() * rInner),
                    end = Offset(cx + cos(ang).toFloat() * rOuter, cy + sin(ang).toFloat() * rOuter),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            val nang = Math.toRadians(startDeg + sweepDeg * progress.coerceIn(0f, 1f).toDouble())
            // Tip lands on the arc stroke centerline (not past it)
            val tipR = radius
            drawLine(
                brush = Brush.linearGradient(listOf(MhjColors.GoldHi, MhjColors.GoldLo)),
                start = Offset(cx, cy),
                end = Offset(
                    cx + cos(nang).toFloat() * tipR,
                    cy + sin(nang).toFloat() * tipR,
                ),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(MhjColors.GoldHi, MhjColors.GoldLo)),
                radius = 7.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
        Text(
            centerLabel,
            color = c.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
        )
    }
}

@Composable
private fun OverlaySheet(
    visible: Boolean,
    fromTop: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    tall: Boolean = false,
    body: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (fromTop) 0.dp else 32.dp,
        topEnd = if (fromTop) 0.dp else 32.dp,
        bottomStart = if (fromTop) 32.dp else 0.dp,
        bottomEnd = if (fromTop) 32.dp else 0.dp,
    )
    val frostStyle = HazeStyle(
        backgroundColor = Color(0x99101014),
        tints = listOf(
            HazeTint(Color(0x6614141A)),
            HazeTint(MhjColors.Gold.copy(alpha = 0.03f)),
        ),
        blurRadius = 22.dp,
        noiseFactor = 0.06f,
    )
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(160)) + slideInVertically(tween(280)) { if (fromTop) -it / 2 else it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(220)) { if (fromTop) -it / 2 else it / 2 },
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(if (fromTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(if (tall) 0.90f else if (fromTop) 0.72f else 0.62f)
                    .clip(shape)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(state = hazeState, style = frostStyle)
                        } else {
                            Modifier.background(Color(0xF214141A))
                        },
                    )
                    .background(Color(0x55101014))
                    .softGoldHairline(32.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                content = body,
            )
        }
    }
}

/**
 * Unified speech-to-text row: main press toggles transcription; trailing folder opens saved text.
 * Replaces the old Dialogue Clarity toggle (pink-marked position → folder).
 */
@Composable
private fun TranscriptRow(
    dark: Boolean,
    running: Boolean,
    downloading: Boolean,
    progress: Float,
    statusFa: String,
    errorFa: String?,
    onToggle: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    val c = MhjTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xAA1C1C22) else Color(0xEEF7F0E6),
                        if (dark) Color(0x99141418) else Color(0xDDEFE4D4),
                    ),
                ),
            )
            .softGoldHairline(18.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.icon_mid_dialogue else R.drawable.icon_porc_dialogue,
                ),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (running) "توقف تبدیل" else "تبدیل به متن",
                    color = c.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    errorFa ?: statusFa,
                    color = if (errorFa != null) Color(0xFFE8A0A0) else c.primary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 2,
                )
                if (downloading || (running && progress in 0f..0.999f)) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.controlFill),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                                .height(3.dp)
                                .background(MhjColors.Gold),
                        )
                    }
                }
            }
        }
        // Folder control — same row, where the old toggle sat (pink mark)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.controlFill)
                .border(1.dp, MhjColors.Gold.copy(alpha = 0.65f), CircleShape)
                .clickable(onClick = onOpenFolder),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.icon_mid_library else R.drawable.icon_porc_library,
                ),
                contentDescription = "پوشه متن",
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun GlassToggleCard(
    iconRes: Int,
    label: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val c = MhjTheme.colors
    val dark = c.isDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xAA1C1C22) else Color(0xEEF7F0E6),
                        if (dark) Color(0x99141418) else Color(0xDDEFE4D4),
                    ),
                ),
            )
            .softGoldHairline(18.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            contentScale = ContentScale.Fit,
        )
        Column(Modifier.weight(1f)) {
            Text(label, color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = c.primary.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        Image(
            painter = painterResource(
                if (checked) {
                    if (dark) R.drawable.icon_mid_switch_on else R.drawable.icon_porc_switch_on
                } else {
                    if (dark) R.drawable.icon_mid_switch_off else R.drawable.icon_porc_switch_off
                },
            ),
            contentDescription = if (checked) "On" else "Off",
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clickable { onChange(!checked) },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun OfficialGridTile(
    modifier: Modifier,
    iconRes: Int,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Column(
        modifier = modifier
            .height(118.dp)
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (c.isDark) Color(0xAA1C1C22) else Color(0xEEF7F0E6),
                        if (c.isDark) Color(0x99141418) else Color(0xDDEFE4D4),
                    ),
                ),
            )
            .softGoldHairline(18.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit,
        )
        Text(label, color = c.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = c.primary.copy(alpha = 0.9f), fontSize = 11.sp)
    }
}

@Composable
private fun MenuCard(iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
    val c = MhjTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(c.elevated, c.surface)))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.controlFill),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Column {
            Text(title, color = c.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BitmapPillChip(
    modifier: Modifier,
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = MhjTheme.colors
    Row(
        modifier = modifier
            .softEmboss(18.dp, c.lightShadow, c.darkShadow, 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) c.controlFill else c.elevated)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.size(6.dp))
        Text(label, color = if (selected) c.primary else c.muted, fontSize = 12.sp)
    }
}
