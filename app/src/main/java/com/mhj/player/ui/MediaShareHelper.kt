package com.mhj.player.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.mhj.player.core.model.MediaItemModel
import com.mhj.player.core.model.MediaKind
import java.io.File
import java.io.FileOutputStream

object MediaShareHelper {
    private const val TAG = "MediaShare"

    fun share(context: Context, item: MediaItemModel) {
        val mime = if (item.kind == MediaKind.VIDEO) "video/*" else "audio/*"
        val uri = resolveShareUri(context, item) ?: run {
            // Fallback: share title + uri text
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.uri}")
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, item.title, uri)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
    }

    /** Several songs / videos in one share window (instead of one window per file). */
    fun shareMany(context: Context, items: List<MediaItemModel>) {
        if (items.isEmpty()) return
        if (items.size == 1) return share(context, items[0])
        val uris = ArrayList(items.mapNotNull { resolveShareUri(context, it) })
        if (uris.isEmpty()) return
        val allVideo = items.all { it.kind == MediaKind.VIDEO }
        val allMusic = items.all { it.kind != MediaKind.VIDEO }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = when { allVideo -> "video/*"; allMusic -> "audio/*"; else -> "*/*" }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val clip = android.content.ClipData.newUri(context.contentResolver, items[0].title, uris[0])
            uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
            clipData = clip
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری ${uris.size} فایل"))
    }

    fun deleteFromDevice(context: Context, item: MediaItemModel): Boolean {
        val uri = Uri.parse(item.uri)
        return try {
            when (uri.scheme) {
                "content" -> {
                    val rows = context.contentResolver.delete(uri, null, null)
                    rows > 0 || runCatching {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    }.getOrDefault(false)
                }
                "file" -> {
                    val path = uri.path ?: return false
                    File(path).delete()
                }
                else -> false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "delete failed ${item.uri}", t)
            false
        }
    }

    fun rename(context: Context, item: MediaItemModel, newTitle: String): Boolean {
        val uri = Uri.parse(item.uri)
        val display = newTitle.trim()
        if (display.isEmpty()) return false
        return try {
            when (uri.scheme) {
                "content" -> {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, display)
                        put(MediaStore.MediaColumns.TITLE, display)
                    }
                    context.contentResolver.update(uri, values, null, null) > 0 ||
                        runCatching {
                            DocumentsContract.renameDocument(context.contentResolver, uri, display) != null
                        }.getOrDefault(false)
                }
                else -> false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "rename failed ${item.uri}", t)
            false
        }
    }

    private fun resolveShareUri(context: Context, item: MediaItemModel): Uri? {
        val parsed = Uri.parse(item.uri)
        when (parsed.scheme) {
            "content" -> return parsed
            "file" -> {
                val file = File(parsed.path ?: return null)
                if (!file.exists()) return null
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }
            "http", "https" -> {
                // Copy a short cache snippet is unnecessary; share as text below
                return null
            }
        }
        // Try copy content into cache for FileProvider
        return runCatching {
            val cacheDir = File(context.cacheDir, "share").also { it.mkdirs() }
            val ext = item.format.lowercase().ifEmpty { "bin" }
            val out = File(cacheDir, "${item.id.hashCode()}.$ext")
            context.contentResolver.openInputStream(parsed)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
        }.getOrNull()
    }
}
