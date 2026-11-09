package com.mhj.player.core.data.memory

import android.content.Context
import android.content.SharedPreferences
import com.mhj.player.core.model.Bookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the app remembers about listening, shared by the screen and the playback service:
 *  - where you stopped in each long file (audiobooks, podcasts), so it continues from there
 *  - bookmarks (saved moments)
 */
object ListenMemory {
    /** Files at least this long remember where you stopped. */
    const val LONG_FILE_MS = 10 * 60_000L
    /** Saved position meaning "listened to the end". */
    const val FINISHED = -1L

    private var prefs: SharedPreferences? = null
    private val _positions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val positions: StateFlow<Map<String, Long>> = _positions.asStateFlow()
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("listen_memory", Context.MODE_PRIVATE)
        prefs = p
        _positions.value = runCatching {
            val o = JSONObject(p.getString("positions", "{}") ?: "{}")
            o.keys().asSequence().associateWith { o.getLong(it) }
        }.getOrDefault(emptyMap())
        _bookmarks.value = runCatching { parseBookmarks(JSONArray(p.getString("bookmarks", "[]") ?: "[]")) }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------- positions

    /** Where to start this file: 0 for short files, finished ones, or ones never opened. */
    fun resumeAt(mediaId: String): Long = (_positions.value[mediaId] ?: 0L).coerceAtLeast(0L)

    fun remember(mediaId: String, positionMs: Long, durationMs: Long) {
        if (mediaId.isBlank() || durationMs < LONG_FILE_MS) return
        val v = when {
            positionMs >= durationMs - 30_000 -> FINISHED
            positionMs < 5_000 -> return
            else -> positionMs
        }
        val cur = _positions.value
        if (cur[mediaId] == v) return
        _positions.value = cur + (mediaId to v)
        savePositions()
    }

    fun setPositions(map: Map<String, Long>) {
        _positions.value = _positions.value + map
        savePositions()
    }

    private fun savePositions() {
        val o = JSONObject()
        _positions.value.forEach { (k, v) -> o.put(k, v) }
        prefs?.edit()?.putString("positions", o.toString())?.apply()
    }

    // ---------------------------------------------------------------- bookmarks

    fun addBookmark(mediaId: String, title: String, positionMs: Long): Bookmark {
        val b = Bookmark(System.currentTimeMillis(), mediaId, title, positionMs.coerceAtLeast(0), System.currentTimeMillis())
        _bookmarks.value = (listOf(b) + _bookmarks.value)
        saveBookmarks()
        return b
    }

    fun removeBookmark(id: Long) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == id }
        saveBookmarks()
    }

    fun addBookmarks(list: List<Bookmark>) {
        val have = _bookmarks.value.map { it.mediaId to it.positionMs / 1000 }.toSet()
        val fresh = list.filter { (it.mediaId to it.positionMs / 1000) !in have }
        _bookmarks.value = (_bookmarks.value + fresh).sortedByDescending { it.createdAt }
        saveBookmarks()
    }

    private fun saveBookmarks() {
        prefs?.edit()?.putString("bookmarks", bookmarksJson(_bookmarks.value).toString())?.apply()
    }

    fun bookmarksJson(list: List<Bookmark>): JSONArray = JSONArray().apply {
        list.forEach { b ->
            put(JSONObject().put("id", b.id).put("mediaId", b.mediaId).put("title", b.title)
                .put("positionMs", b.positionMs).put("createdAt", b.createdAt))
        }
    }

    fun parseBookmarks(a: JSONArray): List<Bookmark> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        Bookmark(o.optLong("id"), o.optString("mediaId"), o.optString("title"), o.optLong("positionMs"), o.optLong("createdAt"))
    }
}
