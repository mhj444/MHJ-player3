package com.mhj.player.core.playback.transcript

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Persists transcript text files under app filesDir so they survive process death.
 * One file per media id; same media appends paragraphs.
 */
class TranscriptStore(context: Context) {
    private val root = File(context.filesDir, "transcripts").also { it.mkdirs() }
    private val indexFile = File(root, "index.json")

    fun fileFor(mediaId: String, title: String): File {
        val safeId = mediaId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(root, "$safeId.txt")
        if (!file.exists()) {
            file.writeText("${title.trim().ifEmpty { "Untitled" }}\n\n")
            putIndex(mediaId, file.name, title)
        }
        return file
    }

    /**
     * A fresh file for one run of speech-to-text. Running it again on the same song makes a new
     * file ("title (2)") instead of adding the same words a second time to the old one.
     */
    fun newRun(mediaId: String, title: String): File {
        val safeId = mediaId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val base = title.trim().ifEmpty { "Untitled" }
        val earlier = root.listFiles { f -> f.extension == "txt" && (f.name == "$safeId.txt" || f.name.startsWith("${safeId}__")) }?.size ?: 0
        val file = File(root, "${safeId}__${System.currentTimeMillis()}.txt")
        val shownTitle = if (earlier == 0) base else "$base (${earlier + 1})"
        file.writeText("$shownTitle\n\n")
        putIndex(mediaId, file.name, shownTitle)
        return file
    }

    fun appendTo(file: File, paragraph: String) {
        val text = paragraph.trim()
        if (text.isEmpty()) return
        file.appendText("$text\n\n")
    }

    fun appendParagraph(mediaId: String, title: String, paragraph: String) {
        val text = paragraph.trim()
        if (text.isEmpty()) return
        val file = fileFor(mediaId, title)
        file.appendText("$text\n\n")
        putIndex(mediaId, file.name, title)
    }

    fun read(mediaId: String): String? {
        val name = getIndex()["files"]?.let { it as? JSONObject }?.optString(mediaId).orEmpty()
        val file = if (name.isNotEmpty()) File(root, name) else {
            File(root, "${mediaId.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.txt")
        }
        return if (file.exists()) file.readText() else null
    }

    fun latest(): Pair<String, String>? {
        val files = root.listFiles { f -> f.extension == "txt" }?.sortedByDescending { it.lastModified() }
        val newest = files?.firstOrNull() ?: return null
        return newest.nameWithoutExtension to newest.readText()
    }

    fun listAll(): List<TranscriptFileInfo> {
        val idx = getIndex().optJSONObject("meta") ?: JSONObject()
        return root.listFiles { f -> f.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                val title = idx.optJSONObject(it.name)?.optString("title").orEmpty()
                    .ifEmpty { it.readText().lineSequence().firstOrNull().orEmpty() }
                TranscriptFileInfo(
                    mediaKey = it.nameWithoutExtension,
                    title = title.ifEmpty { it.nameWithoutExtension },
                    path = it.absolutePath,
                    updatedAt = it.lastModified(),
                )
            }
            .orEmpty()
    }

    fun readPath(path: String): String? = File(path).takeIf { it.exists() && it.parentFile == root }?.readText()

    fun delete(path: String): Boolean {
        val f = File(path)
        if (f.parentFile != root || !f.exists()) return false
        val ok = f.delete()
        runCatching {
            val json = getIndex()
            json.optJSONObject("meta")?.remove(f.name)
            val files = json.optJSONObject("files")
            if (files != null) {
                val drop = files.keys().asSequence().filter { files.optString(it) == f.name }.toList()
                drop.forEach { files.remove(it) }
            }
            indexFile.writeText(json.toString())
        }
        return ok
    }

    private fun getIndex(): JSONObject {
        if (!indexFile.exists()) return JSONObject().put("files", JSONObject()).put("meta", JSONObject())
        return runCatching { JSONObject(indexFile.readText()) }.getOrElse {
            JSONObject().put("files", JSONObject()).put("meta", JSONObject())
        }
    }

    private fun putIndex(mediaId: String, fileName: String, title: String) {
        val json = getIndex()
        val files = json.optJSONObject("files") ?: JSONObject().also { json.put("files", it) }
        val meta = json.optJSONObject("meta") ?: JSONObject().also { json.put("meta", it) }
        files.put(mediaId, fileName)
        meta.put(
            fileName,
            JSONObject()
                .put("title", title)
                .put("updatedAt", System.currentTimeMillis()),
        )
        indexFile.writeText(json.toString())
    }
}

data class TranscriptFileInfo(
    val mediaKey: String,
    val title: String,
    val path: String,
    val updatedAt: Long,
)
