package com.mhj.player.core.data.repository

import com.mhj.player.core.data.db.MediaItemDao
import com.mhj.player.core.data.db.MediaItemEntity
import com.mhj.player.core.data.db.PlaylistDao
import com.mhj.player.core.data.db.PlaylistEntity
import com.mhj.player.core.data.db.PlaylistEntryEntity
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlaylistModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepository(
    private val mediaItemDao: MediaItemDao,
    private val playlistDao: PlaylistDao,
) {
    fun observeMedia(): Flow<List<MediaItemModel>> =
        mediaItemDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observePlaylists(): Flow<List<PlaylistModel>> =
        playlistDao.observeAll().map { list ->
            list.map { PlaylistModel(it.id, it.name, it.createdAt) }
        }

    suspend fun upsertMedia(item: MediaItemModel) {
        mediaItemDao.upsert(item.toEntity())
    }

    suspend fun upsertMedia(items: List<MediaItemModel>) {
        mediaItemDao.upsertAll(items.map { it.toEntity() })
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insert(
            PlaylistEntity(name = name, createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addToPlaylist(playlistId: Long, mediaId: String, position: Int) {
        playlistDao.insertEntry(
            PlaylistEntryEntity(
                playlistId = playlistId,
                mediaId = mediaId,
                position = position,
            ),
        )
    }

    suspend fun getPlaylistMedia(playlistId: Long): List<MediaItemModel> =
        playlistDao.getMediaInPlaylist(playlistId).map { it.toModel() }

    fun observePlaylistMedia(playlistId: Long): Flow<List<MediaItemModel>> =
        playlistDao.observeMediaInPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    fun observePlaylistTrackCount(playlistId: Long): Flow<Int> =
        playlistDao.observeEntryCount(playlistId)

    suspend fun seedDemoCatalog(demoItems: List<MediaItemModel>) {
        mediaItemDao.upsertAll(demoItems.map { it.toEntity() })
    }
}

internal fun MediaItemEntity.toModel(): MediaItemModel = MediaItemModel(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.MUSIC),
)

internal fun MediaItemModel.toEntity(lastPlayedAt: Long = System.currentTimeMillis()): MediaItemEntity =
    MediaItemEntity(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUri = artworkUri,
        kind = kind.name,
        lastPlayedAt = lastPlayedAt,
    )
