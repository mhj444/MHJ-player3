package com.mhj.player.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    // @Upsert updates rows in place. (REPLACE deleted and re-inserted them, which also
    // emptied every playlist those songs were in.)
    @androidx.room.Upsert
    suspend fun upsert(item: MediaItemEntity)

    @androidx.room.Upsert
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaItemEntity?

    @Query("UPDATE media_items SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<String>)

    @Query(
        """
        UPDATE media_items
        SET playCount = playCount + 1, lastPlayedAt = :playedAt
        WHERE id = :id
        """,
    )
    suspend fun recordPlay(id: String, playedAt: Long)

    @Query("SELECT * FROM media_items ORDER BY dateAddedMs DESC LIMIT :limit")
    suspend fun newest(limit: Int): List<MediaItemEntity>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playCount = 0 OR lastPlayedAt = 0 OR lastPlayedAt < :beforeMs
        ORDER BY lastPlayedAt ASC, title ASC
        LIMIT :limit
        """,
    )
    suspend fun notPlayedRecently(beforeMs: Long, limit: Int): List<MediaItemEntity>

    @Query("SELECT * FROM media_items ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    suspend fun mostPlayed(limit: Int): List<MediaItemEntity>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isSmart DESC, createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlaylistEntryEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun clearEntries(playlistId: Long)

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

    @Query("SELECT id FROM playlists WHERE smartKind = :kind LIMIT 1")
    suspend fun findSmartId(kind: String): Long?

    @Query(
        """
        SELECT id FROM playlists
        WHERE isSmart = 0 AND LOWER(name) = 'favorites'
        LIMIT 1
        """,
    )
    suspend fun findFavoritesId(): Long?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeEntry(playlistId: Long, mediaId: String)
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
