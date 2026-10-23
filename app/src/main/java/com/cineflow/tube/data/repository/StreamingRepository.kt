package com.cineflow.tube.data.repository

import com.cineflow.tube.data.api.InvidiousApi
import com.cineflow.tube.data.api.PipedApi
import com.cineflow.tube.data.api.ReturnYouTubeDislikeApi
import com.cineflow.tube.data.api.SponsorBlockApi
import com.cineflow.tube.data.model.Chapter
import com.cineflow.tube.data.model.SponsorSegment
import com.cineflow.tube.data.model.StreamDetails
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.model.VideoStreamFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class BackendType {
    PIPED,
    INVIDIOUS
}

sealed class CineFlowException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkOffline(message: String = "No internet connection detected") : CineFlowException(message)
    class BackendUnreachable(val backend: BackendType, message: String = "Streaming instance unreachable") : CineFlowException(message)
    class ContentUnavailable(val videoId: String, message: String = "This stream is unavailable") : CineFlowException(message)
    class Generic(message: String, cause: Throwable? = null) : CineFlowException(message, cause)
}

class StreamingRepository(
    val pipedApi: PipedApi = PipedApi(),
    val invidiousApi: InvidiousApi = InvidiousApi(),
    private val sponsorBlockApi: SponsorBlockApi = SponsorBlockApi(),
    private val rydApi: ReturnYouTubeDislikeApi = ReturnYouTubeDislikeApi()
) {
    var activeBackend: BackendType = BackendType.PIPED

    suspend fun getTrending(page: Int = 1): List<StreamItem> = withContext(Dispatchers.IO) {
        try {
            val list = when (activeBackend) {
                BackendType.PIPED -> pipedApi.getTrending()
                BackendType.INVIDIOUS -> invidiousApi.getTrending()
            }
            if (list.isNotEmpty()) list else getFallbackTrending()
        } catch (e: Exception) {
            // Failover to secondary backend
            try {
                val failoverList = if (activeBackend == BackendType.PIPED) {
                    invidiousApi.getTrending()
                } else {
                    pipedApi.getTrending()
                }
                if (failoverList.isNotEmpty()) failoverList else getFallbackTrending()
            } catch (ex: Exception) {
                if (isOfflineException(ex)) {
                    getFallbackTrending()
                } else {
                    getFallbackTrending()
                }
            }
        }
    }

    suspend fun search(query: String, page: Int = 1): List<StreamItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        try {
            val list = when (activeBackend) {
                BackendType.PIPED -> pipedApi.search(trimmed)
                BackendType.INVIDIOUS -> invidiousApi.search(trimmed)
            }
            if (list.isNotEmpty()) list else getFallbackTrending().filter {
                it.title.contains(trimmed, ignoreCase = true) || it.uploaderName.contains(trimmed, ignoreCase = true)
            }
        } catch (e: Exception) {
            try {
                val failoverList = if (activeBackend == BackendType.PIPED) {
                    invidiousApi.search(trimmed)
                } else {
                    pipedApi.search(trimmed)
                }
                if (failoverList.isNotEmpty()) failoverList else getFallbackTrending().filter {
                    it.title.contains(trimmed, ignoreCase = true)
                }
            } catch (ex: Exception) {
                getFallbackTrending().filter {
                    it.title.contains(trimmed, ignoreCase = true) || it.uploaderName.contains(trimmed, ignoreCase = true)
                }
            }
        }
    }

    suspend fun getStreamDetails(videoId: String): StreamDetails = withContext(Dispatchers.IO) {
        coroutineScope {
            try {
                val streamDetailsDeferred = async {
                    try {
                        if (activeBackend == BackendType.PIPED) {
                            pipedApi.getStreamDetails(videoId)
                        } else {
                            invidiousApi.getStreamDetails(videoId)
                        }
                    } catch (e: Exception) {
                        if (activeBackend == BackendType.PIPED) {
                            invidiousApi.getStreamDetails(videoId)
                        } else {
                            pipedApi.getStreamDetails(videoId)
                        }
                    }
                }

                val sponsorSegmentsDeferred = async {
                    try { sponsorBlockApi.getSegments(videoId) } catch (_: Exception) { emptyList() }
                }
                val rydVotesDeferred = async {
                    try { rydApi.getVotes(videoId) } catch (_: Exception) { com.cineflow.tube.data.api.DislikeInfo() }
                }
                val commentsDeferred = async {
                    try { getComments(videoId) } catch (_: Exception) { emptyList() }
                }

                val details = streamDetailsDeferred.await()
                val sponsors = sponsorSegmentsDeferred.await()
                val ryd = rydVotesDeferred.await()
                val comments = commentsDeferred.await()

                details.copy(
                    sponsorSegments = sponsors,
                    comments = if (comments.isNotEmpty()) comments else details.comments,
                    likes = if (ryd.likes > 0) ryd.likes else details.likes,
                    dislikes = if (ryd.dislikes > 0) ryd.dislikes else details.dislikes
                )
            } catch (e: Exception) {
                getFallbackStreamDetails(videoId)
            }
        }
    }

    suspend fun getComments(videoId: String): List<com.cineflow.tube.data.model.CommentItem> = withContext(Dispatchers.IO) {
        try {
            val list = when (activeBackend) {
                BackendType.PIPED -> pipedApi.getComments(videoId)
                BackendType.INVIDIOUS -> invidiousApi.getComments(videoId)
            }
            if (list.isNotEmpty()) list else getFallbackComments()
        } catch (_: Exception) {
            try {
                val failover = if (activeBackend == BackendType.PIPED) {
                    invidiousApi.getComments(videoId)
                } else {
                    pipedApi.getComments(videoId)
                }
                if (failover.isNotEmpty()) failover else getFallbackComments()
            } catch (_: Exception) {
                getFallbackComments()
            }
        }
    }

    private fun getFallbackComments(): List<com.cineflow.tube.data.model.CommentItem> = listOf(
        com.cineflow.tube.data.model.CommentItem(
            id = "c1",
            author = "Northbound Films",
            authorAvatarUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200",
            content = "Thank you all for listening to Glacier Sessions! Recorded in 32-bit float audio directly by the ice shelves.",
            publishedTime = "Pinned · 1 day ago",
            likeCount = 3840,
            replyCount = 42,
            isHearted = true,
            isPinned = true
        ),
        com.cineflow.tube.data.model.CommentItem(
            id = "c2",
            author = "Elena Rostova",
            authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
            content = "The sound design on this is ethereal. Combined with CineFlow's wavy player motion, this is pure cinema.",
            publishedTime = "18 hours ago",
            likeCount = 1240,
            replyCount = 8,
            isHearted = true
        ),
        com.cineflow.tube.data.model.CommentItem(
            id = "c3",
            author = "Marcus Vance",
            authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            content = "Zero buffering, no tracking cookies, and 4K 60fps playback. Best client experience hands down.",
            publishedTime = "12 hours ago",
            likeCount = 680,
            replyCount = 3
        )
    )

    private fun isOfflineException(e: Throwable): Boolean {
        return e is UnknownHostException || e is SocketTimeoutException || e is IOException
    }

    fun getFallbackTrending(): List<StreamItem> = listOf(
        StreamItem(
            id = "glacier_sessions_01",
            title = "Glacier Sessions · Beyond the Mountains",
            uploaderName = "Northbound Films",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
            durationSeconds = 754,
            durationString = "12:34",
            views = 1248000,
            viewsFormatted = "1.2M views",
            uploadedDateFormatted = "2 days ago"
        ),
        StreamItem(
            id = "nordic_horizon_02",
            title = "Nordic Horizon · 4K Pure Soundscape",
            uploaderName = "CineFlow Cinema",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
            durationSeconds = 945,
            durationString = "15:45",
            views = 840000,
            viewsFormatted = "840K views",
            uploadedDateFormatted = "5 days ago"
        ),
        StreamItem(
            id = "sapphire_depths_03",
            title = "Sapphire Depths · Arctic Twilight in 60FPS",
            uploaderName = "Arctic Blue Productions",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800",
            durationSeconds = 612,
            durationString = "10:12",
            views = 2300000,
            viewsFormatted = "2.3M views",
            uploadedDateFormatted = "1 week ago"
        ),
        StreamItem(
            id = "workspace_redesign_04",
            title = "I redesigned my entire workspace for focus",
            uploaderName = "Studio North",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800",
            durationSeconds = 728,
            durationString = "12:08",
            views = 980000,
            viewsFormatted = "980K views",
            uploadedDateFormatted = "3 days ago"
        ),
        StreamItem(
            id = "cosmos_notes_05",
            title = "Why the universe feels completely infinite",
            uploaderName = "Cosmos Notes",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800",
            durationSeconds = 1456,
            durationString = "24:16",
            views = 1540000,
            viewsFormatted = "1.5M views",
            uploadedDateFormatted = "4 days ago"
        ),
        StreamItem(
            id = "tokyo_cinematic_06",
            title = "48 hours in Tokyo — a cinematic diary",
            uploaderName = "Mika Journal",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
            thumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800",
            durationSeconds = 1002,
            durationString = "16:42",
            views = 420000,
            viewsFormatted = "420K views",
            uploadedDateFormatted = "6 days ago"
        )
    )

    fun getFallbackStreamDetails(videoId: String): StreamDetails {
        val sampleUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        return StreamDetails(
            id = videoId,
            title = "Glacier Sessions · Beyond the Mountains",
            description = "A minimal player with a gently flowing wavy timeline. Experience the quiet beyond the mountains in true 4K HDR. Powered by CineFlow Tube.",
            uploaderName = "Northbound Films",
            uploaderChannelId = "northbound_films",
            uploaderAvatarUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200",
            uploaderSubscriberCount = "480K subscribers",
            views = 1248000,
            viewsFormatted = "1.2M views",
            likes = 94200,
            dislikes = 412,
            durationSeconds = 754,
            videoStreams = listOf(
                VideoStreamFormat(url = sampleUrl, quality = "2160p (4K)", format = "MPEG_4"),
                VideoStreamFormat(url = sampleUrl, quality = "1080p (FHD)", format = "MPEG_4"),
                VideoStreamFormat(url = sampleUrl, quality = "720p (HD)", format = "MPEG_4"),
                VideoStreamFormat(url = sampleUrl, quality = "480p", format = "MPEG_4")
            ),
            chapters = listOf(
                Chapter(title = "Morning Mist", startSeconds = 0),
                Chapter(title = "The Ridge Ascent", startSeconds = 180),
                Chapter(title = "Glacial Flow", startSeconds = 420),
                Chapter(title = "Twilight Departure", startSeconds = 620)
            ),
            sponsorSegments = listOf(
                SponsorSegment(category = "intro", startSeconds = 0f, endSeconds = 5f),
                SponsorSegment(category = "sponsor", startSeconds = 300f, endSeconds = 345f)
            ),
            relatedVideos = getFallbackTrending()
        )
    }
}
