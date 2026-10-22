package com.cineflow.tube.player

import com.cineflow.tube.data.model.Chapter
import com.cineflow.tube.data.model.SponsorSegment
import com.cineflow.tube.data.model.StreamDetails
import com.cineflow.tube.data.model.VideoStreamFormat

enum class TimelineStyle(val displayName: String) {
    WAVY("Wavy line · reference"),
    CIRCULAR_FLOWER("Wavy + circular"),
    MINIMAL_STRAIGHT("Minimal straight")
}

data class PlaybackState(
    val currentStream: StreamDetails? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val isHoldTo2xActive: Boolean = false,
    val selectedQuality: String = "1080p",
    val availableQualities: List<String> = listOf("Auto", "2160p", "1440p", "1080p", "720p", "480p", "360p"),
    val selectedAudioTrack: String = "Original · Stereo",
    val availableAudioTracks: List<String> = listOf("Original · Stereo", "English · Stereo"),
    val isAudioOnly: Boolean = false,
    val activeChapter: Chapter? = null,
    val skippedSponsorCategory: String? = null,

    // CineFlow Immersive Player Preferences (cineflow_immersive_player.html)
    val timelineStyle: TimelineStyle = TimelineStyle.WAVY,
    val autoHideControls: Boolean = true,
    val repeatVideo: Boolean = false,
    val ambientGlow: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val selectedSubtitle: String = "off",
    val volume: Float = 0.8f,
    val isFullscreen: Boolean = false
)
