package com.cineflow.tube.data.api

import com.cineflow.tube.data.model.AudioStreamFormat
import com.cineflow.tube.data.model.Chapter
import com.cineflow.tube.data.model.StreamDetails
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.model.SubtitleTrack
import com.cineflow.tube.data.model.VideoStreamFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class InvidiousApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        val DEFAULT_INSTANCES = listOf(
            "https://yewtu.be",
            "https://inv.nadeko.net",
            "https://invidious.drgns.space",
            "https://vid.puffyan.us"
        )
    }

    @Volatile
    var currentBaseUrl: String = DEFAULT_INSTANCES.first()
    @Volatile
    private var instanceIndex: Int = 0

    suspend fun <T> executeWithFailover(block: suspend (baseUrl: String) -> T): T = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        val total = DEFAULT_INSTANCES.size
        for (i in 0 until total) {
            val candidateIndex = (instanceIndex + i) % total
            val candidateUrl = DEFAULT_INSTANCES[candidateIndex]
            try {
                val result = block(candidateUrl)
                instanceIndex = candidateIndex
                currentBaseUrl = candidateUrl
                return@withContext result
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: Exception("All Invidious instances failed")
    }

    suspend fun getTrending(): List<StreamItem> = executeWithFailover { baseUrl ->
        val url = "$baseUrl/api/v1/trending"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Invidious trending error: HTTP ${response.code}")

        val body = response.body?.string() ?: return@executeWithFailover emptyList()
        val jsonArray = JSONArray(body)
        val items = mutableListOf<StreamItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.optString("videoId", "")
            val dur = obj.optLong("lengthSeconds", 0)
            val thumbs = obj.optJSONArray("videoThumbnails")
            val thumbUrl = thumbs?.optJSONObject(0)?.optString("url") ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"

            items.add(
                StreamItem(
                    id = id,
                    title = obj.optString("title", "Untitled"),
                    uploaderName = obj.optString("author", "Unknown Channel"),
                    uploaderUrl = "/channel/${obj.optString("authorId", "")}",
                    thumbnailUrl = thumbUrl,
                    durationSeconds = dur,
                    durationString = formatDuration(dur),
                    views = obj.optLong("viewCount", 0),
                    viewsFormatted = formatViews(obj.optLong("viewCount", 0)),
                    uploadedDateFormatted = obj.optString("publishedText", ""),
                    isLive = obj.optBoolean("liveNow", false)
                )
            )
        }
        items
    }

    suspend fun getStreamDetails(id: String): StreamDetails = executeWithFailover { baseUrl ->
        val url = "$baseUrl/api/v1/videos/$id"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Invidious video error: HTTP ${response.code}")

        val body = response.body?.string() ?: throw Exception("Empty Invidious body")
        val obj = JSONObject(body)

        val videoStreams = mutableListOf<VideoStreamFormat>()
        val formatStreams = obj.optJSONArray("formatStreams") ?: JSONArray()
        for (i in 0 until formatStreams.length()) {
            val f = formatStreams.getJSONObject(i)
            videoStreams.add(
                VideoStreamFormat(
                    url = f.optString("url", ""),
                    quality = f.optString("qualityLabel", "Auto"),
                    format = f.optString("container", "mp4"),
                    bitrate = f.optLong("bitrate", 0),
                    isVideoOnly = false
                )
            )
        }

        val adaptiveFormats = obj.optJSONArray("adaptiveFormats") ?: JSONArray()
        val audioStreams = mutableListOf<AudioStreamFormat>()
        for (i in 0 until adaptiveFormats.length()) {
            val a = adaptiveFormats.getJSONObject(i)
            val type = a.optString("type", "")
            if (type.startsWith("audio/")) {
                audioStreams.add(
                    AudioStreamFormat(
                        url = a.optString("url", ""),
                        format = if (type.contains("opus")) "OPUS" else "M4A",
                        quality = a.optString("audioQuality", "160k"),
                        bitrate = a.optLong("bitrate", 0),
                        mimeType = type.substringBefore(";")
                    )
                )
            }
        }

        val captions = obj.optJSONArray("captions") ?: JSONArray()
        val subtitles = mutableListOf<SubtitleTrack>()
        for (i in 0 until captions.length()) {
            val c = captions.getJSONObject(i)
            subtitles.add(
                SubtitleTrack(
                    url = "$currentBaseUrl${c.optString("url", "")}",
                    mimeType = "text/vtt",
                    name = c.optString("label", "Unknown"),
                    language = c.optString("languageCode", "en")
                )
            )
        }

        val dur = obj.optLong("lengthSeconds", 0)
        StreamDetails(
            id = id,
            title = obj.optString("title", "Untitled"),
            description = obj.optString("description", ""),
            uploaderName = obj.optString("author", "Unknown Channel"),
            uploaderChannelId = obj.optString("authorId", ""),
            uploaderAvatarUrl = obj.optJSONArray("authorThumbnails")?.optJSONObject(0)?.optString("url"),
            uploaderSubscriberCount = obj.optString("subCountText", ""),
            views = obj.optLong("viewCount", 0),
            viewsFormatted = formatViews(obj.optLong("viewCount", 0)),
            likes = obj.optLong("likeCount", 0),
            durationSeconds = dur,
            hlsUrl = obj.optString("hlsUrl", null),
            videoStreams = videoStreams,
            audioStreams = audioStreams,
            subtitles = subtitles
        )
    }

    suspend fun search(query: String): List<StreamItem> = executeWithFailover { baseUrl ->
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/api/v1/search?q=$encoded&type=video"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Invidious search error: HTTP ${response.code}")

        val body = response.body?.string() ?: return@executeWithFailover emptyList()
        val jsonArray = JSONArray(body)
        val items = mutableListOf<StreamItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.optString("type") == "video") {
                val id = obj.optString("videoId", "")
                val dur = obj.optLong("lengthSeconds", 0)
                val thumbs = obj.optJSONArray("videoThumbnails")
                val thumbUrl = thumbs?.optJSONObject(0)?.optString("url") ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                items.add(
                    StreamItem(
                        id = id,
                        title = obj.optString("title", "Untitled"),
                        uploaderName = obj.optString("author", "Unknown Channel"),
                        uploaderUrl = "/channel/${obj.optString("authorId", "")}",
                        thumbnailUrl = thumbUrl,
                        durationSeconds = dur,
                        durationString = formatDuration(dur),
                        views = obj.optLong("viewCount", 0),
                        viewsFormatted = formatViews(obj.optLong("viewCount", 0)),
                        uploadedDateFormatted = obj.optString("publishedText", "")
                    )
                )
            }
        }
        items
    }

    suspend fun getComments(videoId: String): List<com.cineflow.tube.data.model.CommentItem> = withContext(Dispatchers.IO) {
        val url = "$currentBaseUrl/api/v1/comments/$videoId"
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val root = JSONObject(body)
            val commentsArray = root.optJSONArray("comments") ?: JSONArray()
            val items = mutableListOf<com.cineflow.tube.data.model.CommentItem>()

            for (i in 0 until commentsArray.length()) {
                val obj = commentsArray.getJSONObject(i)
                val authorThumbs = obj.optJSONArray("authorThumbnails")
                val avatar = authorThumbs?.optJSONObject(0)?.optString("url")

                items.add(
                    com.cineflow.tube.data.model.CommentItem(
                        id = obj.optString("commentId", "inv_comment_$i"),
                        author = obj.optString("author", "Anonymous"),
                        authorAvatarUrl = avatar,
                        content = obj.optString("content", ""),
                        publishedTime = obj.optString("publishedText", ""),
                        likeCount = obj.optLong("likeCount", 0),
                        replyCount = obj.optLong("replyCount", 0),
                        isHearted = obj.optBoolean("isHearted", false),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }

    private fun formatViews(views: Long): String {
        if (views <= 0) return ""
        return when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
    }
}
