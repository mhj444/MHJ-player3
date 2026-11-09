package com.mhj.player.di

import android.content.Context
import com.mhj.player.core.data.datastore.SettingsDataStore
import com.mhj.player.core.data.db.MhjDatabase
import com.mhj.player.core.data.library.MediaLibraryScanner
import com.mhj.player.core.data.repository.LibraryRepository
import com.mhj.player.core.data.repository.PlaybackPersistenceRepository
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.playback.engine.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val db = MhjDatabase.get(appContext)
    val settingsStore = SettingsDataStore(appContext)
    val libraryRepository = LibraryRepository(db.mediaItemDao(), db.playlistDao())
    val playbackPersistence = PlaybackPersistenceRepository(db.queueDao(), db.playbackStateDao())
    val playbackController = PlaybackController(appContext, scope)
    val mediaScanner = MediaLibraryScanner(appContext)

    fun bootstrap() {
        scope.launch {
            runCatching { playbackController.connect() }
            launch(Dispatchers.IO) {
                runCatching {
                    removeDemoCatalog()
                    libraryRepository.ensureSmartPlaylists()
                    libraryRepository.refreshSmartPlaylists()
                    ensureFavoritesPlaylist()
                }
            }
        }
    }

    /** Older builds put three online sample items in the library; take them out. */
    private suspend fun removeDemoCatalog() {
        runCatching { libraryRepository.deleteMedia(listOf("demo_music_1", "demo_music_2", "demo_video_1")) }
    }

    private suspend fun ensureFavoritesPlaylist() {
        libraryRepository.ensureFavoritesPlaylistId()
    }
}
