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
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.MoreVert
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
    onRenamePlaylist: (Long, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (Long) -> Unit = {},
    onReorderPlaylists: (List<Long>) -> Unit = {},
    backEnabled: Boolean = true,
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

    BackHandler(enabled = visible && backEnabled) {
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
                    FeatureRow(Icons.AutoMirrored.Outlined.QueueMusic, "پلی‌لیست‌ها", "فهرست‌های خودتان و فهرست‌های خودکار", onClick = { page = TopPage.PLAYLISTS }) { Chevron() }
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
                onRename = onRenamePlaylist,
                onDelete = onDeletePlaylist,
                onReorder = onReorderPlaylists,
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
                        GoldSwitch(settings.theme == AppTheme.MIDNIGHT_GOLD, onChange = { if (settings.theme != AppTheme.MIDNIGHT_GOLD) onToggleTheme() })
                    }
                    FeatureRow(Icons.Outlined.LightMode, "سفید چینی", "زمینهٔ روشن کرم با طلایی", onClick = { if (settings.theme != AppTheme.PORCELAIN_WHITE) onToggleTheme() }) {
                        GoldSwitch(settings.theme == AppTheme.PORCELAIN_WHITE, onChange = { if (settings.theme != AppTheme.PORCELAIN_WHITE) onToggleTheme() })
                    }
                }
            }
        }
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
                            listOf(artistLabel(item.artist), if (item.kind == MediaKind.VIDEO) "ویدیو" else "موسیقی", item.format).filter { it.isNotBlank() }.joinToString(" · "),
                            color = MhjTheme.colors.muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!selectionMode) {
                        Box {
                            Icon(
                                Icons.Outlined.MoreVert, "بیشتر", tint = MhjColors.Gold,
                                modifier = Modifier.clip(CircleShape).clickable { menuOpen = true }.padding(8.dp).size(22.dp),
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
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val c = MhjTheme.colors
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var newName by remember { mutableStateOf("") }
    val builtIn = playlists.filter { it.isSmart }
    var mine by remember { mutableStateOf(playlists.filter { !it.isSmart }.map { it.id }) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var moved by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var menuFor by remember { mutableStateOf<PlaylistModel?>(null) }
    var renameFor by remember { mutableStateOf<PlaylistModel?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteFor by remember { mutableStateOf<PlaylistModel?>(null) }
    LaunchedEffect(playlists) { if (draggingId == null) mine = playlists.filter { !it.isSmart }.map { it.id } }
    val byId = playlists.associateBy { it.id }
    val rowStepPx = with(LocalDensity.current) { (68 + 12).dp.toPx() }

    SheetTitle("پلی‌لیست‌ها", "برای ویرایش، حذف یا جابه‌جایی، روی پلی‌لیست انگشت نگه دارید", onBack)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = newName,
            onValueChange = { newName = it },
            singleLine = true,
            textStyle = TextStyle(color = c.onBackground, fontSize = 14.sp, fontFamily = com.mhj.player.core.ui.theme.Vazir),
            cursorBrush = SolidColor(MhjColors.Gold),
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.recess).padding(horizontal = 12.dp, vertical = 13.dp),
            decorationBox = { inner ->
                if (newName.isEmpty()) Text("نام پلی‌لیست تازه…", color = c.muted, fontSize = 13.sp)
                inner()
            },
        )
        Spacer(Modifier.width(10.dp))
        RaisedIconButton(Icons.Outlined.Add, "ساختن", {
            if (newName.isNotBlank()) { onCreatePlaylist(newName); newName = "" }
        }, size = 46.dp, iconSize = 22.dp, active = newName.isNotBlank())
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
        userScrollEnabled = draggingId == null,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp, 6.dp),
    ) {
        items(builtIn, key = { "s${it.id}" }) { pl ->
            FeatureRow(Icons.Outlined.AutoAwesome, playlistTitle(pl), "خودکار", Modifier.height(68.dp), onClick = { onOpenPlaylist(pl.id) }) { Chevron() }
        }
        if (mine.isEmpty()) item(key = "empty") {
            Text("هنوز پلی‌لیستی نساخته‌اید؛ نام بنویسید و + را بزنید.", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
        }
        items(mine.mapNotNull { byId[it] }, key = { it.id }) { pl ->
            val dragging = draggingId == pl.id
            Box(
                Modifier
                    .zIndex(if (dragging) 4f else 0f)
                    .graphicsLayer { if (dragging) { translationY = dragOffsetY; scaleX = 1.04f; scaleY = 1.04f } }
                    .tapOrHoldDrag(
                        key = pl.id,
                        onTap = { onOpenPlaylist(pl.id) },
                        onHold = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            draggingId = pl.id; moved = false; dragOffsetY = 0f
                        },
                        onDrag = { dy ->
                            dragOffsetY += dy
                            if (kotlin.math.abs(dragOffsetY) > 12f) moved = true
                            val cur = mine
                            val from = cur.indexOf(pl.id)
                            if (from >= 0) {
                                val to = when {
                                    dragOffsetY > rowStepPx * 0.5f && from < cur.lastIndex -> from + 1
                                    dragOffsetY < -rowStepPx * 0.5f && from > 0 -> from - 1
                                    else -> from
                                }
                                if (to != from) {
                                    mine = cur.toMutableList().also { l -> l.add(to, l.removeAt(from)) }
                                    dragOffsetY -= (to - from) * rowStepPx
                                }
                            }
                        },
                        onEnd = {
                            if (moved) onReorder(mine) else menuFor = pl
                            draggingId = null; dragOffsetY = 0f
                        },
                    ),
            ) {
                FeatureRow(Icons.AutoMirrored.Outlined.QueueMusic, pl.name, "پلی‌لیست شما", Modifier.height(68.dp)) {
                    Box {
                        Icon(
                            Icons.Outlined.MoreVert, "بیشتر", tint = MhjColors.Gold,
                            modifier = Modifier.clip(CircleShape).clickable { menuFor = pl }.padding(8.dp).size(22.dp),
                        )
                        androidx.compose.material3.DropdownMenu(expanded = menuFor?.id == pl.id, onDismissRequest = { menuFor = null }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("تغییر نام") }, onClick = {
                                menuFor = null; renameFor = pl; renameText = pl.name
                            })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("حذف پلی‌لیست", color = Color(0xFFE57373)) }, onClick = {
                                menuFor = null; deleteFor = pl
                            })
                        }
                    }
                }
            }
        }
    }
    renameFor?.let { pl ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameFor = null },
            title = { Text("تغییر نام پلی‌لیست") },
            text = {
                androidx.compose.material3.OutlinedTextField(renameText, { renameText = it }, singleLine = true, label = { Text("نام تازه") })
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renameFor = null }) { Text("انصراف") } },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(pl.id, renameText.trim())
                    renameFor = null
                }) { Text("ذخیره") }
            },
        )
    }
    deleteFor?.let { pl ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("حذف پلی‌لیست") },
            text = { Text("پلی‌لیست «${pl.name}» پاک شود؟ خود آهنگ‌ها از گوشی پاک نمی‌شوند.") },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleteFor = null }) { Text("انصراف") } },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onDelete(pl.id); deleteFor = null }) { Text("حذف", color = Color(0xFFE57373)) }
            },
        )
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
    val canReorder = !playlist.isSmart
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    // The order on screen; follows the database except while a row is being dragged.
    var order by remember(id) { mutableStateOf(tracks.map { it.id }) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tracks.map { it.id }) {
        if (draggingId == null) order = tracks.map { it.id }
    }
    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val orderedTracks = order.mapNotNull { byId[it] }
    val rowStepPx = with(LocalDensity.current) { (68 + 10).dp.toPx() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    SheetTitle(
        playlistTitle(playlist),
        "${Fa.num(tracks.size)} مورد" + if (playlist.isSmart) " · خودکار" else "",
        onBack,
    )
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        RaisedPill("پخش همه") { onPlayAll(id) }
        if (canReorder && tracks.size > 1) {
            Text("برای جابه‌جایی، روی آهنگ انگشت نگه دارید و بکشید", color = MhjTheme.colors.muted, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
    if (orderedTracks.isEmpty()) {
        Text("این پلی‌لیست خالی است", color = MhjTheme.colors.muted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
        userScrollEnabled = draggingId == null,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp, 6.dp),
    ) {
        items(orderedTracks, key = { it.id }) { item ->
            var menuOpen by remember { mutableStateOf(false) }
            val dragging = draggingId == item.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .zIndex(if (dragging) 4f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationY = dragOffsetY
                            scaleX = 1.04f
                            scaleY = 1.04f
                        }
                    }
                    .raised(16.dp, if (dragging) 10.dp else 4.dp, Emboss.body, Emboss.light, Emboss.dark)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Emboss.face())
                    .border(
                        width = if (dragging) 1.5.dp else 1.dp,
                        brush = if (dragging) SolidColor(MhjColors.Gold) else Emboss.rim(0.3f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .tapOrHoldDrag(
                        key = item.id,
                        enabled = canReorder,
                        onTap = { onPlayItem(item) },
                        onHold = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            draggingId = item.id
                            dragOffsetY = 0f
                        },
                        onDrag = { dy ->
                            dragOffsetY += dy
                            val cur = order
                            val from = cur.indexOf(item.id)
                            if (from >= 0) {
                                val to = when {
                                    dragOffsetY > rowStepPx * 0.5f && from < cur.lastIndex -> from + 1
                                    dragOffsetY < -rowStepPx * 0.5f && from > 0 -> from - 1
                                    else -> from
                                }
                                if (to != from) {
                                    order = cur.toMutableList().also { l -> l.add(to, l.removeAt(from)) }
                                    dragOffsetY -= (to - from) * rowStepPx
                                }
                            }
                        },
                        onEnd = {
                            onReorder(id, order)
                            draggingId = null
                            dragOffsetY = 0f
                        },
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canReorder) {
                    Icon(Icons.Outlined.DragIndicator, null, tint = MhjColors.Gold.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = MhjTheme.colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artistLabel(item.artist), color = MhjTheme.colors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    Icon(
                        Icons.Outlined.MoreVert, "بیشتر", tint = MhjColors.Gold,
                        modifier = Modifier.clip(CircleShape).clickable { menuOpen = true }.padding(8.dp).size(22.dp),
                    )
                    androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("افزودن به پلی‌لیست") }, onClick = { menuOpen = false; onAddToPlaylistAsk(item) })
                        if (canReorder) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("حذف از این پلی‌لیست") }, onClick = { menuOpen = false; onRemove(id, item.id) })
                        }
                        androidx.compose.material3.DropdownMenuItem(text = { Text("اشتراک‌گذاری") }, onClick = { menuOpen = false; onShare(item) })
                    }
                }
            }
        }
    }
}

/** "<unknown>" and empty artists read as «خوانندهٔ نامشخص». */
fun artistLabel(a: String?): String {
    val t = a?.trim().orEmpty()
    return if (t.isEmpty() || t.equals("<unknown>", true) || t.equals("unknown artist", true) || t.equals("unknown", true)) "خوانندهٔ نامشخص" else t
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
