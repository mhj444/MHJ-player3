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
                    ensureDemoCatalog()
                    libraryRepository.ensureSmartPlaylists()
                    libraryRepository.refreshSmartPlaylists()
                    ensureFavoritesPlaylist()
                }
            }
        }
    }

    private suspend fun ensureDemoCatalog() {
        val existing = libraryRepository.observeMedia().first()
        if (existing.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val demos = listOf(
            MediaItemModel(
                id = "demo_music_1",
                uri = "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3",
                title = "Jazz in Paris",
                artist = "Media Right Productions",
                album = "YouTube Audio Library",
                durationMs = 102000L,
                kind = MediaKind.MUSIC,
                folder = "Demo",
                format = "MP3",
                dateAddedMs = now,
            ),
            MediaItemModel(
                id = "demo_music_2",
                uri = "https://storage.googleapis.com/exoplayer-test-media-1/mp3/android-dev.mp3",
                title = "The Last Kingdom",
                artist = "Ramin Djawadi",
                album = "Demo",
                durationMs = 0L,
                kind = MediaKind.MUSIC,
                folder = "Demo",
                format = "MP3",
                dateAddedMs = now - 86_400_000L,
            ),
            MediaItemModel(
                id = "demo_video_1",
                uri = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-lavf-56.36.100-aac-lc-128k-h264-baseline-main-level31-4500k-1920x1080.mp4",
                title = "Android Screens",
                artist = "ExoPlayer Demo",
                album = "Video",
                durationMs = 0L,
                kind = MediaKind.VIDEO,
                folder = "Demo",
                format = "MP4",
                dateAddedMs = now - 172_800_000L,
            ),
        )
        libraryRepository.upsertMedia(demos)
    }

    private suspend fun ensureFavoritesPlaylist() {
        val playlists = libraryRepository.observePlaylists().first()
        if (playlists.any { !it.isSmart && it.name.equals("Favorites", ignoreCase = true) }) return
        val id = libraryRepository.createPlaylist("Favorites")
        libraryRepository.addToPlaylist(id, "demo_music_1", 0)
    }
}
