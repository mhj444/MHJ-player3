package com.mhj.player.core.data.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * First-pass library scan via MediaStore + optional SAF tree walk.
 */
class MediaLibraryScanner(private val context: Context) {

    suspend fun scanMediaStore(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItemModel>()
        items += queryCollection(
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            kind = MediaKind.MUSIC,
            idColumn = MediaStore.Audio.Media._ID,
            titleColumn = MediaStore.Audio.Media.TITLE,
            artistColumn = MediaStore.Audio.Media.ARTIST,
            albumColumn = MediaStore.Audio.Media.ALBUM,
            durationColumn = MediaStore.Audio.Media.DURATION,
            dateColumn = MediaStore.Audio.Media.DATE_ADDED,
            displayNameColumn = MediaStore.Audio.Media.DISPLAY_NAME,
            relativePathColumn = MediaStore.Audio.Media.RELATIVE_PATH,
            mimeColumn = MediaStore.Audio.Media.MIME_TYPE,
        )
        items += queryCollection(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            kind = MediaKind.VIDEO,
            idColumn = MediaStore.Video.Media._ID,
            titleColumn = MediaStore.Video.Media.TITLE,
            artistColumn = MediaStore.Video.Media.ARTIST,
            albumColumn = MediaStore.Video.Media.ALBUM,
            durationColumn = MediaStore.Video.Media.DURATION,
            dateColumn = MediaStore.Video.Media.DATE_ADDED,
            displayNameColumn = MediaStore.Video.Media.DISPLAY_NAME,
            relativePathColumn = MediaStore.Video.Media.RELATIVE_PATH,
            mimeColumn = MediaStore.Video.Media.MIME_TYPE,
        )
        items
    }

    suspend fun scanSafTree(treeUri: Uri): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val out = mutableListOf<MediaItemModel>()
        cr.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIdx).orEmpty()
                val kind = when {
                    mime.startsWith("audio/") -> MediaKind.MUSIC
                    mime.startsWith("video/") -> MediaKind.VIDEO
                    else -> null
                } ?: continue
                val docId = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx).orEmpty()
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val format = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mime)
                    ?.uppercase()
                    .orEmpty()
                    .ifEmpty { name.substringAfterLast('.', "").uppercase() }
                out += MediaItemModel(
                    id = "saf_${docId.hashCode()}",
                    uri = uri.toString(),
                    title = name.substringBeforeLast('.').ifEmpty { name },
                    artist = "Unknown artist",
                    album = "",
                    durationMs = 0L,
                    kind = kind,
                    folder = DocumentsContract.getTreeDocumentId(treeUri),
                    format = format,
                    dateAddedMs = cursor.getLong(modIdx),
                )
            }
        }
        out
    }

    private fun queryCollection(
        collection: Uri,
        kind: MediaKind,
        idColumn: String,
        titleColumn: String,
        artistColumn: String,
        albumColumn: String,
        durationColumn: String,
        dateColumn: String,
        displayNameColumn: String,
        relativePathColumn: String,
        mimeColumn: String,
    ): List<MediaItemModel> {
        val projection = arrayOf(
            idColumn,
            titleColumn,
            artistColumn,
            albumColumn,
            durationColumn,
            dateColumn,
            displayNameColumn,
            relativePathColumn,
            mimeColumn,
        )
        val out = mutableListOf<MediaItemModel>()
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "$dateColumn DESC",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(idColumn)
                val titleIdx = cursor.getColumnIndexOrThrow(titleColumn)
                val artistIdx = cursor.getColumnIndexOrThrow(artistColumn)
                val albumIdx = cursor.getColumnIndexOrThrow(albumColumn)
                val durationIdx = cursor.getColumnIndexOrThrow(durationColumn)
                val dateIdx = cursor.getColumnIndexOrThrow(dateColumn)
                val nameIdx = cursor.getColumnIndexOrThrow(displayNameColumn)
                val pathIdx = cursor.getColumnIndexOrThrow(relativePathColumn)
                val mimeIdx = cursor.getColumnIndexOrThrow(mimeColumn)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val display = cursor.getString(nameIdx).orEmpty()
                    val mime = cursor.getString(mimeIdx).orEmpty()
                    val format = MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mime)
                        ?.uppercase()
                        .orEmpty()
                        .ifEmpty { display.substringAfterLast('.', "").uppercase() }
                    out += MediaItemModel(
                        id = "${kind.name.lowercase()}_$id",
                        uri = uri.toString(),
                        title = cursor.getString(titleIdx)?.ifBlank { null }
                            ?: display.substringBeforeLast('.').ifEmpty { "Untitled" },
                        artist = cursor.getString(artistIdx)?.ifBlank { null } ?: "Unknown artist",
                        album = cursor.getString(albumIdx).orEmpty(),
                        durationMs = cursor.getLong(durationIdx).coerceAtLeast(0L),
                        kind = kind,
                        folder = cursor.getString(pathIdx)?.trimEnd('/')?.substringAfterLast('/')
                            ?.ifEmpty { cursor.getString(pathIdx).orEmpty() }
                            .orEmpty(),
                        format = format,
                        dateAddedMs = cursor.getLong(dateIdx) * 1000L,
                    )
                }
            }
        }
        return out
    }
}
