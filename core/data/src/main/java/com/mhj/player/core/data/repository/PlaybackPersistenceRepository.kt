package com.mhj.player.core.data.repository

import com.mhj.player.core.data.db.PlaybackStateDao
import com.mhj.player.core.data.db.PlaybackStateEntity
import com.mhj.player.core.data.db.QueueDao
import com.mhj.player.core.data.db.QueueItemEntity
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import com.mhj.player.core.model.PlaybackSnapshot
import com.mhj.player.core.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlaybackPersistenceRepository(
    private val queueDao: QueueDao,
    private val playbackStateDao: PlaybackStateDao,
) {
    fun observeSnapshot(): Flow<PlaybackSnapshot> = combine(
        playbackStateDao.observe(),
        queueDao.observeQueue(),
    ) { state, queue ->
        PlaybackSnapshot(
            mediaId = state?.mediaId,
            uri = state?.uri,
            title = state?.title,
            artist = state?.artist,
            kind = runCatching {
                MediaKind.valueOf(state?.kind ?: MediaKind.MUSIC.name)
            }.getOrDefault(MediaKind.MUSIC),
            positionMs = state?.positionMs ?: 0L,
            durationMs = state?.durationMs ?: 0L,
            isPlaying = state?.wasPlaying ?: false,
            shuffle = state?.shuffle ?: false,
            repeatMode = runCatching {
                RepeatMode.valueOf(state?.repeatMode ?: RepeatMode.OFF.name)
            }.getOrDefault(RepeatMode.OFF),
            queueJson = queue.joinToString(prefix = "[", postfix = "]") { "\"${it.mediaId}\"" },
            updatedAt = state?.updatedAt ?: 0L,
        )
    }

    suspend fun loadQueue(): List<MediaItemModel> =
        queueDao.getQueue().map { it.toModel() }

    suspend fun loadState(): PlaybackStateEntity? = playbackStateDao.get()

    suspend fun saveQueue(items: List<MediaItemModel>) {
        queueDao.replaceQueue(
            items.mapIndexed { index, item ->
                QueueItemEntity(
                    position = index,
                    mediaId = item.id,
                    uri = item.uri,
                    title = item.title,
                    artist = item.artist,
                    album = item.album,
                    durationMs = item.durationMs,
                    artworkUri = item.artworkUri,
                    kind = item.kind.name,
                )
            },
        )
    }

    suspend fun savePlaybackState(
        item: MediaItemModel?,
        positionMs: Long,
        durationMs: Long,
        wasPlaying: Boolean,
        shuffle: Boolean,
        repeatMode: RepeatMode,
    ) {
        playbackStateDao.upsert(
            PlaybackStateEntity(
                id = 1,
                mediaId = item?.id,
                uri = item?.uri,
                title = item?.title,
                artist = item?.artist,
                kind = item?.kind?.name ?: MediaKind.MUSIC.name,
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                wasPlaying = wasPlaying,
                shuffle = shuffle,
                repeatMode = repeatMode.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

private fun QueueItemEntity.toModel(): MediaItemModel = MediaItemModel(
    id = mediaId,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUri = artworkUri,
    kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.MUSIC),
)
