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
    val abPointAMs: Long? = null,
    val abPointBMs: Long? = null,
) {
    val abLoopActive: Boolean get() = abPointAMs != null && abPointBMs != null
    val abLabel: String
        get() = when {
            abPointAMs != null && abPointBMs != null -> "A-B"
            abPointAMs != null -> "A…"
            else -> "A-B"
        }
}

data class AppSettings(
    val theme: AppTheme = AppTheme.MIDNIGHT_GOLD,
    val keepNaturalPitch: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    /** Last chosen sleep-timer length (for the dial); the timer itself runs from [sleepAtMs]. */
    val sleepTimerMinutes: Int = 0,
    /** When playback stops (epoch ms); 0 = timer off. */
    val sleepAtMs: Long = 0L,
    /** Stop at the end of the current track / video instead of at a time. */
    val sleepAtEnd: Boolean = false,
    val dialogueClarity: Boolean = false,
    val nightModeAudio: Boolean = false,
    val eqEnabled: Boolean = true,
    /** Ten band gains in dB (-12..+12) at [Eq.FREQS]. */
    val eqGains: List<Float> = List(Eq.BANDS) { 0f },
    /** Overall level in dB (-12..+6). */
    val eqPreamp: Float = 0f,
    val eqPreset: String = "flat",
    /** Time label next to the bar shows time left instead of elapsed. */
    val showRemaining: Boolean = false,
)

/** The equalizer layout and its ready-made settings. */
object Eq {
    const val BANDS = 10
    val FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    const val MAX_DB = 12f

    data class Preset(val id: String, val nameFa: String, val gains: List<Float>)

    val PRESETS = listOf(
        Preset("flat", "صاف", List(BANDS) { 0f }),
        Preset("bass", "بیس", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
        Preset("bass_strong", "بیس قوی", listOf(9f, 8f, 6f, 3f, 0f, -1f, -1f, 0f, 0f, 0f)),
        Preset("treble", "تریبل", listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f)),
        Preset("vocal", "صدای خواننده", listOf(-2f, -2f, -1f, 1f, 3f, 4f, 4f, 3f, 1f, 0f)),
        Preset("pop", "پاپ", listOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f)),
        Preset("rock", "راک", listOf(5f, 4f, 2f, -1f, -2f, -1f, 2f, 3f, 4f, 4f)),
        Preset("classical", "کلاسیک", listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f)),
        Preset("jazz", "جز", listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
        Preset("electronic", "الکترونیک", listOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 1f, 4f, 5f)),
        Preset("persian", "سنتی", listOf(1f, 1f, 2f, 2f, 1f, 2f, 3f, 2f, 1f, 0f)),
        Preset("headphones", "هدفون", listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f)),
        Preset("small_speaker", "بلندگوی گوشی", listOf(-6f, -4f, -1f, 2f, 3f, 3f, 2f, 1f, 0f, -1f)),
    )

    fun presetName(id: String) = PRESETS.firstOrNull { it.id == id }?.nameFa ?: "دلخواه"
}

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
