package com.mhj.player.core.playback.transcript

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads and unpacks a one-time on-device **Persian** Vosk speech model.
 * Tries the smallest official `fa` zip first, then newer small-fa builds.
 */
class VoskModelManager(private val context: Context) {
    private val modelsRoot = File(context.filesDir, "vosk-models").also { it.mkdirs() }
    private val resolvedMeta = File(modelsRoot, "resolved-model.txt")

    /** Directory of the model that was successfully downloaded (or already present). */
    val modelDir: File
        get() {
            val name = resolvedModelDirName()
            return File(modelsRoot, name)
        }

    fun resolvedModelDirName(): String {
        if (resolvedMeta.exists()) {
            val name = resolvedMeta.readText().trim()
            if (name.isNotEmpty() && File(modelsRoot, name).isDirectory) return name
        }
        // Prefer any already-unpacked small-fa model
        for (candidate in FA_MODEL_CANDIDATES) {
            val dir = File(modelsRoot, candidate.dirName)
            if (isReady(dir)) {
                resolvedMeta.writeText(candidate.dirName)
                return candidate.dirName
            }
        }
        return FA_MODEL_CANDIDATES.first().dirName
    }

    fun isModelReady(): Boolean = isReady(modelDir)

    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Exact URL that worked for the last successful download (empty if reused local). */
    fun resolvedModelUrl(): String {
        val urlFile = File(modelsRoot, "resolved-url.txt")
        return if (urlFile.exists()) urlFile.readText().trim() else ""
    }

    suspend fun ensureModel(onProgress: (Float, String) -> Unit): File = withContext(Dispatchers.IO) {
        if (isModelReady()) return@withContext modelDir
        if (!isOnline()) error("offline")

        // Remove stale English model so it is never the only option
        File(modelsRoot, "vosk-model-small-en-us-0.15").deleteRecursively()
        File(modelsRoot, "vosk-model-small-en-us-0.15.zip").delete()

        var lastError: Throwable? = null
        for ((index, candidate) in FA_MODEL_CANDIDATES.withIndex()) {
            onProgress(
                0.01f + index * 0.02f,
                "در حال دانلود مدل فارسی… (${candidate.dirName})",
            )
            val zip = File(modelsRoot, "${candidate.dirName}.zip")
            try {
                val code = headStatus(candidate.url)
                if (code == 404) continue
                download(candidate.url, zip) { p ->
                    onProgress(
                        0.05f + p * 0.75f,
                        "در حال دانلود مدل فارسی… ${(p * 100).toInt()}٪",
                    )
                }
                onProgress(0.82f, "در حال آماده‌سازی مدل فارسی…")
                // Clean previous extract of this name
                File(modelsRoot, candidate.dirName).deleteRecursively()
                unzip(zip, modelsRoot)
                zip.delete()
                var dir = File(modelsRoot, candidate.dirName)
                if (!isReady(dir)) {
                    val found = modelsRoot.walkTopDown()
                        .firstOrNull { it.name == "conf" && it.isDirectory }?.parentFile
                    if (found != null && found.absolutePath != dir.absolutePath) {
                        if (dir.exists()) dir.deleteRecursively()
                        found.renameTo(dir)
                    }
                }
                if (!isReady(dir)) {
                    lastError = IllegalStateException("model_unpack_failed:${candidate.dirName}")
                    continue
                }
                resolvedMeta.writeText(candidate.dirName)
                File(modelsRoot, "resolved-url.txt").writeText(candidate.url)
                onProgress(1f, "مدل فارسی آماده است")
                return@withContext dir
            } catch (t: Throwable) {
                lastError = t
                zip.delete()
                File(modelsRoot, candidate.dirName).deleteRecursively()
            }
        }
        throw lastError ?: IllegalStateException("no_fa_model")
    }

    private fun isReady(dir: File): Boolean {
        val am = File(dir, "am")
        val conf = File(dir, "conf")
        return dir.isDirectory && am.exists() && conf.exists()
    }

    private fun headStatus(url: String): Int {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }
        return try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
        }
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            error("http_${conn.responseCode}")
        }
        conn.inputStream.use { input ->
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            BufferedInputStream(input).use { bis ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val n = bis.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        readTotal += n
                        onProgress((readTotal.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        conn.disconnect()
    }

    private fun unzip(zip: File, destDir: File) {
        ZipInputStream(BufferedInputStream(zip.inputStream())).use { zis ->
            var entry = zis.nextEntry
            val buf = ByteArray(64 * 1024)
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        while (true) {
                            val n = zis.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    data class FaModelCandidate(val dirName: String, val url: String)

    companion object {
        /**
         * Official alphacephei Persian models, smallest first.
         * Prefer `vosk-model-small-fa-*`; skip 404s at runtime.
         */
        val FA_MODEL_CANDIDATES = listOf(
            FaModelCandidate(
                "vosk-model-small-fa-0.4",
                "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.4.zip",
            ),
            FaModelCandidate(
                "vosk-model-small-fa-0.42",
                "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.42.zip",
            ),
            FaModelCandidate(
                "vosk-model-small-fa-0.5",
                "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.5.zip",
            ),
        )
    }
}
