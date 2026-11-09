package com.mhj.player.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val kind: String,
    val folder: String = "",
    val format: String = "",
    val dateAddedMs: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isSmart: Boolean = false,
    val smartKind: String? = null,
)

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaId")],
)
data class PlaylistEntryEntity(
    val playlistId: Long,
    val mediaId: String,
    val position: Int,
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val kind: String,
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val mediaId: String?,
    val uri: String?,
    val title: String?,
    val artist: String?,
    val kind: String,
    val positionMs: Long,
    val durationMs: Long,
    val wasPlaying: Boolean,
    val shuffle: Boolean,
    val repeatMode: String,
    val updatedAt: Long,
)
