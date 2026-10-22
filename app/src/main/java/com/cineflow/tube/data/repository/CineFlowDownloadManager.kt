package com.cineflow.tube.data.repository

import android.content.Context
import com.cineflow.tube.data.model.DownloadItem
import com.cineflow.tube.data.model.StreamDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * CineFlow Download Manager (Phase 4)
 * ─────────────────────────────────────────────────────────────────────────
 * Manages background download tasks using OkHttp streaming into the
 * app-private files directory. Tracks per-item byte progress via StateFlow.
 *
 * Features:
 *  - Concurrent download support (one OkHttp call per item)
 *  - Byte-level progress tracking (bytesDownloaded / totalBytes)
 *  - Pause (cancel + preserve partial file) and resume (range-request)
 *  - Delete download and remove local file
 *  - Offline playback path resolution for PlaybackController
 *  - Persisted download index via in-memory StateFlow (persistence to DataStore
 *    can be layered on top without changing the public API)
 *
 * Storage: Context.filesDir / "downloads" / "<videoId>.<extension>"
 *
 * Threading: All file I/O runs on Dispatchers.IO. Progress updates flow to
 * Dispatchers.Main via StateFlow so collectors never touch the IO thread.
 */
class CineFlowDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── Public state ───────────────────────────────────────────────────────

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    // Active download job refs, keyed by video ID
    private val activeJobs = mutableMapOf<String, Job>()

    // ── Download directory ─────────────────────────────────────────────────

    private val downloadDir: File
        get() = File(context.filesDir, "downloads").also { it.mkdirs() }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Start downloading the best video stream from [details].
     * If a download for this video ID already exists, this is a no-op.
     */
    fun startDownload(details: StreamDetails, quality: String = "720p") {
        if (_downloads.value.any { it.id == details.id }) return

        // Pick the stream URL: prefer matching quality, fall back to best available
        val streamUrl = resolveStreamUrl(details, quality) ?: run {
            addDownloadItem(
                DownloadItem(
                    id = details.id,
                    title = details.title,
                    uploaderName = details.uploaderName,
                    thumbnailUrl = details.uploaderAvatarUrl ?: "",
                    quality = quality,
                    isCompleted = false,
                    localFilePath = null
                )
            )
            return
        }

        val extension = if (streamUrl.contains(".webm")) "webm" else "mp4"
        val outputFile = File(downloadDir, "${details.id}.$extension")

        val item = DownloadItem(
            id = details.id,
            title = details.title,
            uploaderName = details.uploaderName,
            thumbnailUrl = details.uploaderAvatarUrl ?: "",
            quality = quality,
            format = extension.uppercase(),
            bytesDownloaded = outputFile.length(), // resume from partial
            totalBytes = 0L,
            isCompleted = outputFile.exists() && outputFile.length() > 0,
            localFilePath = if (outputFile.exists()) outputFile.absolutePath else null
        )

        addDownloadItem(item)

        if (item.isCompleted) return  // already on disk

        val job = scope.launch {
            downloadStream(details.id, streamUrl, outputFile)
        }
        activeJobs[details.id] = job
    }

    /**
     * Pause (cancel) an active download without deleting the partial file.
     * A subsequent [startDownload] call for the same video will resume via
     * HTTP Range headers.
     */
    fun pauseDownload(videoId: String) {
        activeJobs[videoId]?.cancel()
        activeJobs.remove(videoId)
    }

    /**
     * Delete the download record and remove the local file.
     */
    fun deleteDownload(videoId: String) {
        pauseDownload(videoId)
        val item = _downloads.value.find { it.id == videoId } ?: return
        item.localFilePath?.let { File(it).delete() }
        // Also try guessing file names
        File(downloadDir, "$videoId.mp4").delete()
        File(downloadDir, "$videoId.webm").delete()
        _downloads.value = _downloads.value.filter { it.id != videoId }
    }

    /**
     * Returns the local file path for [videoId] if the download is complete,
     * null otherwise. Used by PlaybackController to route offline playback.
     */
    fun localPathFor(videoId: String): String? {
        return _downloads.value
            .find { it.id == videoId && it.isCompleted }
            ?.localFilePath
    }

    /**
     * Total bytes used by all completed downloads.
     */
    fun totalStorageUsedBytes(): Long =
        _downloads.value.filter { it.isCompleted }.sumOf { it.totalBytes }

    // ── Internal download logic ────────────────────────────────────────────

    private fun downloadStream(videoId: String, url: String, outputFile: File) {
        val alreadyDownloaded = outputFile.length()

        val requestBuilder = Request.Builder().url(url)
        if (alreadyDownloaded > 0) {
            requestBuilder.header("Range", "bytes=$alreadyDownloaded-")
        }

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            updateItem(videoId) { copy(localFilePath = null) }
            return
        }

        if (!response.isSuccessful && response.code != 206) {
            response.close()
            updateItem(videoId) { copy(localFilePath = null) }
            return
        }

        val body = response.body ?: run { response.close(); return }
        val contentLength = body.contentLength().coerceAtLeast(0L)
        val totalBytes = alreadyDownloaded + contentLength

        updateItem(videoId) { copy(totalBytes = totalBytes) }

        try {
            FileOutputStream(outputFile, /* append = */ alreadyDownloaded > 0).use { fos ->
                body.byteStream().use { stream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var written = alreadyDownloaded

                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        written += bytesRead

                        // Throttle progress updates to every 64 KB to avoid state spam
                        if (written % PROGRESS_REPORT_INTERVAL < BUFFER_SIZE) {
                            updateItem(videoId) { copy(bytesDownloaded = written) }
                        }
                    }

                    // Mark complete
                    updateItem(videoId) {
                        copy(
                            bytesDownloaded = written,
                            totalBytes = written.coerceAtLeast(totalBytes),
                            isCompleted = true,
                            localFilePath = outputFile.absolutePath
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Cancelled or IO error — item stays as partially downloaded
            updateItem(videoId) { copy(bytesDownloaded = outputFile.length()) }
        } finally {
            body.close()
            activeJobs.remove(videoId)
        }
    }

    private fun resolveStreamUrl(details: StreamDetails, quality: String): String? {
        // HLS / DASH are not suitable for simple file downloads; use progressive streams
        val match = details.videoStreams.find {
            it.quality.contains(quality.substringBefore(" "), ignoreCase = true) && !it.isVideoOnly
        }
        return match?.url
            ?: details.videoStreams.firstOrNull { !it.isVideoOnly }?.url
            ?: details.videoStreams.firstOrNull()?.url
            ?: details.audioStreams.maxByOrNull { it.bitrate }?.url
    }

    private fun addDownloadItem(item: DownloadItem) {
        _downloads.value = _downloads.value + item
    }

    private inline fun updateItem(videoId: String, transform: DownloadItem.() -> DownloadItem) {
        _downloads.value = _downloads.value.map { if (it.id == videoId) it.transform() else it }
    }

    companion object {
        private const val BUFFER_SIZE = 8 * 1024          // 8 KB IO buffer
        private const val PROGRESS_REPORT_INTERVAL = 64 * 1024L // report every 64 KB
    }
}
