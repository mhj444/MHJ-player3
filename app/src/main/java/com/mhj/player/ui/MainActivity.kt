package com.mhj.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhj.player.MhjApp
import com.mhj.player.core.ui.components.AmbientBackground
import com.mhj.player.core.ui.components.BottomAudioDrawer
import com.mhj.player.core.ui.components.TopMenuDrawer
import com.mhj.player.core.ui.gestures.playerEdgeSwipes
import com.mhj.player.core.ui.theme.MhjPlayerTheme
import com.mhj.player.core.ui.theme.MhjTheme
import com.mhj.player.feature.music.MusicPlayerScreen
import com.mhj.player.feature.video.VideoPlayerScreen

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModel.factory((application as MhjApp).container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Media3 session notification works after grant */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        maybeRequestNotificationPermission()

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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRoot(viewModel: PlayerViewModel) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val library by viewModel.libraryItems.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var topOpen by remember { mutableStateOf(false) }
    var bottomOpen by remember { mutableStateOf(false) }

    // Phase 1 smoke: if nothing was restored into the session, load the demo music queue.
    LaunchedEffect(library, playerState.queue.size, playerState.current) {
        if (library.isNotEmpty() && playerState.queue.isEmpty() && playerState.current == null) {
            viewModel.playDemoMusic()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .playerEdgeSwipes(
                onSwipeDownFromTop = {
                    topOpen = true
                    bottomOpen = false
                },
                onSwipeUpFromBottom = {
                    bottomOpen = true
                    topOpen = false
                },
            ),
    ) {
        AmbientBackground()
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !topOpen && !bottomOpen,
        ) { page ->
            when (page) {
                0 -> MusicPlayerScreen(
                    state = playerState,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::cycleRepeat,
                    onOpenTopMenu = { topOpen = true },
                    onOpenBottomMenu = { bottomOpen = true },
                )
                else -> VideoPlayerScreen(
                    state = playerState,
                    player = viewModel.playerOrNull(),
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSeekBy = viewModel::seekBy,
                    onOpenBottomMenu = { bottomOpen = true },
                )
            }
        }

        TopMenuDrawer(
            visible = topOpen,
            settings = settings,
            onDismiss = { topOpen = false },
            onToggleTheme = viewModel::toggleTheme,
        )
        BottomAudioDrawer(
            visible = bottomOpen,
            settings = settings,
            onDismiss = { bottomOpen = false },
            onDialogueClarity = viewModel::setDialogueClarity,
            onNightMode = viewModel::setNightMode,
            onSpeedClick = viewModel::bumpSpeed,
            onEqClick = { /* DSP Phase 3 hook */ },
            onTimerClick = viewModel::cycleSleepTimer,
        )
        }
    }
}
