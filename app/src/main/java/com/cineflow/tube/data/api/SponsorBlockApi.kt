package com.cineflow.tube.data.api

import com.cineflow.tube.data.model.SponsorSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class SponsorBlockApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        val categories = "%5B%22sponsor%22%2C%22selfpromo%22%2C%22interaction%22%2C%22intro%22%2C%22outro%22%5D"
        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$categories"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(body)
            val segments = mutableListOf<SponsorSegment>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val segmentRange = obj.optJSONArray("segment") ?: continue
                if (segmentRange.length() >= 2) {
                    val start = segmentRange.getDouble(0).toFloat()
                    val end = segmentRange.getDouble(1).toFloat()
                    val category = obj.optString("category", "sponsor")
                    val actionType = obj.optString("actionType", "skip")

                    segments.add(
                        SponsorSegment(
                            category = category,
                            startSeconds = start,
                            endSeconds = end,
                            actionType = actionType
                        )
                    )
                }
            }
            segments
        } catch (e: Exception) {
            emptyList()
        }
    }
}
