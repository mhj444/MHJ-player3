package com.mhj.player.core.data

import android.content.Context
import com.mhj.player.core.data.datastore.SettingsDataStore
import com.mhj.player.core.data.db.MhjDatabase
import com.mhj.player.core.data.repository.LibraryRepository
import com.mhj.player.core.data.repository.PlaybackPersistenceRepository

/** Simple factory so app/service bootstrap stays free of Room wiring details. */
object DataModule {
    fun database(context: Context): MhjDatabase = MhjDatabase.get(context)

    fun settings(context: Context): SettingsDataStore = SettingsDataStore(context)

    fun libraryRepository(context: Context): LibraryRepository {
        val db = database(context)
        return LibraryRepository(db.mediaItemDao(), db.playlistDao())
    }

    fun playbackPersistence(context: Context): PlaybackPersistenceRepository {
        val db = database(context)
        return PlaybackPersistenceRepository(db.queueDao(), db.playbackStateDao())
    }
}
