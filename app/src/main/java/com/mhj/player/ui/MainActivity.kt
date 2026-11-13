package com.mhj.player.ui

import com.mhj.player.core.model.L

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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.mhj.player.core.ui.components.TopMenuDrawer
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

    private val rescanHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val rescan = Runnable { if (hasMediaPermission()) viewModel.scanDeviceLibrary() }
    private var mediaWatcher: android.database.ContentObserver? = null

    override fun onStart() {
        super.onStart()
        // Catch anything downloaded while the app was closed.
        rescanHandler.removeCallbacks(rescan)
        rescanHandler.postDelayed(rescan, 600)
    }

    override fun onDestroy() {
        mediaWatcher?.let { contentResolver.unregisterContentObserver(it) }
        rescanHandler.removeCallbacks(rescan)
        super.onDestroy()
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
            Toast.makeText(this, L.t("این گوشی پنجرهٔ کوچک ندارد"), Toast.LENGTH_SHORT).show()
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
        // New songs or videos saved on the phone (downloads etc.) are added by themselves.
        val watcher = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                rescanHandler.removeCallbacks(rescan)
                rescanHandler.postDelayed(rescan, 4_000)
            }
        }
        contentResolver.registerContentObserver(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, watcher)
        contentResolver.registerContentObserver(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, watcher)
        mediaWatcher = watcher

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
                // Language: Persian = right-to-left, English = left-to-right. Changing it rebuilds the screens.
                com.mhj.player.core.model.L.en = settings.englishUi
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        if (settings.englishUi) androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl,
                ) {
                    androidx.compose.runtime.key(settings.englishUi) {
                        PlayerRoot(
                            viewModel = viewModel,
                            inPip = inPip.value,
                            onEnterPip = ::enterPip,
                            onPipEligible = ::setPipEligible,
                        )
                    }
                }
            }
        }
        openIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openIncoming(intent)
    }

    /** Telegram "Open with" sends a temporary content:// URI, not a saved file. */
    private fun openIncoming(incoming: Intent?) {
        if (incoming == null) return
        val action = incoming.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return
        val uri = incoming.data ?: streamExtra(incoming) ?: return
        val mime = incoming.type ?: contentResolver.getType(uri)
        val name = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val kind = when {
            mime?.startsWith("audio/") == true -> MediaKind.MUSIC
            mime?.startsWith("video/") == true -> MediaKind.VIDEO
            name.isAudioName() -> MediaKind.MUSIC
            else -> MediaKind.VIDEO
        }
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.importAndPlayUri(uri, kind, name)
        incoming.action = null
    }

    private fun streamExtra(incoming: Intent): Uri? {
        val extra = if (Build.VERSION.SDK_INT >= 33) {
            incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return extra ?: incoming.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private fun String?.isAudioName(): Boolean {
        val n = this?.lowercase().orEmpty()
        return listOf(".mp3", ".m4a", ".aac", ".flac", ".wav", ".ogg", ".opus").any { n.endsWith(it) }
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
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val pagerState = rememberPagerState(initialPage = PAGE_MUSIC, pageCount = { 2 })
    val externalOpen by viewModel.externalOpen.collectAsStateWithLifecycle()
    LaunchedEffect(externalOpen) {
        val kind = externalOpen ?: return@LaunchedEffect
        val dest = if (kind == MediaKind.VIDEO) PAGE_VIDEO else PAGE_MUSIC
        pagerState.scrollToPage(dest)
        viewModel.consumeExternalOpen()
    }
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
    SideEffect {
        val activity = context as? android.app.Activity ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val immersive = videoActive && !inPip && playerState.current?.kind == MediaKind.VIDEO
        if (immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
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

    val listenPositions by viewModel.listenPositions.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val backupScope = androidx.compose.runtime.rememberCoroutineScope()
    val backupSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            backupStatus = runCatching {
                val text = viewModel.buildBackup()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                }
                L.t("پشتیبان ذخیره شد. این فایل را به گوشی جدید بفرستید و آنجا «بازیابی از فایل» را بزنید.")
            }.getOrElse { L.pick("ذخیره نشد: ", "Could not save: ") + (it.message ?: "") }
        }
    }
    val backupOpenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupStatus = L.t("در حال بازیابی…")
        backupScope.launch {
            backupStatus = runCatching {
                val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                } ?: ""
                viewModel.restoreBackup(text)
            }.getOrElse { L.pick("بازیابی نشد: ", "Could not restore: ") + (it.message ?: "") }
        }
    }
    // ---------------------------------------------------------------- real file changes
    val fileScope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
    val grantLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val then = pendingGrant
        pendingGrant = null
        if (r.resultCode == android.app.Activity.RESULT_OK) then?.invoke()
    }
    /** Android asks once ("Allow changes to these files?"), then [action] runs. */
    fun withFilePermission(items: List<MediaItemModel>, delete: Boolean, action: () -> Unit) {
        val sender = runCatching {
            if (delete) MediaFileOps.deleteRequest(context, items) else MediaFileOps.writeRequest(context, items)
        }.getOrNull()
        if (sender == null) { action(); return }
        pendingGrant = action
        grantLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
    }
    fun deleteFiles(items: List<MediaItemModel>) {
        if (items.isEmpty()) return
        val (store, other) = items.partition { MediaFileOps.isMediaStore(it) }
        other.forEach { MediaShareHelper.deleteFromDevice(context, it); viewModel.deleteMediaItem(it.id) }
        if (store.isNotEmpty()) withFilePermission(store, delete = true) {
            // Android has deleted them; take them out of the library too.
            store.forEach { viewModel.deleteMediaItem(it.id) }
            Toast.makeText(context, L.pick("${store.size} فایل حذف شد", "${store.size} files deleted"), Toast.LENGTH_SHORT).show()
        }
    }
    var moveAsk by remember { mutableStateOf<List<MediaItemModel>?>(null) }
    var folderRenameAsk by remember { mutableStateOf<Pair<String, List<MediaItemModel>>?>(null) }
    var folderDeleteAsk by remember { mutableStateOf<Pair<String, List<MediaItemModel>>?>(null) }
    fun moveFiles(items: List<MediaItemModel>, dest: (MediaItemModel) -> String?) {
        val store = items.filter { MediaFileOps.isMediaStore(it) }
        if (store.isEmpty()) {
            Toast.makeText(context, L.t("این فایل‌ها را نمی‌شود جابه‌جا کرد"), Toast.LENGTH_SHORT).show(); return
        }
        withFilePermission(store, delete = false) {
            fileScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    store.count { item -> dest(item)?.let { MediaFileOps.move(context, item, it) } == true }
                }
                viewModel.scanDeviceLibrary()
                Toast.makeText(
                    context,
                    if (ok == store.size) L.pick("$ok فایل جابه‌جا شد", "$ok files moved")
                    else L.pick("$ok از ${store.size} فایل جابه‌جا شد (اندروید بعضی پوشه‌ها را برای این نوع فایل اجازه نمی‌دهد)", "$ok of ${store.size} moved (Android doesn't allow some folders for this file type)"),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cur = viewModel.playerState.value.current ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        fileScope.launch {
            val got = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { MediaFileOps.importSubtitle(context, uri, cur.id) }
            if (got == null) {
                Toast.makeText(context, L.t("این فایل زیرنویس خوانده نشد"), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.attachSubtitle(cur.id, got.first.absolutePath, got.second)
                Toast.makeText(context, L.t("زیرنویس اضافه شد"), Toast.LENGTH_SHORT).show()
            }
        }
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
        Toast.makeText(context, L.pick("پخش ناموفق: $err", "Playback failed: $err"), Toast.LENGTH_LONG).show()
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
        enabled = topOpen || bottomOpen ||
            (pagerState.currentPage != PAGE_MUSIC && !lockPagerForVideoGestures),
    ) {
        when {
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
            onScrubStart = viewModel::scrubStart,
            onScrub = viewModel::scrubTo,
            onScrubEnd = viewModel::scrubEnd,
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
                        onScrubStart = viewModel::scrubStart,
                        onScrub = viewModel::scrubTo,
                        onScrubEnd = viewModel::scrubEnd,
                        onPickVideo = {
                            pickVideoLauncher.launch(arrayOf("video/*"))
                        },
                        onEnterPip = onEnterPip,
                        onPickSubtitle = { subtitleLauncher.launch(arrayOf("*/*")) },
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
                        onSeekBy = viewModel::seekBy,
                        onBookmark = {
                            val at = viewModel.addBookmarkNow()
                            if (at != null) Toast.makeText(context, L.pick("نشانک ذخیره شد: ", "Bookmark saved: ") + com.mhj.player.core.ui.components.Fa.time(at), Toast.LENGTH_SHORT).show()
                        },
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
                    listenPositions = listenPositions,
                    onPlayFolder = { list, item ->
                        viewModel.playFolder(list, item)
                        topOpen = false
                        val dest = if (item.kind == MediaKind.VIDEO) PAGE_VIDEO else PAGE_MUSIC
                        scope.launch { pagerState.animateScrollToPage(dest) }
                    },
                    onBackupSave = { backupSaveLauncher.launch("MHJ-Backup.json") },
                    onBackupRestore = { backupOpenLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                    backupStatus = backupStatus,
                    onMoveAsk = { moveAsk = it },
                    onRenameFolderAsk = { n, f -> folderRenameAsk = n to f },
                    onDeleteFolderAsk = { n, f -> folderDeleteAsk = n to f },
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onSetEnglish = { viewModel.setEnglishUi(it) },
                    onSetTheme = { viewModel.setTheme(it) },
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onReorderPlaylists = viewModel::reorderPlaylists,
                    backEnabled = addToPlaylistAsk == null,
                    hazeState = hazeState,
                )
                BottomAudioDrawer(
                    visible = bottomOpen,
                    settings = settings,
                    abActive = playerState.abLoopActive,
                    abHalf = playerState.abPointAMs != null && playerState.abPointBMs == null,
                    bookmarks = bookmarks,
                    actions = remember(viewModel) {
                        AudioActions(
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
                            onOpenBookmark = { b ->
                                if (!viewModel.openBookmark(b)) Toast.makeText(context, L.t("فایل این نشانک در گوشی نیست"), Toast.LENGTH_SHORT).show()
                            },
                            onDeleteBookmark = { viewModel.deleteBookmark(it.id) },
                            onTimerMinutes = viewModel::setSleepTimerMinutes,
                            onTimerAtEnd = { viewModel.setSleepAtEnd() },
                            onTimerOff = { viewModel.cancelSleep() },
                        )
                    },
                    onDismiss = { bottomOpen = false },
                    hazeState = hazeState,
                )
            }


            deleteAsk?.let { item ->
                AlertDialog(
                    onDismissRequest = { deleteAsk = null },
                    title = { Text(L.t("حذف فایل")) },
                    text = { Text(L.t("مطمئنی می‌خوای از دستگاه حذف بشه؟")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteAsk = null
                                deleteFiles(listOf(item))
                            },
                        ) { Text(L.t("حذف")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteAsk = null }) { Text(L.t("انصراف")) }
                    },
                )
            }

            renameAsk?.let { item ->
                AlertDialog(
                    onDismissRequest = { renameAsk = null },
                    title = { Text(L.t("تغییر نام")) },
                    text = {
                        OutlinedTextField(
                            value = renameDraft,
                            onValueChange = { renameDraft = it },
                            singleLine = true,
                            label = { Text(L.t("نام جدید")) },
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = renameDraft.trim()
                                renameAsk = null
                                if (name.isNotEmpty()) {
                                    if (MediaFileOps.isMediaStore(item)) {
                                        withFilePermission(listOf(item), delete = false) {
                                            fileScope.launch {
                                                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { MediaFileOps.rename(context, item, name) }
                                                viewModel.renameMediaItem(item.id, name)
                                                if (!ok) Toast.makeText(context, L.t("نام فایل در گوشی عوض نشد"), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        MediaShareHelper.rename(context, item, name)
                                        viewModel.renameMediaItem(item.id, name)
                                    }
                                }
                            },
                        ) { Text(L.t("ذخیره")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { renameAsk = null }) { Text(L.t("انصراف")) }
                    },
                )
            }

            if (deleteSelectedAsk) {
                AlertDialog(
                    onDismissRequest = { deleteSelectedAsk = false },
                    title = { Text(L.t("حذف فایل")) },
                    text = { Text(L.t("مطمئنی می‌خوای از دستگاه حذف بشه؟")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val sel = viewModel.selectedMediaItems()
                                viewModel.clearSelection()
                                deleteSelectedAsk = false
                                deleteFiles(sel)
                            },
                        ) { Text(L.t("حذف")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteSelectedAsk = false }) { Text(L.t("انصراف")) }
                    },
                )
            }

            moveAsk?.let { items ->
                MoveDialog(items.size, onDismiss = { moveAsk = null }) { folder ->
                    moveAsk = null
                    viewModel.clearSelection()
                    moveFiles(items) { folder }
                }
            }
            folderRenameAsk?.let { (name, files) ->
                var draft by remember(name) { mutableStateOf(name) }
                AlertDialog(
                    onDismissRequest = { folderRenameAsk = null },
                    title = { Text(L.t("تغییر نام پوشه")) },
                    text = { OutlinedTextField(draft, { draft = it }, singleLine = true, label = { Text(L.t("نام جدید")) }) },
                    confirmButton = {
                        TextButton(onClick = {
                            folderRenameAsk = null
                            val n = draft.trim()
                            if (n.isNotEmpty() && n != name) moveFiles(files) { item ->
                                MediaFileOps.relativePath(context, item)?.let { MediaFileOps.renamedFolder(it, n) }
                            }
                        }) { Text(L.t("ذخیره")) }
                    },
                    dismissButton = { TextButton(onClick = { folderRenameAsk = null }) { Text(L.t("انصراف")) } },
                )
            }
            folderDeleteAsk?.let { (name, files) ->
                AlertDialog(
                    onDismissRequest = { folderDeleteAsk = null },
                    title = { Text(L.t("حذف پوشه از دستگاه")) },
                    text = { Text(L.pick("همهٔ ${files.size} فایل پوشهٔ «$name» از گوشی پاک شوند؟", "Delete all ${files.size} files in “$name” from the phone?")) },
                    confirmButton = {
                        TextButton(onClick = { folderDeleteAsk = null; deleteFiles(files) }) { Text(L.t("حذف"), color = androidx.compose.ui.graphics.Color(0xFFE57373)) }
                    },
                    dismissButton = { TextButton(onClick = { folderDeleteAsk = null }) { Text(L.t("انصراف")) } },
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
                    pickItems.size == 1 -> L.pick("«${pickItems[0].title}» — یک پلی‌لیست را بزنید", "“${pickItems[0].title}” — tap a playlist")
                    else -> L.pick("${pickItems.size} آهنگ — یک پلی‌لیست را بزنید", "${pickItems.size} songs — tap a playlist")
                },
                playlists = pickerData,
                onPick = { pl ->
                    val items = pickItems.orEmpty()
                    viewModel.addManyToPlaylist(pl.id, items.map { it.id })
                    addToPlaylistAsk = null
                    viewModel.clearSelection()
                    Toast.makeText(context, if (pl.hasAll) L.pick("از قبل در «${pl.name}» بود", "Already in “${pl.name}”") else L.pick("به «${pl.name}» اضافه شد", "Added to “${pl.name}”"), Toast.LENGTH_SHORT).show()
                },
                onCreate = { name ->
                    val items = pickItems.orEmpty()
                    if (items.isNotEmpty()) viewModel.createPlaylistAndAddAll(name, items.map { it.id })
                    addToPlaylistAsk = null
                    viewModel.clearSelection()
                    Toast.makeText(context, L.pick("پلی‌لیست «$name» ساخته شد", "Playlist “$name” created"), Toast.LENGTH_SHORT).show()
                },
                onDismiss = { addToPlaylistAsk = null },
                hazeState = hazeState,
            )
        }
    }
}


/** Pick where to move files: an existing folder on the phone, or a new one. */
@Composable
private fun MoveDialog(count: Int, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val folders by androidx.compose.runtime.produceState(emptyList<String>()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { MediaFileOps.allFolders(context) }
    }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L.pick("انتقال $count فایل به…", "Move $count files to…")) },
        text = {
            Column {
                OutlinedTextField(
                    newName, { newName = it }, singleLine = true,
                    label = { Text(L.t("پوشهٔ تازه در Music")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(folders.size) { i ->
                        val f = folders[i]
                        Column(
                            Modifier.fillMaxWidth().clickable { onPick(f) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(f.trimEnd('/').substringAfterLast('/'), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text(f, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = newName.trim().trim('/')
                if (n.isNotEmpty()) onPick("Music/$n/")
            }) { Text(L.t("ساختن و انتقال")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L.t("انصراف")) } },
    )
}
