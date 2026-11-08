package com.mhj.player.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaItemEntity?
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlaylistEntryEntity)

    @Query(
        """
        SELECT m.* FROM media_items m
        INNER JOIN playlist_entries e ON e.mediaId = m.id
        WHERE e.playlistId = :playlistId
        ORDER BY e.position ASC
        """,
    )
    fun observeMediaInPlaylist(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT m.* FROM media_items m
        INNER JOIN playlist_entries e ON e.mediaId = m.id
        WHERE e.playlistId = :playlistId
        ORDER BY e.position ASC
        """,
    )
    suspend fun getMediaInPlaylist(playlistId: Long): List<MediaItemEntity>

    @Query("SELECT COUNT(*) FROM playlist_entries WHERE playlistId = :playlistId")
    fun observeEntryCount(playlistId: Long): Flow<Int>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun observeQueue(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getQueue(): List<QueueItemEntity>

    @Query("DELETE FROM queue_items")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Transaction
    suspend fun replaceQueue(items: List<QueueItemEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM playback_state WHERE id = 1 LIMIT 1")
    suspend fun get(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlaybackStateEntity)
}
