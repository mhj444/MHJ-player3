package com.mhj.player.ui

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.mhj.player.core.model.MediaItemModel
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Real file changes on the phone (delete, rename, move between folders). Android asks the user
 * once per action ("Allow MHJ Player to modify these files?") for files made by other apps.
 */
object MediaFileOps {
    private const val TAG = "MediaFileOps"

    fun isMediaStore(item: MediaItemModel): Boolean =
        Uri.parse(item.uri).let { it.scheme == "content" && it.authority == MediaStore.AUTHORITY }

    fun uris(items: List<MediaItemModel>): List<Uri> = items.filter(::isMediaStore).map { Uri.parse(it.uri) }

    fun deleteRequest(context: Context, items: List<MediaItemModel>): IntentSender? =
        uris(items).takeIf { it.isNotEmpty() }?.let { MediaStore.createDeleteRequest(context.contentResolver, it).intentSender }

    fun writeRequest(context: Context, items: List<MediaItemModel>): IntentSender? =
        uris(items).takeIf { it.isNotEmpty() }?.let { MediaStore.createWriteRequest(context.contentResolver, it).intentSender }

    /** "Music/Audiobooks/" style folder of one file. */
    fun relativePath(context: Context, item: MediaItemModel): String? = runCatching {
        context.contentResolver.query(Uri.parse(item.uri), arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    /** Every folder on the phone that has songs or videos in it. */
    fun allFolders(context: Context): List<String> {
        val out = sortedSetOf<String>()
        listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).forEach { col ->
            runCatching {
                context.contentResolver.query(col, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { c ->
                    while (c.moveToNext()) c.getString(0)?.let { out += it }
                }
            }
        }
        return out.toList()
    }

    /** New file name (the extension is kept). Call after the user allowed changes. */
    fun rename(context: Context, item: MediaItemModel, newName: String): Boolean {
        val uri = Uri.parse(item.uri)
        val old = displayName(context, uri).orEmpty()
        val ext = old.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        val base = newName.trim().removeSuffix(ext)
        if (base.isEmpty()) return false
        return runCatching {
            val v = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, base + ext)
                put(MediaStore.MediaColumns.TITLE, base)
            }
            context.contentResolver.update(uri, v, null, null) > 0
        }.onFailure { Log.e(TAG, "rename", it) }.getOrDefault(false)
    }

    /** Moves a file to another folder ("Music/Old songs/"). Call after the user allowed changes. */
    fun move(context: Context, item: MediaItemModel, folder: String): Boolean {
        val dest = folder.trim().trim('/') + "/"
        return runCatching {
            val v = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, dest) }
            context.contentResolver.update(Uri.parse(item.uri), v, null, null) > 0
        }.onFailure { Log.e(TAG, "move", it) }.getOrDefault(false)
    }

    /** The same folder with a new last name ("Music/A/" → "Music/B/"). */
    fun renamedFolder(path: String, newName: String): String {
        val parts = path.trim('/').split('/').toMutableList()
        if (parts.isEmpty()) return newName.trim() + "/"
        parts[parts.lastIndex] = newName.trim()
        return parts.joinToString("/") + "/"
    }

    // ---------------------------------------------------------------- subtitles

    /**
     * Copies a subtitle file into the app as UTF-8. Persian .srt files are often saved in the old
     * Windows-1256 code page, which shows as gibberish unless converted.
     */
    fun importSubtitle(context: Context, source: Uri, mediaId: String): Pair<File, String>? = runCatching {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() } ?: return null
        val name = displayName(context, source).orEmpty().lowercase()
        val text = decode(bytes)
        val ext = when {
            name.endsWith(".vtt") || text.trimStart().startsWith("WEBVTT") -> "vtt"
            name.endsWith(".ass") || name.endsWith(".ssa") || text.contains("[Script Info]") -> "ass"
            else -> "srt"
        }
        val dir = File(context.filesDir, "subtitles").apply { mkdirs() }
        val out = File(dir, mediaId.replace(Regex("[^A-Za-z0-9_-]"), "_") + "." + ext)
        out.writeText(text, Charsets.UTF_8)
        val mime = when (ext) {
            "vtt" -> androidx.media3.common.MimeTypes.TEXT_VTT
            "ass" -> androidx.media3.common.MimeTypes.TEXT_SSA
            else -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
        }
        out to mime
    }.onFailure { Log.e(TAG, "subtitle", it) }.getOrNull()

    private fun decode(b: ByteArray): String {
        if (b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte()) return String(b, 2, b.size - 2, Charsets.UTF_16LE)
        if (b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte()) return String(b, 2, b.size - 2, Charsets.UTF_16BE)
        val start = if (b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) 3 else 0
        val utf8 = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        return runCatching { utf8.decode(ByteBuffer.wrap(b, start, b.size - start)).toString() }
            .getOrElse { String(b, Charset.forName("windows-1256")) }
    }
}
