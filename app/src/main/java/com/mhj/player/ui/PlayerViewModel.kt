package com.mhj.player.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhj.player.core.data.datastore.SettingsDataStore
import com.mhj.player.core.data.library.MediaLibraryScanner
import com.mhj.player.core.data.repository.LibraryRepository
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.AppTheme
import com.mhj.player.core.model.LibraryFilter
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlayerUiState
import com.mhj.player.core.model.PlaylistModel
import com.mhj.player.core.playback.engine.AudioEffectsController
import com.mhj.player.core.playback.engine.PlaybackController
import com.mhj.player.core.playback.transcript.TranscriptionController
import com.mhj.player.core.playback.transcript.TranscriptionUiState
import com.mhj.player.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playback: PlaybackController,
    private val settingsStore: SettingsDataStore,
    private val library: LibraryRepository,
    private val scanner: MediaLibraryScanner,
    private val transcription: TranscriptionController,
) : ViewModel() {

    private val audioEffects = AudioEffectsController()

    val playerState: StateFlow<PlayerUiState> = playback.state
    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val transcriptState: StateFlow<TranscriptionUiState> = transcription.state

    private val libraryFilter = MutableStateFlow(LibraryFilter())
    val filter: StateFlow<LibraryFilter> = libraryFilter

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryItems: StateFlow<List<MediaItemModel>> = libraryFilter
        .flatMapLatest { library.observeFiltered(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allLibraryItems: StateFlow<List<MediaItemModel>> = library.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistModel>> = library.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activePlaylistId = MutableStateFlow<Long?>(null)

    val activePlaylist: StateFlow<PlaylistModel?> = combine(activePlaylistId, playlists) { id, list ->
        id?.let { pid -> list.firstOrNull { it.id == pid } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistTracks: StateFlow<List<MediaItemModel>> = activePlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else library.observePlaylistMedia(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    init {
        viewModelScope.launch {
            playback.state
                .map { it.audioSessionId }
                .distinctUntilChanged()
                .collect { sessionId -> audioEffects.bindSession(sessionId) }
        }
        viewModelScope.launch {
            settingsStore.settings
                .map { it.nightModeAudio }
                .distinctUntilChanged()
                .collect { night -> audioEffects.setNightMode(night) }
        }
        viewModelScope.launch {
            // Increment play count when the current media id changes while playing
            var lastId: String? = null
            playback.state.collect { state ->
                val id = state.current?.id
                if (id != null && id != lastId && state.isPlaying) {
                    lastId = id
                    library.recordPlay(id)
                    library.refreshSmartPlaylists()
                }
            }
        }
        viewModelScope.launch {
            // Keep activePlaylist in sync when playlists refresh
            playlists.collect { list ->
                val id = activePlaylistId.value ?: return@collect
                if (list.none { it.id == id }) activePlaylistId.value = null
            }
        }
    }

    override fun onCleared() {
        audioEffects.release()
        super.onCleared()
    }

    fun playDemoMusic() {
        viewModelScope.launch {
            val queue = allLibraryItems.value
                .filter { it.kind == MediaKind.MUSIC }
                .ifEmpty { return@launch }
            playback.awaitConnected()
            playback.play(queue, 0)
        }
    }

    fun playItem(item: MediaItemModel) {
        viewModelScope.launch {
            playback.awaitConnected()
            val queue = allLibraryItems.value.ifEmpty { listOf(item) }
            val index = queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
            playback.play(if (queue.any { it.id == item.id }) queue else listOf(item), index)
            library.recordPlay(item.id)
            library.refreshSmartPlaylists()
        }
    }

    fun playQueue(items: List<MediaItemModel>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            playback.awaitConnected()
            playback.play(items, startIndex.coerceIn(0, items.lastIndex))
            items.getOrNull(startIndex)?.let { library.recordPlay(it.id) }
            library.refreshSmartPlaylists()
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
    fun audioSessionId(): Int = playback.audioSessionId().takeIf { it > 0 }
        ?: playerState.value.audioSessionId

    fun tapAbLoop(): String = playback.tapAbLoop()

    /**
     * Bind the live audio session (starting playback if needed) and return
     * OPEN_SESSION broadcast + DISPLAY panel intents with EXTRA_AUDIO_SESSION set.
     */
    suspend fun prepareEqualizerIntents(packageName: String): List<Intent> {
        var session = audioSessionId()
        if (session <= 0) {
            playback.awaitConnected()
            if (!playerState.value.isPlaying && playerState.value.current != null) {
                playback.togglePlayPause()
            } else if (playerState.value.current == null) {
                val queue = allLibraryItems.value.filter { it.kind == MediaKind.MUSIC }
                if (queue.isNotEmpty()) {
                    playback.play(queue, 0)
                }
            }
            // Brief wait for ExoPlayer to publish a session id
            repeat(20) {
                kotlinx.coroutines.delay(50)
                session = audioSessionId()
                if (session > 0) return@repeat
            }
            session = audioSessionId()
        }
        if (session <= 0) return emptyList()
        audioEffects.bindSession(session)
        return audioEffects.buildEqualizerIntents(packageName)
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val next = when (settings.value.theme) {
                AppTheme.MIDNIGHT_GOLD -> AppTheme.PORCELAIN_WHITE
                AppTheme.PORCELAIN_WHITE -> AppTheme.MIDNIGHT_GOLD
            }
            settingsStore.setTheme(next)
        }
    }

    fun setNightMode(enabled: Boolean) {
        viewModelScope.launch {
            audioEffects.bindSession(audioSessionId())
            audioEffects.setNightMode(enabled)
            settingsStore.setNightModeAudio(enabled)
        }
    }

    /** Re-attach AudioEffect chain to the live ExoPlayer session without rewriting prefs. */
    fun rebindAudioEffects() {
        val session = audioSessionId()
        if (session <= 0) return
        audioEffects.bindSession(session)
        audioEffects.setNightMode(settings.value.nightModeAudio)
    }

    fun toggleTranscription() {
        val current = playerState.value.current ?: return
        if (current.uri.isBlank()) return
        transcription.toggleForMedia(current.id, current.title, current.uri)
    }

    fun openTranscriptFolder(): Boolean {
        val current = playerState.value.current
        return transcription.openViewerFor(current?.id, current?.title)
    }

    fun dismissTranscriptViewer() = transcription.dismissViewer()

    fun bumpSpeed() {
        viewModelScope.launch {
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)
            val current = settings.value.playbackSpeed
            val idx = speeds.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
            val next = speeds[(idx + 1).floorMod(speeds.size)]
            setPlaybackSpeed(next)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            val clamped = speed.coerceIn(0.25f, 3.0f)
            settingsStore.setPlaybackSpeed(clamped)
            playback.setSpeed(clamped)
        }
    }

    fun nudgeSpeed(delta: Float) {
        setPlaybackSpeed(settings.value.playbackSpeed + delta)
    }

    fun cycleSleepTimer() {
        viewModelScope.launch {
            val options = listOf(0, 15, 30, 45, 60)
            val current = settings.value.sleepTimerMinutes
            val idx = options.indexOf(current).let { if (it < 0) 0 else it }
            setSleepTimerMinutes(options[(idx + 1) % options.size])
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setSleepTimerMinutes(minutes.coerceIn(0, 120))
        }
    }

    fun updateFilter(transform: (LibraryFilter) -> LibraryFilter) {
        libraryFilter.value = transform(libraryFilter.value)
    }

    fun toggleSelect(id: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun renameSelected(title: String) {
        val id = _selectedIds.value.singleOrNull() ?: return
        viewModelScope.launch {
            library.renameMedia(id, title)
            clearSelection()
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            library.deleteMedia(ids)
            clearSelection()
            library.refreshSmartPlaylists()
        }
    }

    fun scanDeviceLibrary() {
        viewModelScope.launch {
            _scanning.value = true
            runCatching {
                val scanned = scanner.scanMediaStore()
                if (scanned.isNotEmpty()) {
                    library.upsertMedia(scanned)
                    library.refreshSmartPlaylists()
                }
            }
            _scanning.value = false
        }
    }

    fun importSafTree(uri: Uri) {
        viewModelScope.launch {
            _scanning.value = true
            runCatching {
                val scanned = scanner.scanSafTree(uri)
                if (scanned.isNotEmpty()) {
                    library.upsertMedia(scanned)
                    library.refreshSmartPlaylists()
                }
            }
            _scanning.value = false
        }
    }

    /** Single-file SAF pick for music or video. */
    fun importAndPlayUri(uri: Uri, kind: MediaKind, displayName: String? = null) {
        viewModelScope.launch {
            val name = displayName?.substringBeforeLast('.')?.ifBlank { null }
                ?: uri.lastPathSegment?.substringAfterLast(':')?.substringBeforeLast('.')
                ?: if (kind == MediaKind.VIDEO) "Video" else "Track"
            val format = displayName?.substringAfterLast('.', "")?.uppercase().orEmpty()
                .ifEmpty {
                    uri.lastPathSegment?.substringAfterLast('.', "")?.uppercase().orEmpty()
                }
            val item = MediaItemModel(
                id = "saf_file_${uri.toString().hashCode()}",
                uri = uri.toString(),
                title = name,
                artist = "Local file",
                album = "",
                durationMs = 0L,
                kind = kind,
                folder = "Imported",
                format = format,
                dateAddedMs = System.currentTimeMillis(),
            )
            library.upsertMedia(item)
            library.refreshSmartPlaylists()
            playback.awaitConnected()
            playback.play(listOf(item), 0)
            library.recordPlay(item.id)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim().ifEmpty { return@launch }
            library.createPlaylist(trimmed)
        }
    }

    fun openPlaylist(playlistId: Long) {
        viewModelScope.launch {
            library.refreshSmartPlaylists()
            activePlaylistId.value = playlistId
        }
    }

    fun removeFromPlaylist(playlistId: Long, mediaId: String) {
        viewModelScope.launch {
            library.removeFromPlaylist(playlistId, mediaId)
        }
    }

    fun reorderPlaylist(playlistId: Long, orderedMediaIds: List<String>) {
        viewModelScope.launch {
            library.reorderPlaylist(playlistId, orderedMediaIds)
        }
    }

    fun addMediaToPlaylist(playlistId: Long, mediaId: String) {
        viewModelScope.launch {
            val existing = library.getPlaylistMedia(playlistId)
            if (existing.any { it.id == mediaId }) return@launch
            library.addToPlaylist(playlistId, mediaId, existing.size)
        }
    }

    fun createPlaylistAndAdd(name: String, mediaId: String) {
        viewModelScope.launch {
            val trimmed = name.trim().ifEmpty { return@launch }
            val id = library.createPlaylist(trimmed)
            library.addToPlaylist(id, mediaId, 0)
        }
    }

    fun renameMediaItem(id: String, title: String) {
        viewModelScope.launch {
            library.renameMedia(id, title)
        }
    }

    fun deleteMediaItem(id: String) {
        viewModelScope.launch {
            library.deleteMedia(listOf(id))
            library.refreshSmartPlaylists()
        }
    }

    fun playPlaylist(playlistId: Long) {
        viewModelScope.launch {
            library.refreshSmartPlaylists()
            val queue = library.getPlaylistMedia(playlistId)
            if (queue.isEmpty()) return@launch
            playback.awaitConnected()
            playback.play(queue, 0)
            library.recordPlay(queue.first().id)
            library.refreshSmartPlaylists()
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
                        scanner = container.mediaScanner,
                        transcription = container.transcription,
                    ) as T
                }
            }
    }
}

private fun Int.floorMod(other: Int): Int {
    val r = this % other
    return if (r < 0) r + other else r
}
