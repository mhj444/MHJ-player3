package com.mhj.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhj.player.core.data.datastore.SettingsDataStore
import com.mhj.player.core.data.repository.LibraryRepository
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.playback.engine.AudioEffectsController
import com.mhj.player.core.playback.engine.PlaybackController
import com.mhj.player.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playback: PlaybackController,
    private val settingsStore: SettingsDataStore,
    private val library: LibraryRepository,
) : ViewModel() {

    private val audioEffects = AudioEffectsController()

    val playerState: StateFlow<PlayerUiState> = playback.state
    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val libraryItems: StateFlow<List<MediaItemModel>> = library.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Media3 session id → Android AudioEffect (dialogue clarity / night mode)
        viewModelScope.launch {
            playback.state
                .map { it.audioSessionId }
                .distinctUntilChanged()
                .collect { sessionId -> audioEffects.bindSession(sessionId) }
        }
        viewModelScope.launch {
            combine(
                settingsStore.settings.map { it.dialogueClarity },
                settingsStore.settings.map { it.nightModeAudio },
            ) { dialogue, night -> dialogue to night }
                .distinctUntilChanged()
                .collect { (dialogue, night) ->
                    audioEffects.setDialogueClarity(dialogue)
                    audioEffects.setNightMode(night)
                }
        }
    }

    override fun onCleared() {
        audioEffects.release()
        super.onCleared()
    }
    fun playDemoMusic() {
        viewModelScope.launch {
            val queue = libraryItems.value
                .filter { it.kind == MediaKind.MUSIC }
                .ifEmpty { return@launch }
            playback.awaitConnected()
            playback.play(queue, 0)
        }
    }

    fun playItem(item: MediaItemModel) {
        viewModelScope.launch {
            playback.awaitConnected()
            val queue = libraryItems.value.ifEmpty { listOf(item) }
            val index = queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
            playback.play(if (queue.any { it.id == item.id }) queue else listOf(item), index)
        }
    }

    fun togglePlayPause() = playback.togglePlayPause()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun seekBy(ms: Long) = playback.seekBy(ms)
    fun next() = playback.skipNext()
    fun previous() = playback.skipPrevious()
    fun toggleShuffle() = playback.setShuffle(!playerState.value.shuffle)
    fun cycleRepeat() = playback.cycleRepeat()
    fun playerOrNull() = playback.playerOrNull()

    fun toggleTheme() {
        viewModelScope.launch {
            val next = when (settings.value.theme) {
                AppTheme.MIDNIGHT_GOLD -> AppTheme.PORCELAIN_WHITE
                AppTheme.PORCELAIN_WHITE -> AppTheme.MIDNIGHT_GOLD
            }
            settingsStore.setTheme(next)
        }
    }

    fun setDialogueClarity(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDialogueClarity(enabled) }
    }

    fun setNightMode(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNightModeAudio(enabled) }
    }

    fun bumpSpeed() {
        viewModelScope.launch {
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
            val current = settings.value.playbackSpeed
            val idx = speeds.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
            val next = speeds[(idx + 1).floorMod(speeds.size)]
            settingsStore.setPlaybackSpeed(next)
            playback.setSpeed(next)
        }
    }

    fun cycleSleepTimer() {
        viewModelScope.launch {
            val options = listOf(0, 15, 30, 45, 60)
            val current = settings.value.sleepTimerMinutes
            val idx = options.indexOf(current).let { if (it < 0) 0 else it }
            settingsStore.setSleepTimerMinutes(options[(idx + 1) % options.size])
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(
                        playback = container.playbackController,
                        settingsStore = container.settingsStore,
                        library = container.libraryRepository,
                    ) as T
                }
            }
    }
}

private fun Int.floorMod(other: Int): Int {
    val r = this % other
    return if (r < 0) r + other else r
}
