package com.mhj.player.core.model

enum class AppTheme {
    PORCELAIN_WHITE,
    MIDNIGHT_GOLD,
}

enum class MediaKind {
    MUSIC,
    VIDEO,
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

data class MediaItemModel(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "",
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val kind: MediaKind = MediaKind.MUSIC,
    val folder: String = "",
    val format: String = "",
    val dateAddedMs: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
)

data class PlaylistModel(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isSmart: Boolean = false,
    val smartKind: String? = null,
)

data class PlaybackSnapshot(
    val mediaId: String? = null,
    val uri: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val kind: MediaKind = MediaKind.MUSIC,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queueJson: String = "[]",
    val updatedAt: Long = 0L,
)

data class PlayerUiState(
    val current: MediaItemModel? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<MediaItemModel> = emptyList(),
    val errorMessage: String? = null,
    val audioSessionId: Int = 0,
)

data class AppSettings(
    val theme: AppTheme = AppTheme.MIDNIGHT_GOLD,
    val keepNaturalPitch: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int = 0,
    val dialogueClarity: Boolean = false,
    val nightModeAudio: Boolean = false,
)

enum class LibrarySort {
    NAME,
    ARTIST,
    ALBUM,
    DATE_ADDED,
    FORMAT,
    FOLDER,
    DURATION,
    PLAY_COUNT,
}

data class LibraryFilter(
    val query: String = "",
    val kind: MediaKind? = null,
    val artist: String? = null,
    val album: String? = null,
    val format: String? = null,
    val folder: String? = null,
    val playedOnly: Boolean? = null, // true=played, false=unplayed, null=all
    val sort: LibrarySort = LibrarySort.NAME,
)
