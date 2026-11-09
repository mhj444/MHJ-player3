package com.mhj.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhj.player.MhjApp
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.ui.components.BoardBackground
import android.app.PictureInPictureParams
import android.util.Rational
import com.mhj.player.core.ui.components.AudioActions
import com.mhj.player.core.ui.components.PickerPlaylist
import com.mhj.player.core.ui.components.PlaylistPickerSheet
import androidx.compose.runtime.produceState
import com.mhj.player.core.ui.components.BottomAudioDrawer
import com.mhj.player.core.ui.components.SavedTranscript
import com.mhj.player.core.ui.components.TranscriptUi
import com.mhj.player.core.ui.components.shareText
import com.mhj.player.core.ui.components.TopMenuDrawer
import com.mhj.player.core.ui.components.TranscriptViewerOverlay
import com.mhj.player.core.ui.gestures.playerEdgeSwipes
import com.mhj.player.core.ui.theme.MhjPlayerTheme
import com.mhj.player.core.ui.theme.MhjTheme
import com.mhj.player.feature.music.MusicPlayerScreen
import com.mhj.player.feature.video.VideoPlayerScreen
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModel.factory((application as MhjApp).container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // As soon as the phone allows it, fill the library with the songs and videos on the phone.
        if (result.any { (perm, ok) -> ok && perm != Manifest.permission.POST_NOTIFICATIONS }) viewModel.scanDeviceLibrary()
    }

    /** True while the app is shown as a small floating video window. */
    private val inPip = mutableStateOf(false)
    /** Video is on screen and playing — going home should keep it in a small window. */
    private var pipEligible = false

    private fun pipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(pipEligible)
            .setSeamlessResizeEnabled(true)
            .build()

    fun setPipEligible(on: Boolean) {
        if (on == pipEligible) return
        pipEligible = on
        runCatching { setPictureInPictureParams(pipParams()) }
    }

    fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(this, "این گوشی پنجرهٔ کوچک ندارد", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { enterPictureInPictureMode(pipParams()) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        maybeRequestPermissions()
        if (hasMediaPermission()) viewModel.scanDeviceLibrary()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MhjPlayerTheme(theme = settings.theme) {
                val dark = MhjTheme.colors.isDark
                SideEffect {
                    val insets = WindowCompat.getInsetsController(window, window.decorView)
                    insets.isAppearanceLightStatusBars = !dark
                    insets.isAppearanceLightNavigationBars = !dark
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                }
                PlayerRoot(
                    viewModel = viewModel,
                    inPip = inPip.value,
                    onEnterPip = ::enterPip,
                    onPipEligible = ::setPipEligible,
                )
            }
        }
    }

    private fun hasMediaPermission(): Boolean {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    }

    private fun maybeRequestPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

private const val PAGE_VIDEO = 0
private const val PAGE_MUSIC = 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRoot(
    viewModel: PlayerViewModel,
    inPip: Boolean,
    onEnterPip: () -> Unit,
    onPipEligible: (Boolean) -> Unit,
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val library by viewModel.libraryItems.collectAsStateWithLifecycle()
    val allLibrary by viewModel.allLibraryItems.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.playlistTracks.collectAsStateWithLifecycle()
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val transcript by viewModel.transcriptState.collectAsStateWithLifecycle()
    val savedTranscripts by viewModel.transcriptHistory.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val pagerState = rememberPagerState(initialPage = PAGE_MUSIC, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var topOpen by remember { mutableStateOf(false) }
    var bottomOpen by remember { mutableStateOf(false) }
    var deleteAsk by remember { mutableStateOf<MediaItemModel?>(null) }
    var deleteSelectedAsk by remember { mutableStateOf(false) }
    var renameAsk by remember { mutableStateOf<MediaItemModel?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var addToPlaylistAsk by remember { mutableStateOf<List<MediaItemModel>?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    val hazeState = remember { HazeState() }

    val onMusicPage by remember {
        derivedStateOf { pagerState.currentPage == PAGE_MUSIC }
    }
    val videoActive by remember {
        derivedStateOf { pagerState.currentPage == PAGE_VIDEO }
    }
    // Landscape video owns horizontal drag for seek — do not swipe back to music
    val lockPagerForVideoGestures = videoActive && landscape

    fun persistUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    fun displayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        }.getOrNull()
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let {
            persistUri(it)
            viewModel.importSafTree(it)
        }
    }

    val addMusicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        persistUri(uri)
        viewModel.importAndPlayUri(uri, MediaKind.MUSIC, displayName(uri))
        topOpen = false
        scope.launch { pagerState.animateScrollToPage(PAGE_MUSIC) }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        persistUri(uri)
        viewModel.importAndPlayUri(uri, MediaKind.VIDEO, displayName(uri))
        bottomOpen = false
        topOpen = false
        scope.launch { pagerState.animateScrollToPage(PAGE_VIDEO) }
    }

    // Leaving the app while a video plays keeps it in a small floating window.
    val pipNow = videoActive && playerState.isPlaying && playerState.current?.kind == MediaKind.VIDEO
    LaunchedEffect(pipNow) { onPipEligible(pipNow) }

    LaunchedEffect(playerState.errorMessage) {
        val err = playerState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, "پخش ناموفق: $err", Toast.LENGTH_LONG).show()
    }

    // Leaving music page always dismisses the music audio sheet
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != PAGE_MUSIC) {
                    bottomOpen = false
                    topOpen = false
                }
            }
    }

    BackHandler(
        enabled = transcript.viewerText != null || topOpen || bottomOpen ||
            (pagerState.currentPage != PAGE_MUSIC && !lockPagerForVideoGestures),
    ) {
        when {
            transcript.viewerText != null -> viewModel.dismissTranscriptViewer()
            bottomOpen -> bottomOpen = false
            topOpen -> topOpen = false
            pagerState.currentPage != PAGE_MUSIC && !lockPagerForVideoGestures -> scope.launch {
                pagerState.animateScrollToPage(PAGE_MUSIC)
            }
        }
    }

    if (inPip) {
        VideoPlayerScreen(
            state = playerState,
            player = viewModel.playerOrNull(),
            active = true,
            onPlayPause = viewModel::togglePlayPause,
            onSeek = viewModel::seekTo,
            onSeekBy = viewModel::seekBy,
            onPickVideo = {},
            inPip = true,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onMusicPage) {
                    Modifier.playerEdgeSwipes(
                        onSwipeDownFromTop = {
                            topOpen = true
                            bottomOpen = false
                        },
                        onSwipeUpFromBottom = {
                            bottomOpen = true
                            topOpen = false
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        BoardBackground(hazeState = hazeState)
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !topOpen && !bottomOpen && !lockPagerForVideoGestures,
                beyondViewportPageCount = 0,
            ) { page ->
                when (page) {
                    PAGE_VIDEO -> VideoPlayerScreen(
                        state = playerState,
                        player = viewModel.playerOrNull(),
                        active = videoActive,
                        onPlayPause = viewModel::togglePlayPause,
                        onSeek = viewModel::seekTo,
                        onSeekBy = viewModel::seekBy,
                        onPickVideo = {
                            pickVideoLauncher.launch(arrayOf("video/*"))
                        },
                        onEnterPip = onEnterPip,
                    )
                    else -> MusicPlayerScreen(
                        state = playerState,
                        onPlayPause = viewModel::togglePlayPause,
                        onSeek = viewModel::seekTo,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onShuffle = viewModel::toggleShuffle,
                        onRepeat = viewModel::cycleRepeat,
                        onOpenTopMenu = { topOpen = true },
                        onCast = { CastHelper.openCastChooser(context) },
                        onOpenBottomMenu = {
                            if (onMusicPage) bottomOpen = true
                        },
                        showRemaining = settings.showRemaining,
                        onToggleRemaining = { viewModel.toggleShowRemaining() },
                        isFavorite = isFavorite,
                        onToggleFavorite = viewModel::toggleFavoriteCurrent,
                        speed = settings.playbackSpeed,
                    )
                }
            }

            // Music-only drawers — never shown over video
            if (onMusicPage) {
                TopMenuDrawer(
                    visible = topOpen,
                    settings = settings,
                    playlists = playlists,
                    libraryItems = library,
                    playlistTracks = playlistTracks,
                    activePlaylist = activePlaylist,
                    filter = filter,
                    selectedIds = selectedIds,
                    selectionMode = selectionMode,
                    scanning = scanning,
                    onDismiss = {
                        viewModel.clearSelection()
                        topOpen = false
                    },
                    onToggleTheme = viewModel::toggleTheme,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onOpenPlaylist = viewModel::openPlaylist,
                    onPlayPlaylist = { id ->
                        viewModel.playPlaylist(id)
                        topOpen = false
                        scope.launch { pagerState.animateScrollToPage(PAGE_MUSIC) }
                    },
                    onUpdateFilter = viewModel::updateFilter,
                    onToggleSelect = viewModel::toggleSelect,
                    onEnterSelection = viewModel::enterSelection,
                    onClearSelection = viewModel::clearSelection,
                    onPlayItem = { item ->
                        viewModel.playItem(item)
                        topOpen = false
                        val dest = if (item.kind == MediaKind.VIDEO) PAGE_VIDEO else PAGE_MUSIC
                        scope.launch { pagerState.animateScrollToPage(dest) }
                    },
                    onScanDevice = viewModel::scanDeviceLibrary,
                    onImportFolder = { folderLauncher.launch(null) },
                    onAddMusic = { addMusicLauncher.launch(arrayOf("audio/*")) },
                    onShareItem = { item -> MediaShareHelper.share(context, item) },
                    onDeleteItemAsk = { item -> deleteAsk = item },
                    onRenameItemAsk = { item ->
                        renameAsk = item
                        renameDraft = item.title
                    },
                    onAddItemToPlaylistAsk = { item ->
                        addToPlaylistAsk = listOf(item)
                        newPlaylistName = ""
                    },
                    onAddPlaylistTrackToPlaylistAsk = { item ->
                        addToPlaylistAsk = listOf(item)
                        newPlaylistName = ""
                    },
                    onRemoveFromPlaylist = viewModel::removeFromPlaylist,
                    onReorderPlaylist = viewModel::reorderPlaylist,
                    onSharePlaylistTrack = { item -> MediaShareHelper.share(context, item) },
                    onShareSelected = {
                        MediaShareHelper.shareMany(context, viewModel.selectedMediaItems())
                    },
                    onAddSelectedToPlaylist = {
                        val items = viewModel.selectedMediaItems()
                        if (items.isNotEmpty()) {
                            addToPlaylistAsk = items
                            newPlaylistName = ""
                        }
                    },
                    onDeleteSelectedAsk = { deleteSelectedAsk = true },
                    onOpenFavorites = viewModel::openFavoritesPlaylist,
                    hazeState = hazeState,
                )
                BottomAudioDrawer(
                    visible = bottomOpen,
                    settings = settings,
                    abActive = playerState.abLoopActive,
                    abHalf = playerState.abPointAMs != null && playerState.abPointBMs == null,
                    transcript = TranscriptUi(
                        running = transcript.isRunning,
                        downloading = transcript.isDownloading,
                        progress = transcript.progress,
                        status = transcript.statusFa,
                        error = transcript.errorFa,
                    ),
                    saved = savedTranscripts.map { SavedTranscript(it.path, it.title, it.updatedAt) },
                    actions = remember(viewModel) {
                        AudioActions(
                            onToggleTranscript = {
                                if (viewModel.playerState.value.current == null) {
                                    Toast.makeText(context, "اول یک آهنگ یا ویدیو پخش کنید", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.toggleTranscription()
                                }
                            },
                            onOpenSaved = { viewModel.openSavedTranscript(it.id) },
                            onShareSaved = { t ->
                                viewModel.readSavedTranscript(t.id)?.let { (title, body) -> shareText(context, title, body) }
                            },
                            onDeleteSaved = { viewModel.deleteSavedTranscript(it.id) },
                            onRefreshSaved = viewModel::refreshTranscripts,
                            onDialogue = { viewModel.setDialogueClarity(it) },
                            onNightMode = { viewModel.setNightMode(it) },
                            onSpeed = viewModel::setPlaybackSpeed,
                            onKeepPitch = { viewModel.setKeepNaturalPitch(it) },
                            onAbClick = {
                                val msg = viewModel.tapAbLoop()
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onEqEnabled = { viewModel.setEqEnabled(it) },
                            onEqGain = { i, db -> viewModel.setEqGain(i, db) },
                            onEqPreamp = { viewModel.setEqPreamp(it) },
                            onEqPreset = { viewModel.applyEqPreset(it) },
                            onTimerMinutes = viewModel::setSleepTimerMinutes,
                            onTimerAtEnd = { viewModel.setSleepAtEnd() },
                            onTimerOff = { viewModel.cancelSleep() },
                        )
                    },
                    onDismiss = { bottomOpen = false },
                    hazeState = hazeState,
                )
            }

            TranscriptViewerOverlay(
                visible = transcript.viewerText != null,
                title = transcript.viewerTitle.orEmpty(),
                body = transcript.viewerText.orEmpty(),
                onDismiss = viewModel::dismissTranscriptViewer,
            )

            deleteAsk?.let { item ->
                AlertDialog(
                    onDismissRequest = { deleteAsk = null },
                    title = { Text("حذف فایل") },
                    text = { Text("مطمئنی می‌خوای از دستگاه حذف بشه؟") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                MediaShareHelper.deleteFromDevice(context, item)
                                viewModel.deleteMediaItem(item.id)
                                deleteAsk = null
                            },
                        ) { Text("حذف") }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteAsk = null }) { Text("انصراف") }
                    },
                )
            }

            renameAsk?.let { item ->
                AlertDialog(
                    onDismissRequest = { renameAsk = null },
                    title = { Text("تغییر نام") },
                    text = {
                        OutlinedTextField(
                            value = renameDraft,
                            onValueChange = { renameDraft = it },
                            singleLine = true,
                            label = { Text("نام جدید") },
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = renameDraft.trim()
                                if (name.isNotEmpty()) {
                                    MediaShareHelper.rename(context, item, name)
                                    viewModel.renameMediaItem(item.id, name)
                                }
                                renameAsk = null
                            },
                        ) { Text("ذخیره") }
                    },
                    dismissButton = {
                        TextButton(onClick = { renameAsk = null }) { Text("انصراف") }
                    },
                )
            }

            if (deleteSelectedAsk) {
                AlertDialog(
                    onDismissRequest = { deleteSelectedAsk = false },
                    title = { Text("حذف فایل") },
                    text = { Text("مطمئنی می‌خوای از دستگاه حذف بشه؟") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.selectedMediaItems().forEach { item ->
                                    MediaShareHelper.deleteFromDevice(context, item)
                                    viewModel.deleteMediaItem(item.id)
                                }
                                viewModel.clearSelection()
                                deleteSelectedAsk = false
                            },
                        ) { Text("حذف") }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteSelectedAsk = false }) { Text("انصراف") }
                    },
                )
            }

            val pickItems = addToPlaylistAsk
            val pickerData by produceState(emptyList<PickerPlaylist>(), pickItems, playlists) {
                value = if (pickItems == null) emptyList() else viewModel.pickerPlaylists(pickItems.map { it.id })
            }
            PlaylistPickerSheet(
                visible = pickItems != null,
                subtitle = when {
                    pickItems == null -> ""
                    pickItems.size == 1 -> "«${pickItems[0].title}» — یک پلی‌لیست را بزنید"
                    else -> "${pickItems.size} آهنگ — یک پلی‌لیست را بزنید"
                },
                playlists = pickerData,
                onPick = { pl ->
                    val items = pickItems.orEmpty()
                    items.forEach { viewModel.addMediaToPlaylist(pl.id, it.id) }
                    addToPlaylistAsk = null
                    viewModel.clearSelection()
                    Toast.makeText(context, if (pl.hasAll) "از قبل در «${pl.name}» بود" else "به «${pl.name}» اضافه شد", Toast.LENGTH_SHORT).show()
                },
                onCreate = { name ->
                    val items = pickItems.orEmpty()
                    if (items.isNotEmpty()) viewModel.createPlaylistAndAddAll(name, items.map { it.id })
                    addToPlaylistAsk = null
                    viewModel.clearSelection()
                    Toast.makeText(context, "پلی‌لیست «$name» ساخته شد", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { addToPlaylistAsk = null },
                hazeState = hazeState,
            )
        }
    }
}
