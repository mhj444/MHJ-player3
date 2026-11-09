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
 * Downloads and unpacks a one-time on-device Vosk speech model.
 */
class VoskModelManager(private val context: Context) {
    private val modelsRoot = File(context.filesDir, "vosk-models").also { it.mkdirs() }

    val modelDir: File get() = File(modelsRoot, MODEL_DIR_NAME)

    fun isModelReady(): Boolean {
        val am = File(modelDir, "am")
        val conf = File(modelDir, "conf")
        return modelDir.isDirectory && am.exists() && conf.exists()
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun ensureModel(onProgress: (Float, String) -> Unit): File = withContext(Dispatchers.IO) {
        if (isModelReady()) return@withContext modelDir
        if (!isOnline()) {
            error("offline")
        }
        onProgress(0.01f, "در حال دانلود مدل گفتار…")
        val zip = File(modelsRoot, "$MODEL_DIR_NAME.zip")
        download(MODEL_URL, zip) { p ->
            onProgress(0.05f + p * 0.75f, "در حال دانلود مدل گفتار… ${(p * 100).toInt()}٪")
        }
        onProgress(0.82f, "در حال آماده‌سازی مدل…")
        unzip(zip, modelsRoot)
        zip.delete()
        // Zip root folder name matches MODEL_DIR_NAME
        if (!isModelReady()) {
            // Some zips nest one level — find conf/am
            val found = modelsRoot.walkTopDown().firstOrNull { it.name == "conf" && it.isDirectory }?.parentFile
            if (found != null && found.absolutePath != modelDir.absolutePath) {
                if (modelDir.exists()) modelDir.deleteRecursively()
                found.renameTo(modelDir)
            }
        }
        if (!isModelReady()) error("model_unpack_failed")
        onProgress(1f, "مدل آماده است")
        modelDir
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
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

    companion object {
        // English small model (~40MB). Persian UI; model works for English media/demo tracks.
        const val MODEL_DIR_NAME = "vosk-model-small-en-us-0.15"
        const val MODEL_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    }
}
