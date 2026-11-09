package com.mhj.player.core.data.repository

import com.mhj.player.core.data.db.MediaItemDao
import com.mhj.player.core.data.db.MediaItemEntity
import com.mhj.player.core.data.db.PlaylistDao
import com.mhj.player.core.data.db.PlaylistEntity
import com.mhj.player.core.data.db.PlaylistEntryEntity
import com.mhj.player.core.model.LibraryFilter
import com.mhj.player.core.model.LibrarySort
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

    fun observeFiltered(filter: LibraryFilter): Flow<List<MediaItemModel>> =
        observeMedia().map { list -> list.applyFilter(filter) }

    fun observePlaylists(): Flow<List<PlaylistModel>> =
        playlistDao.observeAll().map { list ->
            list.map {
                PlaylistModel(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt,
                    isSmart = it.isSmart,
                    smartKind = it.smartKind,
                )
            }
        }

    suspend fun upsertMedia(item: MediaItemModel) {
        mediaItemDao.upsert(item.toEntity())
    }

    suspend fun upsertMedia(items: List<MediaItemModel>) {
        mediaItemDao.upsertAll(items.map { it.toEntity() })
    }

    suspend fun renameMedia(id: String, title: String) {
        mediaItemDao.rename(id, title.trim().ifEmpty { return })
    }

    suspend fun deleteMedia(ids: List<String>) {
        if (ids.isNotEmpty()) mediaItemDao.deleteIds(ids)
    }

    suspend fun recordPlay(id: String) {
        mediaItemDao.recordPlay(id, System.currentTimeMillis())
    }

    suspend fun createPlaylist(name: String, isSmart: Boolean = false, smartKind: String? = null): Long {
        return playlistDao.insert(
            PlaylistEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                isSmart = isSmart,
                smartKind = smartKind,
            ),
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

    suspend fun replacePlaylistEntries(playlistId: Long, mediaIds: List<String>) {
        playlistDao.clearEntries(playlistId)
        mediaIds.forEachIndexed { index, id ->
            playlistDao.insertEntry(PlaylistEntryEntity(playlistId, id, index))
        }
    }

    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.rename(id, name)

    suspend fun deletePlaylist(id: Long) {
        playlistDao.clearEntries(id)
        playlistDao.delete(id)
    }

    suspend fun removeFromPlaylist(playlistId: Long, mediaId: String) {
        playlistDao.removeEntry(playlistId, mediaId)
    }

    suspend fun reorderPlaylist(playlistId: Long, orderedMediaIds: List<String>) {
        replacePlaylistEntries(playlistId, orderedMediaIds)
    }

    suspend fun getPlaylistMedia(playlistId: Long): List<MediaItemModel> =
        playlistDao.getMediaInPlaylist(playlistId).map { it.toModel() }

    fun observePlaylistMedia(playlistId: Long): Flow<List<MediaItemModel>> =
        playlistDao.observeMediaInPlaylist(playlistId).map { list -> list.map { it.toModel() } }

    suspend fun ensureSmartPlaylists() {
        suspend fun ensure(kind: String, name: String) {
            if (playlistDao.findSmartId(kind) == null) {
                createPlaylist(name, isSmart = true, smartKind = kind)
            }
        }
        ensure(SMART_NEWEST, "Newest")
        ensure(SMART_STALE, "Not played recently")
        ensure(SMART_MOST, "Most played")
    }

    /** Persistent user Favorites playlist (not a computed smart list). */
    suspend fun ensureFavoritesPlaylistId(): Long {
        playlistDao.findFavoritesId()?.let { return it }
        return createPlaylist("Favorites", isSmart = false, smartKind = null)
    }


    suspend fun isInFavorites(mediaId: String): Boolean {
        val favId = ensureFavoritesPlaylistId()
        return getPlaylistMedia(favId).any { it.id == mediaId }
    }

    suspend fun toggleFavorite(mediaId: String): Boolean {
        val favId = ensureFavoritesPlaylistId()
        val tracks = getPlaylistMedia(favId)
        return if (tracks.any { it.id == mediaId }) {
            removeFromPlaylist(favId, mediaId)
            false
        } else {
            addToPlaylist(favId, mediaId, tracks.size)
            true
        }
    }

    suspend fun refreshSmartPlaylists() {
        ensureSmartPlaylists()
        val newestId = playlistDao.findSmartId(SMART_NEWEST) ?: return
        val staleId = playlistDao.findSmartId(SMART_STALE) ?: return
        val mostId = playlistDao.findSmartId(SMART_MOST) ?: return
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        replacePlaylistEntries(newestId, mediaItemDao.newest(50).map { it.id })
        replacePlaylistEntries(staleId, mediaItemDao.notPlayedRecently(thirtyDaysAgo, 50).map { it.id })
        replacePlaylistEntries(mostId, mediaItemDao.mostPlayed(50).map { it.id })
    }

    suspend fun seedDemoCatalog(demoItems: List<MediaItemModel>) {
        mediaItemDao.upsertAll(demoItems.map { it.toEntity() })
    }

    companion object {
        const val SMART_NEWEST = "newest"
        const val SMART_STALE = "stale"
        const val SMART_MOST = "most_played"
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
    folder = folder,
    format = format,
    dateAddedMs = dateAddedMs,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
)

internal fun MediaItemModel.toEntity(): MediaItemEntity =
    MediaItemEntity(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        artworkUri = artworkUri,
        kind = kind.name,
        folder = folder,
        format = format,
        dateAddedMs = dateAddedMs,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
    )

private fun List<MediaItemModel>.applyFilter(filter: LibraryFilter): List<MediaItemModel> {
    val q = filter.query.trim().lowercase()
    var list = this
    if (q.isNotEmpty()) {
        list = list.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.folder.lowercase().contains(q) ||
                it.format.lowercase().contains(q)
        }
    }
    filter.kind?.let { k -> list = list.filter { it.kind == k } }
    filter.artist?.let { a -> list = list.filter { it.artist.equals(a, ignoreCase = true) } }
    filter.album?.let { a -> list = list.filter { it.album.equals(a, ignoreCase = true) } }
    filter.format?.let { f -> list = list.filter { it.format.equals(f, ignoreCase = true) } }
    filter.folder?.let { f -> list = list.filter { it.folder.equals(f, ignoreCase = true) } }
    when (filter.playedOnly) {
        true -> list = list.filter { it.playCount > 0 || it.lastPlayedAt > 0 }
        false -> list = list.filter { it.playCount == 0 && it.lastPlayedAt == 0L }
        null -> Unit
    }
    return when (filter.sort) {
        LibrarySort.NAME -> list.sortedBy { it.title.lowercase() }
        LibrarySort.ARTIST -> list.sortedBy { it.artist.lowercase() }
        LibrarySort.ALBUM -> list.sortedBy { it.album.lowercase() }
        LibrarySort.DATE_ADDED -> list.sortedByDescending { it.dateAddedMs }
        LibrarySort.FORMAT -> list.sortedBy { it.format.lowercase() }
        LibrarySort.FOLDER -> list.sortedBy { it.folder.lowercase() }
        LibrarySort.DURATION -> list.sortedByDescending { it.durationMs }
        LibrarySort.PLAY_COUNT -> list.sortedByDescending { it.playCount }
    }
}
