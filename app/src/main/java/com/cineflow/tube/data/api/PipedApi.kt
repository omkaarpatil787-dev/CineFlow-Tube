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

class PipedApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        val DEFAULT_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://piped-api.lunar.icu",
            "https://api.piped.projectsegfau.lt"
        )
    }

    @Volatile
    var currentBaseUrl: String = DEFAULT_INSTANCES.first()

    suspend fun getTrending(region: String = "US"): List<StreamItem> = withContext(Dispatchers.IO) {
        val url = "$currentBaseUrl/trending?region=$region"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Piped trending error: HTTP ${response.code}")

        val body = response.body?.string() ?: return@withContext emptyList()
        val jsonArray = JSONArray(body)
        val items = mutableListOf<StreamItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val urlPath = obj.optString("url", "")
            val id = if (urlPath.startsWith("/watch?v=")) urlPath.substringAfter("/watch?v=") else urlPath

            val durationSeconds = obj.optLong("duration", 0)
            items.add(
                StreamItem(
                    id = id,
                    title = obj.optString("title", "Untitled"),
                    uploaderName = obj.optString("uploaderName", "Unknown Channel"),
                    uploaderUrl = obj.optString("uploaderUrl", null),
                    uploaderAvatarUrl = obj.optString("uploaderAvatar", null),
                    thumbnailUrl = obj.optString("thumbnail", ""),
                    durationSeconds = durationSeconds,
                    durationString = formatDuration(durationSeconds),
                    views = obj.optLong("views", 0),
                    viewsFormatted = formatViews(obj.optLong("views", 0)),
                    uploadedDateFormatted = obj.optString("uploadedDate", ""),
                    isLive = obj.optBoolean("isLive", false)
                )
            )
        }
        items
    }

    suspend fun getStreamDetails(id: String): StreamDetails = withContext(Dispatchers.IO) {
        val url = "$currentBaseUrl/streams/$id"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Piped stream error: HTTP ${response.code}")

        val body = response.body?.string() ?: throw Exception("Empty stream body")
        val obj = JSONObject(body)

        val videoStreams = mutableListOf<VideoStreamFormat>()
        val videoStreamsJson = obj.optJSONArray("videoStreams") ?: JSONArray()
        for (i in 0 until videoStreamsJson.length()) {
            val v = videoStreamsJson.getJSONObject(i)
            videoStreams.add(
                VideoStreamFormat(
                    url = v.optString("url", ""),
                    quality = v.optString("quality", "Auto"),
                    format = v.optString("format", "MPEG_4"),
                    width = v.optInt("width", 0),
                    height = v.optInt("height", 0),
                    fps = v.optInt("fps", 30),
                    bitrate = v.optLong("bitrate", 0),
                    isVideoOnly = v.optBoolean("videoOnly", false)
                )
            )
        }

        val audioStreams = mutableListOf<AudioStreamFormat>()
        val audioStreamsJson = obj.optJSONArray("audioStreams") ?: JSONArray()
        for (i in 0 until audioStreamsJson.length()) {
            val a = audioStreamsJson.getJSONObject(i)
            audioStreams.add(
                AudioStreamFormat(
                    url = a.optString("url", ""),
                    format = a.optString("format", "OPUS"),
                    quality = a.optString("quality", "160k"),
                    bitrate = a.optLong("bitrate", 0),
                    mimeType = a.optString("mimeType", "audio/webm")
                )
            )
        }

        val subtitles = mutableListOf<SubtitleTrack>()
        val subtitlesJson = obj.optJSONArray("subtitles") ?: JSONArray()
        for (i in 0 until subtitlesJson.length()) {
            val s = subtitlesJson.getJSONObject(i)
            subtitles.add(
                SubtitleTrack(
                    url = s.optString("url", ""),
                    mimeType = s.optString("mimeType", "text/vtt"),
                    name = s.optString("name", "Unknown"),
                    language = s.optString("code", "en"),
                    isAutoGenerated = s.optBoolean("autoGenerated", false)
                )
            )
        }

        val chapters = mutableListOf<Chapter>()
        val chaptersJson = obj.optJSONArray("chapters") ?: JSONArray()
        for (i in 0 until chaptersJson.length()) {
            val c = chaptersJson.getJSONObject(i)
            chapters.add(
                Chapter(
                    title = c.optString("title", ""),
                    startSeconds = c.optLong("start", 0),
                    thumbnail = c.optString("image", null)
                )
            )
        }

        val relatedVideos = mutableListOf<StreamItem>()
        val relatedJson = obj.optJSONArray("relatedStreams") ?: JSONArray()
        for (i in 0 until relatedJson.length()) {
            val r = relatedJson.getJSONObject(i)
            val rUrl = r.optString("url", "")
            val rId = if (rUrl.startsWith("/watch?v=")) rUrl.substringAfter("/watch?v=") else rUrl
            val dur = r.optLong("duration", 0)
            relatedVideos.add(
                StreamItem(
                    id = rId,
                    title = r.optString("title", "Untitled"),
                    uploaderName = r.optString("uploaderName", ""),
                    uploaderAvatarUrl = r.optString("uploaderAvatar", null),
                    thumbnailUrl = r.optString("thumbnail", ""),
                    durationSeconds = dur,
                    durationString = formatDuration(dur),
                    views = r.optLong("views", 0),
                    viewsFormatted = formatViews(r.optLong("views", 0)),
                    uploadedDateFormatted = r.optString("uploadedDate", "")
                )
            )
        }

        val durationSec = obj.optLong("duration", 0)
        StreamDetails(
            id = id,
            title = obj.optString("title", "Untitled"),
            description = obj.optString("description", ""),
            uploaderName = obj.optString("uploader", "Unknown Channel"),
            uploaderChannelId = obj.optString("uploaderUrl", "").substringAfter("/channel/"),
            uploaderAvatarUrl = obj.optString("uploaderAvatar", null),
            uploaderSubscriberCount = obj.optString("uploaderSubscriberCount", ""),
            views = obj.optLong("views", 0),
            viewsFormatted = formatViews(obj.optLong("views", 0)),
            likes = obj.optLong("likes", 0),
            dislikes = obj.optLong("dislikes", 0),
            durationSeconds = durationSec,
            hlsUrl = obj.optString("hls", null).takeIf { !it.isNullOrEmpty() },
            dashUrl = obj.optString("dash", null).takeIf { !it.isNullOrEmpty() },
            videoStreams = videoStreams,
            audioStreams = audioStreams,
            subtitles = subtitles,
            chapters = chapters,
            relatedVideos = relatedVideos
        )
    }

    suspend fun search(query: String, filter: String = "all"): List<StreamItem> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$currentBaseUrl/search?q=$encoded&filter=$filter"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Piped search error: HTTP ${response.code}")

        val body = response.body?.string() ?: return@withContext emptyList()
        val obj = JSONObject(body)
        val jsonArray = obj.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<StreamItem>()

        for (i in 0 until jsonArray.length()) {
            val itemObj = jsonArray.getJSONObject(i)
            if (itemObj.optString("type") == "stream") {
                val urlPath = itemObj.optString("url", "")
                val id = if (urlPath.startsWith("/watch?v=")) urlPath.substringAfter("/watch?v=") else urlPath
                val dur = itemObj.optLong("duration", 0)
                items.add(
                    StreamItem(
                        id = id,
                        title = itemObj.optString("title", "Untitled"),
                        uploaderName = itemObj.optString("uploaderName", "Unknown Channel"),
                        uploaderAvatarUrl = itemObj.optString("uploaderAvatar", null),
                        thumbnailUrl = itemObj.optString("thumbnail", ""),
                        durationSeconds = dur,
                        durationString = formatDuration(dur),
                        views = itemObj.optLong("views", 0),
                        viewsFormatted = formatViews(itemObj.optLong("views", 0)),
                        uploadedDateFormatted = itemObj.optString("uploadedDate", "")
                    )
                )
            }
        }
        items
    }

    suspend fun getComments(videoId: String): List<com.cineflow.tube.data.model.CommentItem> = withContext(Dispatchers.IO) {
        val url = "$currentBaseUrl/comments/$videoId"
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
                items.add(
                    com.cineflow.tube.data.model.CommentItem(
                        id = obj.optString("commentId", "comment_$i"),
                        author = obj.optString("author", "Anonymous"),
                        authorAvatarUrl = obj.optString("thumbnail", null),
                        content = obj.optString("commentText", ""),
                        publishedTime = obj.optString("commentedTime", ""),
                        likeCount = obj.optLong("likeCount", 0),
                        replyCount = obj.optLong("replyCount", 0),
                        isHearted = obj.optBoolean("hearted", false),
                        isPinned = obj.optBoolean("pinned", false)
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
