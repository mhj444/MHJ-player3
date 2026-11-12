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
import com.mhj.player.core.model.Bookmark
import com.mhj.player.core.data.memory.ListenMemory
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

    // ---------------------------------------------------------------- listening memory

    /** Where each long file was left (ms), or ListenMemory.FINISHED. */
    val listenPositions: StateFlow<Map<String, Long>> = ListenMemory.positions
    val bookmarks: StateFlow<List<Bookmark>> = ListenMemory.bookmarks

    /** Saves the current moment as a bookmark; returns its time, or null when nothing plays. */
    fun addBookmarkNow(): Long? {
        val st = playerState.value
        val cur = st.current ?: return null
        val pos = playback.playerOrNull()?.currentPosition ?: st.positionMs
        ListenMemory.addBookmark(cur.id, cur.title, pos)
        return pos
    }

    fun deleteBookmark(id: Long) = ListenMemory.removeBookmark(id)

    /** Remembers the subtitle for this video and shows it now. */
    fun attachSubtitle(mediaId: String, path: String, mime: String) {
        ListenMemory.setSubtitle(mediaId, path, mime)
        playback.attachSubtitle(path, mime)
    }

    /** Opens the bookmark's file (from the library) at its moment. */
    fun openBookmark(b: Bookmark): Boolean {
        if (playerState.value.current?.id == b.mediaId) {
            playback.seekTo(b.positionMs)
            if (!playerState.value.isPlaying) playback.togglePlayPause()
            return true
        }
        val item = allLibraryItems.value.firstOrNull { it.id == b.mediaId } ?: return false
        viewModelScope.launch {
            playback.awaitConnected()
            playback.play(listOf(item), 0, b.positionMs)
        }
        return true
    }

    /** Plays a whole folder in name order, from [start]. */
    fun playFolder(items: List<MediaItemModel>, start: MediaItemModel) {
        val idx = items.indexOfFirst { it.id == start.id }.coerceAtLeast(0)
        playQueue(items, idx)
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

    fun setTheme(theme: AppTheme) = viewModelScope.launch { settingsStore.setTheme(theme) }

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
                AppTheme.PORCELAIN_WHITE -> AppTheme.MIDNIGHT_GOLD
                else -> AppTheme.PORCELAIN_WHITE
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
                    // Only kinds the scan could actually see (e.g. no video permission → leave videos alone).
                    .filter { id ->
                        val kindSeen = (id.startsWith("music_") && scanned.any { it.id.startsWith("music_") }) ||
                            (id.startsWith("video_") && scanned.any { it.id.startsWith("video_") })
                        kindSeen && id !in keep
                    }
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

    /** Adds several songs at once, one after another, keeping their order. */
    fun addManyToPlaylist(playlistId: Long, mediaIds: List<String>) {
        viewModelScope.launch {
            val existing = library.getPlaylistMedia(playlistId).map { it.id }.toMutableList()
            mediaIds.distinct().filter { it !in existing }.forEach { id ->
                library.addToPlaylist(playlistId, id, existing.size)
                existing += id
            }
        }
    }

    // ---------------------------------------------------------------- backup

    private fun trackJson(m: MediaItemModel) = org.json.JSONObject()
        .put("title", m.title).put("artist", m.artist).put("album", m.album)
        .put("durationMs", m.durationMs).put("folder", m.folder).put("format", m.format)
        .put("kind", m.kind.name)

    /** Everything worth moving to another phone, as text (JSON). */
    suspend fun buildBackup(): String {
        val lib = allLibraryItems.value.ifEmpty { library.observeMedia().first() }
        val byId = lib.associateBy { it.id }
        val root = org.json.JSONObject().put("app", "MHJ Player").put("version", 1).put("createdAt", System.currentTimeMillis())
        val st = settings.value
        root.put("settings", org.json.JSONObject()
            .put("theme", st.theme.name).put("englishUi", st.englishUi).put("keepNaturalPitch", st.keepNaturalPitch)
            .put("playbackSpeed", st.playbackSpeed.toDouble()).put("dialogueClarity", st.dialogueClarity)
            .put("nightModeAudio", st.nightModeAudio).put("eqEnabled", st.eqEnabled).put("eqPreamp", st.eqPreamp.toDouble())
            .put("eqPreset", st.eqPreset).put("showRemaining", st.showRemaining)
            .put("eqGains", org.json.JSONArray().apply { st.eqGains.forEach { put(it.toDouble()) } }))
        val lists = org.json.JSONArray()
        val all = library.observePlaylists().first()
        val order = playlists.value.map { it.id }
        all.filter { !it.isSmart }.sortedBy { order.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }.forEach { pl ->
            val items = org.json.JSONArray()
            library.getPlaylistMedia(pl.id).forEach { items.put(trackJson(it)) }
            lists.put(org.json.JSONObject().put("name", pl.name).put("favorites", pl.name.equals("Favorites", true)).put("items", items))
        }
        root.put("playlists", lists)
        root.put("bookmarks", org.json.JSONArray().apply {
            ListenMemory.bookmarks.value.forEach { b ->
                val m = byId[b.mediaId] ?: return@forEach
                put(org.json.JSONObject().put("track", trackJson(m)).put("positionMs", b.positionMs).put("createdAt", b.createdAt))
            }
        })
        root.put("positions", org.json.JSONArray().apply {
            ListenMemory.positions.value.forEach { (id, pos) ->
                val m = byId[id] ?: return@forEach
                put(org.json.JSONObject().put("track", trackJson(m)).put("positionMs", pos))
            }
        })
        return root.toString(2)
    }

    /** Finds the same song on this phone: same name, then the closest length (and same folder). */
    private fun matcher(lib: List<MediaItemModel>): (org.json.JSONObject) -> MediaItemModel? {
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val byTitle = lib.groupBy { norm(it.title) }
        return { t ->
            val cands = byTitle[norm(t.optString("title"))].orEmpty()
            val dur = t.optLong("durationMs")
            val folder = t.optString("folder")
            cands.minByOrNull { m ->
                val d = if (dur > 0 && m.durationMs > 0) kotlin.math.abs(m.durationMs - dur) else 0L
                d + (if (m.folder == folder) 0 else 1_500)
            }?.takeIf { m -> dur <= 0 || m.durationMs <= 0 || kotlin.math.abs(m.durationMs - dur) < 5_000 }
        }
    }

    /** Reads a backup made by [buildBackup]; returns a short report. */
    suspend fun restoreBackup(text: String): String {
        val root = runCatching { org.json.JSONObject(text) }.getOrNull()
            ?: return L.t("این فایل پشتیبان MHJ Player نیست.")
        if (root.optString("app") != "MHJ Player") return L.t("این فایل پشتیبان MHJ Player نیست.")
        // Make sure the phone's songs are in the library first.
        if (allLibraryItems.value.isEmpty()) runCatching { library.upsertMedia(scanner.scanMediaStore()) }
        val lib = library.observeMedia().first()
        val find = matcher(lib)
        var found = 0
        var missing = 0
        val missingNames = mutableListOf<String>()
        root.optJSONObject("settings")?.let { o ->
            settingsStore.update { st ->
                st.copy(
                    theme = runCatching { AppTheme.valueOf(o.optString("theme")) }.getOrDefault(st.theme),
                    englishUi = o.optBoolean("englishUi", st.englishUi),
                    keepNaturalPitch = o.optBoolean("keepNaturalPitch", st.keepNaturalPitch),
                    playbackSpeed = o.optDouble("playbackSpeed", st.playbackSpeed.toDouble()).toFloat(),
                    dialogueClarity = o.optBoolean("dialogueClarity", st.dialogueClarity),
                    nightModeAudio = o.optBoolean("nightModeAudio", st.nightModeAudio),
                    eqEnabled = o.optBoolean("eqEnabled", st.eqEnabled),
                    eqPreamp = o.optDouble("eqPreamp", st.eqPreamp.toDouble()).toFloat(),
                    eqPreset = o.optString("eqPreset", st.eqPreset),
                    showRemaining = o.optBoolean("showRemaining", st.showRemaining),
                    eqGains = o.optJSONArray("eqGains")?.let { a -> (0 until a.length()).map { a.optDouble(it).toFloat() } }
                        ?.takeIf { it.size == st.eqGains.size } ?: st.eqGains,
                )
            }
        }
        val existing = library.observePlaylists().first().filter { !it.isSmart }
        val newOrder = mutableListOf<Long>()
        val lists = root.optJSONArray("playlists") ?: org.json.JSONArray()
        for (i in 0 until lists.length()) {
            val o = lists.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            val id = if (o.optBoolean("favorites")) library.ensureFavoritesPlaylistId()
                else existing.firstOrNull { it.name == name }?.id ?: library.createPlaylist(name)
            val arr = o.optJSONArray("items") ?: org.json.JSONArray()
            val ids = mutableListOf<String>()
            for (k in 0 until arr.length()) {
                val t = arr.optJSONObject(k) ?: continue
                val m = find(t)
                if (m != null) { found++; if (m.id !in ids) ids += m.id }
                else { missing++; if (missingNames.size < 8) missingNames += t.optString("title") }
            }
            // Same order as on the old phone; songs already in the list here stay after them.
            val here = library.getPlaylistMedia(id).map { it.id }.filter { it !in ids }
            library.replacePlaylistEntries(id, ids + here)
            if (!o.optBoolean("favorites")) newOrder += id
        }
        if (newOrder.isNotEmpty()) {
            val rest = playlists.value.filter { !it.isSmart && it.id !in newOrder }.map { it.id }
            settingsStore.setPlaylistOrder(newOrder + rest)
        }
        root.optJSONArray("bookmarks")?.let { a ->
            val list = (0 until a.length()).mapNotNull { k ->
                val o = a.optJSONObject(k) ?: return@mapNotNull null
                val m = find(o.optJSONObject("track") ?: return@mapNotNull null) ?: return@mapNotNull null
                Bookmark(o.optLong("createdAt") + k, m.id, m.title, o.optLong("positionMs"), o.optLong("createdAt"))
            }
            ListenMemory.addBookmarks(list)
        }
        root.optJSONArray("positions")?.let { a ->
            val map = (0 until a.length()).mapNotNull { k ->
                val o = a.optJSONObject(k) ?: return@mapNotNull null
                val m = find(o.optJSONObject("track") ?: return@mapNotNull null) ?: return@mapNotNull null
                m.id to o.optLong("positionMs")
            }.toMap()
            if (map.isNotEmpty()) ListenMemory.setPositions(map)
        }
        val head = L.pick(
            "${lists.length()} پلی‌لیست بازیابی شد. $found آهنگ پیدا شد" + (if (missing > 0) " و $missing آهنگ در این گوشی نبود" else "") + ".",
            "${lists.length()} playlists restored. $found songs found" + (if (missing > 0) ", $missing not on this phone" else "") + ".",
        )
        return if (missingNames.isEmpty()) head else head + "\n" + L.pick("پیدا نشد: ", "Not found: ") + missingNames.joinToString("، ")
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
