package com.mhj.player.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        QueueItemEntity::class,
        PlaybackStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MhjDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun playbackStateDao(): PlaybackStateDao

    companion object {
        @Volatile
        private var instance: MhjDatabase? = null

        fun get(context: Context): MhjDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MhjDatabase::class.java,
                    "mhj_player.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
