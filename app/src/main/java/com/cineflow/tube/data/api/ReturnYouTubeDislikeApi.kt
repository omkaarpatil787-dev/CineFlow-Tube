package com.cineflow.tube.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DislikeInfo(
    val likes: Long = 0,
    val dislikes: Long = 0,
    val rating: Double = 0.0,
    val viewCount: Long = 0
)

class ReturnYouTubeDislikeApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getVotes(videoId: String): DislikeInfo = withContext(Dispatchers.IO) {
        val url = "https://returnyoutubedislikeapi.com/votes?videoId=$videoId"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext DislikeInfo()

            val body = response.body?.string() ?: return@withContext DislikeInfo()
            val obj = JSONObject(body)

            DislikeInfo(
                likes = obj.optLong("likes", 0),
                dislikes = obj.optLong("dislikes", 0),
                rating = obj.optDouble("rating", 0.0),
                viewCount = obj.optLong("viewCount", 0)
            )
        } catch (e: Exception) {
            DislikeInfo()
        }
    }
}
