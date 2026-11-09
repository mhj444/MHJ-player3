package com.mhj.player.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhj.player.MhjApp
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.ui.components.BoardBackground
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
    ) { /* Media scan / notifications after grant */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        maybeRequestPermissions()

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
                PlayerRoot(viewModel = viewModel)
            }
        }
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
private fun PlayerRoot(viewModel: PlayerViewModel) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val library by viewModel.libraryItems.collectAsStateWithLifecycle()
    val allLibrary by viewModel.allLibraryItems.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pagerState = rememberPagerState(initialPage = PAGE_MUSIC, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var topOpen by remember { mutableStateOf(false) }
    var bottomOpen by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val onMusicPage by remember {
        derivedStateOf { pagerState.currentPage == PAGE_MUSIC }
    }
    val videoActive by remember {
        derivedStateOf { pagerState.currentPage == PAGE_VIDEO }
    }

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

    LaunchedEffect(allLibrary, playerState.queue.size, playerState.current) {
        if (allLibrary.isNotEmpty() && playerState.queue.isEmpty() && playerState.current == null) {
            viewModel.playDemoMusic()
        }
    }

    LaunchedEffect(playerState.audioSessionId) {
        if (playerState.audioSessionId > 0) {
            viewModel.rebindAudioEffects()
        }
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

    BackHandler(enabled = topOpen || bottomOpen || pagerState.currentPage != PAGE_MUSIC) {
        when {
            bottomOpen -> bottomOpen = false
            topOpen -> topOpen = false
            pagerState.currentPage != PAGE_MUSIC -> scope.launch {
                pagerState.animateScrollToPage(PAGE_MUSIC)
            }
        }
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
                userScrollEnabled = !topOpen && !bottomOpen,
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
                    filter = filter,
                    selectedIds = selectedIds,
                    scanning = scanning,
                    onDismiss = { topOpen = false },
                    onToggleTheme = viewModel::toggleTheme,
                    onCreatePlaylist = viewModel::createPlaylist,
                    onPlayPlaylist = { id ->
                        viewModel.playPlaylist(id)
                        topOpen = false
                        scope.launch { pagerState.animateScrollToPage(PAGE_MUSIC) }
                    },
                    onUpdateFilter = viewModel::updateFilter,
                    onToggleSelect = viewModel::toggleSelect,
                    onClearSelection = viewModel::clearSelection,
                    onRenameSelected = viewModel::renameSelected,
                    onDeleteSelected = viewModel::deleteSelected,
                    onPlayItem = { item ->
                        viewModel.playItem(item)
                        topOpen = false
                        val dest = if (item.kind == MediaKind.VIDEO) PAGE_VIDEO else PAGE_MUSIC
                        scope.launch { pagerState.animateScrollToPage(dest) }
                    },
                    onScanDevice = viewModel::scanDeviceLibrary,
                    onImportFolder = { folderLauncher.launch(null) },
                    onAddMusic = { addMusicLauncher.launch(arrayOf("audio/*")) },
                    hazeState = hazeState,
                )
                BottomAudioDrawer(
                    visible = bottomOpen,
                    settings = settings,
                    abLabel = playerState.abLabel,
                    abActive = playerState.abLoopActive,
                    onDismiss = { bottomOpen = false },
                    onDialogueClarity = viewModel::setDialogueClarity,
                    onNightMode = viewModel::setNightMode,
                    onSpeedChange = viewModel::setPlaybackSpeed,
                    onSpeedNudge = viewModel::nudgeSpeed,
                    onAbClick = {
                        val msg = viewModel.tapAbLoop()
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onEqClick = {
                        val intent = viewModel.openSystemEqualizer(context.packageName)
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                                .onFailure {
                                    Toast.makeText(context, "No system equalizer", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Start playback for EQ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onTimerChange = viewModel::setSleepTimerMinutes,
                    hazeState = hazeState,
                )
            }
        }
    }
}
