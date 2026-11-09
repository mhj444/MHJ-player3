package com.mhj.player.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.model.LibraryFilter
import com.mhj.player.core.model.LibrarySort
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlaylistModel
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeState

private enum class TopPage { ROOT, LIBRARY, PLAYLISTS, FAVORITES, THEME, PLAYLIST_DETAIL }

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

    MhjSheet(
        visible = visible,
        fromTop = true,
        heightFraction = if (page == TopPage.ROOT || page == TopPage.THEME) 0.62f else 0.92f,
        onDismiss = onDismiss,
        modifier = modifier,
        hazeState = hazeState,
    ) {
        when (page) {
            TopPage.ROOT -> {
                BrandLogoHeader()
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeatureRow(Icons.Outlined.LibraryMusic, "کتابخانه", "همهٔ آهنگ‌ها و ویدیوهای گوشی", onClick = { page = TopPage.LIBRARY }) { Chevron() }
                    FeatureRow(Icons.Outlined.QueueMusic, "پلی‌لیست‌ها", "فهرست‌های خودتان و فهرست‌های خودکار", onClick = { page = TopPage.PLAYLISTS }) { Chevron() }
                    FeatureRow(Icons.Outlined.FavoriteBorder, "علاقه‌مندی‌ها", "آهنگ‌هایی که قلب زده‌اید", onClick = {
                        playlistReturn = TopPage.ROOT
                        onOpenFavorites()
                        page = TopPage.PLAYLIST_DETAIL
                    }) { Chevron() }
                    FeatureRow(Icons.Outlined.Palette, "ظاهر", if (settings.theme == AppTheme.MIDNIGHT_GOLD) "شب طلایی" else "سفید چینی", onClick = { page = TopPage.THEME }) { Chevron() }
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
                SheetTitle("ظاهر", "رنگ‌بندی برنامه", { page = TopPage.ROOT })
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeatureRow(Icons.Outlined.DarkMode, "شب طلایی", "زمینهٔ تیره با طلایی", onClick = { if (settings.theme != AppTheme.MIDNIGHT_GOLD) onToggleTheme() }) {
                        GoldSwitch(settings.theme == AppTheme.MIDNIGHT_GOLD) { if (settings.theme != AppTheme.MIDNIGHT_GOLD) onToggleTheme() }
                    }
                    FeatureRow(Icons.Outlined.LightMode, "سفید چینی", "زمینهٔ روشن کرم با طلایی", onClick = { if (settings.theme != AppTheme.PORCELAIN_WHITE) onToggleTheme() }) {
                        GoldSwitch(settings.theme == AppTheme.PORCELAIN_WHITE) { if (settings.theme != AppTheme.PORCELAIN_WHITE) onToggleTheme() }
                    }
                }
            }
        }
    }
}

@Composable
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
    SheetTitle("کتابخانه", "${Fa.num(items.size)} مورد", onBack)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RaisedPill("افزودن فایل", onClick = onAddMusic)
        RaisedPill(if (scanning) "در حال جستجو…" else "جستجوی گوشی", onClick = { if (!scanning) onScanDevice() })
        RaisedPill("افزودن پوشه", onClick = onImportFolder)
    }
    Spacer(Modifier.height(8.dp))
    BasicTextField(
        value = filter.query,
        onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
        singleLine = true,
        textStyle = TextStyle(color = MhjTheme.colors.onBackground, fontSize = 14.sp, fontFamily = com.mhj.player.core.ui.theme.Vazir),
        cursorBrush = SolidColor(MhjColors.Gold),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MhjTheme.colors.controlFill)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (filter.query.isEmpty()) {
                Text("جستجوی نام، خواننده، آلبوم، پوشه…", color = MhjTheme.colors.muted, fontSize = 13.sp)
            }
            inner()
        },
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("همه", filter.kind == null) { onUpdateFilter { it.copy(kind = null) } }
        FilterChip("موسیقی", filter.kind == MediaKind.MUSIC) { onUpdateFilter { it.copy(kind = MediaKind.MUSIC) } }
        FilterChip("ویدیو", filter.kind == MediaKind.VIDEO) { onUpdateFilter { it.copy(kind = MediaKind.VIDEO) } }
        FilterChip("پخش‌شده", filter.playedOnly == true) {
            onUpdateFilter { it.copy(playedOnly = if (it.playedOnly == true) null else true) }
        }
        FilterChip("پخش‌نشده", filter.playedOnly == false) {
            onUpdateFilter { it.copy(playedOnly = if (it.playedOnly == false) null else false) }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibrarySort.entries.forEach { sort ->
            FilterChip(sortName(sort), filter.sort == sort) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 4.dp, end = 4.dp, top = 6.dp,
                bottom = if (selectionMode) 70.dp else 8.dp,
            ),
        ) {
            items(items, key = { it.id }) { item ->
                val selected = item.id in selectedIds
                var menuOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .raised(16.dp, if (selected) 1.dp else 4.dp, Emboss.body, Emboss.light, Emboss.dark)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Emboss.face(selected))
                        .border(
                            1.dp,
                            if (selected) androidx.compose.ui.graphics.SolidColor(MhjColors.Gold) else Emboss.rim(0.3f),
                            RoundedCornerShape(16.dp),
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
                            listOf(item.artist, if (item.kind == MediaKind.VIDEO) "ویدیو" else "موسیقی", item.format).filter { it.isNotBlank() }.joinToString(" · "),
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
                    .background(MhjTheme.colors.elevated)
                    .border(1.dp, Emboss.rim(0.5f), RoundedCornerShape(16.dp))
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
    title: String = "پلی‌لیست‌ها",
    subtitle: String = "برای ساختن، نام بنویسید و + را بزنید",
) {
    var newName by remember { mutableStateOf("") }
    SheetTitle(title, subtitle, onBack)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = newName,
            onValueChange = { newName = it },
            singleLine = true,
            textStyle = TextStyle(color = MhjTheme.colors.onBackground, fontSize = 14.sp, fontFamily = com.mhj.player.core.ui.theme.Vazir),
            cursorBrush = SolidColor(MhjColors.Gold),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MhjTheme.colors.controlFill)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (newName.isEmpty()) Text("نام پلی‌لیست تازه…", color = MhjTheme.colors.muted, fontSize = 13.sp)
                inner()
            },
        )
        RaisedIconButton(Icons.Outlined.Add, "ساختن", {
            onCreatePlaylist(newName)
            newName = ""
        }, size = 44.dp, iconSize = 22.dp)
    }
    Spacer(Modifier.height(10.dp))
    if (playlists.isEmpty()) {
        Text("هنوز پلی‌لیستی نساخته‌اید", color = MhjTheme.colors.muted, fontSize = 13.sp)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                FeatureRow(
                    if (playlist.isSmart) Icons.Outlined.AutoAwesome else Icons.Outlined.QueueMusic,
                    playlistTitle(playlist),
                    if (playlist.isSmart) "خودکار" else "پلی‌لیست شما",
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    onClick = { onOpenPlaylist(playlist.id) },
                ) { Chevron() }
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

    SheetTitle(
        playlistTitle(playlist),
        "${Fa.num(tracks.size)} مورد" + if (playlist?.isSmart == true) " · خودکار" else "",
        onBack,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RaisedPill("پخش همه") { onPlayAll(id) }
        if (canReorder) {
            Text("برای جابه‌جایی، ☰ را نگه دارید و بکشید", color = MhjTheme.colors.muted, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    if (orderedTracks.isEmpty()) {
        Text("این پلی‌لیست خالی است", color = MhjTheme.colors.muted, fontSize = 13.sp)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            userScrollEnabled = draggingIndex < 0,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp, 6.dp),
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
                        .raised(16.dp, if (dragging) 0.dp else 4.dp, Emboss.body, Emboss.light, Emboss.dark)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Emboss.face())
                        .border(
                            width = if (dragging) 1.5.dp else 1.dp,
                            brush = if (dragging) androidx.compose.ui.graphics.SolidColor(MhjColors.Gold) else Emboss.rim(0.3f),
                            shape = RoundedCornerShape(16.dp),
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
private fun Chevron() {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = MhjColors.Gold, modifier = Modifier.size(24.dp))
}

private fun playlistTitle(p: PlaylistModel?): String {
    val n = p?.name ?: return "پلی‌لیست"
    return when (n.trim().lowercase()) {
        "favorites" -> "علاقه‌مندی‌ها"
        "newest" -> "تازه‌ها"
        "not played recently" -> "مدتی پخش‌نشده"
        "most played" -> "پرپخش‌ترین"
        else -> n
    }
}

private fun sortName(s: LibrarySort): String = when (s.name) {
    "NAME" -> "نام"
    "ARTIST" -> "خواننده"
    "ALBUM" -> "آلبوم"
    "DATE_ADDED" -> "تاریخ افزودن"
    "DURATION" -> "مدت"
    "PLAY_COUNT" -> "تعداد پخش"
    "LAST_PLAYED" -> "آخرین پخش"
    "FOLDER" -> "پوشه"
    "FORMAT" -> "قالب"
    else -> s.name.lowercase().replace('_', ' ')
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    RaisedPill(label, selected = selected, onClick = onClick)
}
