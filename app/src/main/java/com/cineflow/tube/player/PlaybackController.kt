package com.cineflow.tube.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cineflow.tube.data.model.StreamDetails
import com.cineflow.tube.data.model.StreamItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * CineFlow Playback Controller
 * Manages the single ExoPlayer instance, foreground playback service bridge,
 * fine-grained progress tracking isolation (zero recomposition thrashing),
 * and SponsorBlock automatic segment skipping.
 */
object PlaybackController {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    // Fine-grained progress tracker: Isolated from _state so ticking every 200ms
    // does NOT trigger recomposition waterfalls across the entire watch page!
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    val player: ExoPlayer?
        get() = exoPlayer

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressTrackerJob: Job? = null
    private var preHoldSpeed: Float = 1.0f

    var autoSkipSponsors: Boolean = true

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            _state.value = _state.value.copy(
                isBuffering = isBuffering,
                durationMs = duration
            )

            // Auto-advance queue when current video ends
            if (playbackState == Player.STATE_ENDED) {
                val nextItem = QueueManager.advanceToNext()
                if (nextItem != null) {
                    _onQueueAdvance?.invoke(nextItem)
                }
            }
        }
    }

    /**
     * Callback invoked when the queue auto-advances to the next stream.
     * The host (MainActivity / TheaterPlayerView) must set this to trigger
     * a new stream load without restarting the player engine from scratch.
     */
    var _onQueueAdvance: ((StreamItem) -> Unit)? = null

    fun attachPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.addListener(playerListener)
        player.volume = _state.value.volume
        player.repeatMode = if (_state.value.repeatVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(_state.value.playbackSpeed)
    }

    fun detachPlayer() {
        stopProgressTracker()
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
    }

    fun play(details: StreamDetails, preferredQuality: String = "1080p", audioOnly: Boolean = false) {
        val p = exoPlayer ?: return

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(details.title)
            .setArtist(details.uploaderName)
            .setArtworkUri(details.uploaderAvatarUrl?.let { Uri.parse(it) })
            .build()

        val mediaItem = buildMediaItem(details, preferredQuality, audioOnly, mediaMetadata)

        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true

        val streamQualities = details.videoStreams.map { it.quality }.distinct()
        val qualities = if (streamQualities.isNotEmpty()) {
            listOf("Auto") + streamQualities
        } else {
            listOf("Auto", "2160p (4K)", "1440p (2K)", "1080p (FHD)", "720p (HD)", "480p", "360p")
        }

        _currentPositionMs.value = 0L

        _state.value = _state.value.copy(
            currentStream = details,
            selectedQuality = if (audioOnly) "Audio Only" else preferredQuality,
            availableQualities = qualities,
            isAudioOnly = audioOnly,
            durationMs = details.durationSeconds * 1000L
        )
    }

    private fun buildMediaItem(
        details: StreamDetails,
        preferredQuality: String,
        audioOnly: Boolean,
        metadata: MediaMetadata
    ): MediaItem {
        if (audioOnly) {
            val bestAudio = details.audioStreams.maxByOrNull { it.bitrate }
            val streamUrl = bestAudio?.url ?: details.audioStreams.firstOrNull()?.url ?: ""
            return MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()
        }

        // Try HLS stream first for adaptive bitrates
        if (!details.hlsUrl.isNullOrEmpty()) {
            return MediaItem.Builder()
                .setUri(details.hlsUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setMediaMetadata(metadata)
                .build()
        }

        // Try DASH manifest
        if (!details.dashUrl.isNullOrEmpty()) {
            return MediaItem.Builder()
                .setUri(details.dashUrl)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .setMediaMetadata(metadata)
                .build()
        }

        // Progressive video stream
        val selectedFormat = details.videoStreams.find { it.quality.contains(preferredQuality.substringBefore(" ")) }
            ?: details.videoStreams.firstOrNull()

        val targetUrl = selectedFormat?.url ?: ""
        return MediaItem.Builder()
            .setUri(targetUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Play a locally downloaded file directly from disk.
     * Bypasses all API/stream URL resolution — no network required.
     * Used when a completed DownloadItem is tapped in YourSpaceScreen.
     */
    fun playOffline(
        localFilePath: String,
        title: String,
        uploaderName: String,
        durationSeconds: Long = 0L,
        thumbnailUrl: String? = null
    ) {
        val p = exoPlayer ?: return

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(uploaderName)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(localFilePath)
            .setMediaMetadata(metadata)
            .build()

        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true

        _currentPositionMs.value = 0L

        _state.value = _state.value.copy(
            currentStream = StreamDetails(
                id = "offline_${localFilePath.hashCode()}",
                title = title,
                description = "Playing offline from device storage",
                uploaderName = uploaderName,
                uploaderChannelId = "",
                durationSeconds = durationSeconds,
                uploaderAvatarUrl = thumbnailUrl
            ),
            selectedQuality = "Offline",
            availableQualities = listOf("Offline"),
            isAudioOnly = false,
            durationMs = durationSeconds * 1000L
        )
    }

    fun togglePlayPause() {
        val p = exoPlayer ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        val p = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, p.duration.coerceAtLeast(0L))
        p.seekTo(clamped)
        _currentPositionMs.value = clamped
    }

    fun setSpeed(speed: Float) {
        val p = exoPlayer
        p?.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun setHoldTo2x(active: Boolean) {
        val p = exoPlayer ?: return
        if (active) {
            preHoldSpeed = p.playbackParameters.speed
            p.setPlaybackSpeed(2.0f)
            _state.value = _state.value.copy(isHoldTo2xActive = true)
        } else {
            p.setPlaybackSpeed(preHoldSpeed)
            _state.value = _state.value.copy(isHoldTo2xActive = false)
        }
    }

    fun setQuality(quality: String) {
        _state.value = _state.value.copy(selectedQuality = quality)
        val stream = _state.value.currentStream ?: return
        val currentPos = _currentPositionMs.value
        val isCurrentlyPlaying = _state.value.isPlaying

        exoPlayer?.let { p ->
            val metadata = p.mediaMetadata
            val mediaItem = buildMediaItem(stream, quality, _state.value.isAudioOnly, metadata)
            p.setMediaItem(mediaItem, currentPos)
            p.prepare()
            if (isCurrentlyPlaying) p.play()
        }
    }

    fun setAudioTrack(track: String) {
        _state.value = _state.value.copy(selectedAudioTrack = track)
    }

    fun setTimelineStyle(style: TimelineStyle) {
        _state.value = _state.value.copy(timelineStyle = style)
    }

    fun setAutoHideControls(enabled: Boolean) {
        _state.value = _state.value.copy(autoHideControls = enabled)
    }

    fun setRepeatVideo(repeat: Boolean) {
        _state.value = _state.value.copy(repeatVideo = repeat)
        exoPlayer?.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun setAmbientGlow(enabled: Boolean) {
        _state.value = _state.value.copy(ambientGlow = enabled)
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(
            subtitlesEnabled = enabled,
            selectedSubtitle = if (enabled) "en" else "off"
        )
    }

    fun setSelectedSubtitle(sub: String) {
        val enabled = sub != "off"
        _state.value = _state.value.copy(
            subtitlesEnabled = enabled,
            selectedSubtitle = sub
        )
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        exoPlayer?.volume = clamped
    }

    fun toggleFullscreen() {
        _state.value = _state.value.copy(isFullscreen = !_state.value.isFullscreen)
    }

    fun setFullscreen(fullscreen: Boolean) {
        _state.value = _state.value.copy(isFullscreen = fullscreen)
    }

    fun resetPreferences() {
        setQuality("1080p")
        setSpeed(1.0f)
        setAudioTrack("Original · Stereo")
        setSubtitlesEnabled(false)
        setAutoHideControls(true)
        setRepeatVideo(false)
        setAmbientGlow(true)
        setVolume(0.8f)
        setTimelineStyle(TimelineStyle.WAVY)
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (isActive) {
                val p = exoPlayer
                if (p != null && p.isPlaying) {
                    val pos = p.currentPosition
                    val dur = p.duration.coerceAtLeast(0L)

                    // 1. Fine-grained position emission (does NOT trigger watch page recomposition)
                    _currentPositionMs.value = pos

                    // 2. Chapter detection
                    val currentStream = _state.value.currentStream
                    val activeChap = currentStream?.chapters?.lastOrNull {
                        pos >= it.startSeconds * 1000L
                    }

                    // 3. SponsorBlock skipping
                    var skippedCategory: String? = null
                    if (autoSkipSponsors && currentStream != null) {
                        for (segment in currentStream.sponsorSegments) {
                            val startMs = (segment.startSeconds * 1000).toLong()
                            val endMs = (segment.endSeconds * 1000).toLong()
                            if (pos in startMs until endMs) {
                                p.seekTo(endMs + 200)
                                skippedCategory = segment.category
                                break
                            }
                        }
                    }

                    // Only update _state if duration, chapter or sponsor segment changed
                    if (_state.value.durationMs != dur ||
                        _state.value.activeChapter != activeChap ||
                        _state.value.skippedSponsorCategory != skippedCategory
                    ) {
                        _state.value = _state.value.copy(
                            durationMs = dur,
                            activeChapter = activeChap,
                            skippedSponsorCategory = skippedCategory
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }
}
