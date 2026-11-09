package com.mhj.player.ui

import com.mhj.player.core.model.L

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
import com.mhj.player.core.playback.engine.PlaybackController
import com.mhj.player.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playback: PlaybackController,
    private val settingsStore: SettingsDataStore,
    private val library: LibraryRepository,
    private val scanner: MediaLibraryScanner,
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = playback.state
    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val libraryFilter = MutableStateFlow(LibraryFilter())
    val filter: StateFlow<LibraryFilter> = libraryFilter

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryItems: StateFlow<List<MediaItemModel>> = libraryFilter
        .flatMapLatest { library.observeFiltered(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allLibraryItems: StateFlow<List<MediaItemModel>> = library.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Built-in lists first, then your own in the order you dragged them into. */
    val playlists: StateFlow<List<PlaylistModel>> = combine(library.observePlaylists(), settingsStore.playlistOrder) { list, order ->
        val pos = order.withIndex().associate { (i, id) -> id to i }
        val (smart, mine) = list.partition { it.isSmart }
        smart + mine.sortedWith(compareBy<PlaylistModel> { pos[it.id] ?: Int.MAX_VALUE }.thenByDescending { it.createdAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun renamePlaylist(id: Long, name: String) {
        val n = name.trim().ifEmpty { return }
        viewModelScope.launch { library.renamePlaylist(id, n) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { library.deletePlaylist(id) }
    }

    fun reorderPlaylists(ids: List<Long>) {
        viewModelScope.launch { settingsStore.setPlaylistOrder(ids) }
    }

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

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favoritesPlaylistId: StateFlow<Long?> = playlists
        .map { list ->
            list.firstOrNull { !it.isSmart && it.name.equals("Favorites", ignoreCase = true) }?.id
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteIds: StateFlow<Set<String>> = favoritesPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else library.observePlaylistMedia(id)
        }
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val isCurrentFavorite: StateFlow<Boolean> = combine(playerState, favoriteIds) { state, ids ->
        state.current?.id?.let { it in ids } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            library.ensureFavoritesPlaylistId()
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

    // ---- Sound (applied by the playback service, so it keeps working with the screen off) ----

    fun setEqGain(index: Int, db: Float) = viewModelScope.launch {
        settingsStore.update { st ->
            val g = st.eqGains.toMutableList()
            if (index in g.indices) g[index] = db.coerceIn(-com.mhj.player.core.model.Eq.MAX_DB, com.mhj.player.core.model.Eq.MAX_DB)
            st.copy(eqGains = g, eqPreset = "custom")
        }
    }

    fun setEqPreamp(db: Float) = viewModelScope.launch { settingsStore.update { it.copy(eqPreamp = db.coerceIn(-12f, 6f)) } }

    fun applyEqPreset(id: String) = viewModelScope.launch {
        val p = com.mhj.player.core.model.Eq.PRESETS.firstOrNull { it.id == id } ?: return@launch
        settingsStore.update { it.copy(eqGains = p.gains, eqPreset = p.id, eqEnabled = true) }
    }

    fun setEqEnabled(on: Boolean) = viewModelScope.launch { settingsStore.update { it.copy(eqEnabled = on) } }

    fun setDialogueClarity(on: Boolean) = viewModelScope.launch { settingsStore.setDialogueClarity(on) }

    fun setKeepNaturalPitch(on: Boolean) = viewModelScope.launch { settingsStore.setKeepNaturalPitch(on) }

    fun setEnglishUi(on: Boolean) = viewModelScope.launch { settingsStore.update { it.copy(englishUi = on) } }

    fun toggleShowRemaining() = viewModelScope.launch { settingsStore.update { it.copy(showRemaining = !it.showRemaining) } }

    fun toggleFavoriteCurrent() {
        val id = playerState.value.current?.id ?: return
        viewModelScope.launch {
            library.ensureFavoritesPlaylistId()
            library.toggleFavorite(id)
        }
    }

    fun openFavoritesPlaylist() {
        viewModelScope.launch {
            val id = library.ensureFavoritesPlaylistId()
            activePlaylistId.value = id
        }
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

    fun setNightMode(enabled: Boolean) = viewModelScope.launch { settingsStore.setNightModeAudio(enabled) }

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
            // Snap to 0.05 steps so the dial lands on round numbers.
            val clamped = (Math.round(speed.coerceIn(0.25f, 3.0f) * 20f) / 20f)
            settingsStore.setPlaybackSpeed(clamped)
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

    /** 0 = off; otherwise playback fades out and pauses after this many minutes. */
    fun setSleepTimerMinutes(minutes: Int) {
        viewModelScope.launch { settingsStore.setSleepTimerMinutes(minutes.coerceIn(0, 180)) }
    }

    fun setSleepAtEnd() = viewModelScope.launch { settingsStore.setSleepAtEnd(true) }
    fun cancelSleep() = viewModelScope.launch { settingsStore.clearSleep() }

    fun updateFilter(transform: (LibraryFilter) -> LibraryFilter) {
        libraryFilter.value = transform(libraryFilter.value)
    }

    fun enterSelection(id: String) {
        _selectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelect(id: String) {
        if (!_selectionMode.value) {
            enterSelection(id)
            return
        }
        _selectedIds.value = _selectedIds.value.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
        if (_selectedIds.value.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectionMode.value = false
    }

    fun selectedMediaItems(): List<MediaItemModel> {
        val ids = _selectedIds.value
        return allLibraryItems.value.filter { it.id in ids }
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
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            runCatching {
                val scanned = scanner.scanMediaStore()
                // Drop phone files that are gone (or are no longer counted as media).
                val keep = scanned.map { it.id }.toSet()
                val stale = allLibraryItems.value.map { it.id }
                    .ifEmpty { library.observeMedia().first().map { it.id } }
                    .filter { (it.startsWith("music_") || it.startsWith("video_")) && it !in keep }
                if (stale.isNotEmpty() && scanned.isNotEmpty()) library.deleteMedia(stale)
                if (scanned.isNotEmpty()) library.upsertMedia(scanned)
                library.refreshSmartPlaylists()
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

    /** Your own playlists for the "add to playlist" panel, with song counts; Favorites first. */
    suspend fun pickerPlaylists(mediaIds: List<String>): List<com.mhj.player.core.ui.components.PickerPlaylist> =
        playlists.value.filter { !it.isSmart }.map { pl ->
            val songs = library.getPlaylistMedia(pl.id).map { it.id }.toSet()
            val fav = pl.name.equals("Favorites", ignoreCase = true)
            com.mhj.player.core.ui.components.PickerPlaylist(
                id = pl.id,
                name = if (fav) L.t("علاقه‌مندی‌ها") else pl.name,
                count = songs.size,
                hasAll = mediaIds.isNotEmpty() && mediaIds.all { it in songs },
                isFavorites = fav,
            )
        }.sortedByDescending { it.isFavorites }

    fun createPlaylistAndAdd(name: String, mediaId: String) {
        createPlaylistAndAddAll(name, listOf(mediaId))
    }

    fun createPlaylistAndAddAll(name: String, mediaIds: List<String>) {
        viewModelScope.launch {
            val trimmed = name.trim().ifEmpty { return@launch }
            val id = library.createPlaylist(trimmed)
            mediaIds.distinct().forEachIndexed { index, mediaId ->
                library.addToPlaylist(id, mediaId, index)
            }
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
                    ) as T
                }
            }
    }
}

private fun Int.floorMod(other: Int): Int {
    val r = this % other
    return if (r < 0) r + other else r
}
