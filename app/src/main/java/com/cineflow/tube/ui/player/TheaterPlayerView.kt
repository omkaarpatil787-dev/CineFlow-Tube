package com.cineflow.tube.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.player.PlaybackState
import com.cineflow.tube.ui.components.AmbientBackdropGlow
import com.cineflow.tube.ui.components.VideoCard
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.delay

/**
 * Production-ready CineFlow Immersive Player (finalized from cineflow_immersive_player.html & jadzumi_stream_v02.html):
 * - "Less chrome. More cinema." philosophy
 * - Recomposition-isolated timeline progress (zero watch page thrashing)
 * - Continuous video-surface lifecycle without recreate flashes
 * - Responsive Phone (stacked) vs Tablet/Landscape (dual-pane) layouts
 * - Ambient Glacier Glow (#68CCE518) & filmic scrim vignette
 * - 2-Level Hierarchical Options Sheet ("Your viewing space")
 */
@OptIn(UnstableApi::class)
@Composable
fun TheaterPlayerView(
    onCollapseClick: () -> Unit,
    onRelatedVideoClick: (StreamItem) -> Unit,
    onCycleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by PlaybackController.state.collectAsState()
    val stream = state.currentStream ?: return

    var controlsVisible by remember { mutableStateOf(true) }
    var isOptionsSheetOpen by remember { mutableStateOf(false) }
    var initialSheetPicker by remember { mutableStateOf<PickerCategory?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var seekFeedbackDirection by remember { mutableStateOf<Boolean?>(null) }

    // Action toggles
    var isSubscribed by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    fun showToast(msg: String) {
        toastMessage = msg
    }

    // Toast auto-fade timer (1300ms)
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1300)
            toastMessage = null
        }
    }

    // Controls auto-hide timer (3000ms)
    LaunchedEffect(controlsVisible, state.isPlaying, state.autoHideControls, isOptionsSheetOpen) {
        if (controlsVisible && state.isPlaying && state.autoHideControls && !isOptionsSheetOpen) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CineFlow.tokens.bg)
    ) {
        // Dynamic Ambient Glacier Glow
        if (state.ambientGlow) {
            AmbientBackdropGlow(
                accentColor = Color(0xFF68CCE5),
                modifier = Modifier.fillMaxSize(),
                intensity = 0.24f
            )
        }

        if (state.isFullscreen) {
            // Fullscreen / Theater Mode: Player fills entire viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                PlayerSurfaceWithOverlay(
                    state = state,
                    streamTitle = stream.title,
                    streamAuthor = stream.uploaderName,
                    viewsFormatted = stream.viewsFormatted,
                    controlsVisible = controlsVisible,
                    seekFeedbackDirection = seekFeedbackDirection,
                    toastMessage = toastMessage,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onHideControls = { controlsVisible = false },
                    onBackClick = {
                        PlaybackController.setFullscreen(false)
                        showToast("Returned to player preview")
                    },
                    onToggleSubtitles = {
                        val next = !state.subtitlesEnabled
                        PlaybackController.setSubtitlesEnabled(next)
                        showToast(if (next) "Sample captions on" else "Sample captions off")
                    },
                    onOpenOptions = { picker ->
                        initialSheetPicker = picker
                        isOptionsSheetOpen = true
                    },
                    onSeekDelta = { deltaSec ->
                        val currentPos = PlaybackController.currentPositionMs.value
                        val target = currentPos + (deltaSec * 1000L)
                        PlaybackController.seekTo(target)
                        seekFeedbackDirection = deltaSec > 0
                        showToast(if (deltaSec > 0) "+$deltaSec seconds" else "−${Math.abs(deltaSec)} seconds")
                    },
                    onSeekAbsolute = { posMs ->
                        PlaybackController.seekTo(posMs)
                    },
                    onToggleFullscreen = {
                        PlaybackController.setFullscreen(false)
                        showToast("Returned to player preview")
                    },
                    onClearSeekFeedback = { seekFeedbackDirection = null }
                )
            }
        } else {
            // Responsive Watch Layout (Phone stacked vs Tablet dual-pane)
            BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                val isTabletWide = maxWidth >= 760.dp

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar (Minimize, wordmark, theme switcher)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize Button (↓)
                        Box(
                            modifier = Modifier
                                .size(CineFlow.tokens.minTouchTarget)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft)
                                .springPress(onClick = onCollapseClick),
                            contentAlignment = Alignment.Center
                        ) {
                            BackChevronIcon(tint = CineFlow.tokens.ink, size = 18.dp)
                        }

                        // Wordmark: cineflow tube
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "cineflow",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.ink
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "tube",
                                fontSize = 17.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.accent
                            )
                        }

                        // Theme Cycle Button (☼)
                        Box(
                            modifier = Modifier
                                .size(CineFlow.tokens.minTouchTarget)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft)
                                .springPress(onClick = onCycleTheme),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "☼",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.ink
                            )
                        }
                    }

                    if (isTabletWide) {
                        // TABLET DUAL-PANE LAYOUT (matches jadzumi_stream_v02.html: minmax(0,1fr) 330px)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Left Pane: Video Surface + Channel Details
                            Column(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(CineFlow.tokens.sheetRadius))
                                        .background(Color.Black)
                                ) {
                                    PlayerSurfaceWithOverlay(
                                        state = state,
                                        streamTitle = stream.title,
                                        streamAuthor = stream.uploaderName,
                                        viewsFormatted = stream.viewsFormatted,
                                        controlsVisible = controlsVisible,
                                        seekFeedbackDirection = seekFeedbackDirection,
                                        toastMessage = toastMessage,
                                        onToggleControls = { controlsVisible = !controlsVisible },
                                        onHideControls = { controlsVisible = false },
                                        onBackClick = onCollapseClick,
                                        onToggleSubtitles = {
                                            val next = !state.subtitlesEnabled
                                            PlaybackController.setSubtitlesEnabled(next)
                                            showToast(if (next) "Sample captions on" else "Sample captions off")
                                        },
                                        onOpenOptions = { picker ->
                                            initialSheetPicker = picker
                                            isOptionsSheetOpen = true
                                        },
                                        onSeekDelta = { deltaSec ->
                                            val currentPos = PlaybackController.currentPositionMs.value
                                            val target = currentPos + (deltaSec * 1000L)
                                            PlaybackController.seekTo(target)
                                            seekFeedbackDirection = deltaSec > 0
                                            showToast(if (deltaSec > 0) "+$deltaSec seconds" else "−${Math.abs(deltaSec)} seconds")
                                        },
                                        onSeekAbsolute = { posMs ->
                                            PlaybackController.seekTo(posMs)
                                        },
                                        onToggleFullscreen = {
                                            PlaybackController.setFullscreen(true)
                                            showToast("Theater preview")
                                        },
                                        onClearSeekFeedback = { seekFeedbackDirection = null }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                WatchDetailsSection(
                                    stream = stream,
                                    isSubscribed = isSubscribed,
                                    isLiked = isLiked,
                                    isSaved = isSaved,
                                    onSubscribeClick = { isSubscribed = !isSubscribed },
                                    onLikeClick = { isLiked = !isLiked },
                                    onShareClick = { showToast("Share link copied") },
                                    onSaveClick = {
                                        isSaved = !isSaved
                                        showToast(if (isSaved) "Saved to library" else "Removed from library")
                                    },
                                    onSponsorBlockClick = { showToast("SponsorBlock active") }
                                )
                            }

                            // Right Pane: Up Next Recommendation List
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Up next",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineFlow.tokens.ink
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                stream.relatedVideos.forEach { related ->
                                    VideoCard(
                                        item = related,
                                        onClick = { onRelatedVideoClick(related) }
                                    )
                                }
                            }
                        }
                    } else {
                        // PHONE STACKED LAYOUT (16:9 on top + scrollable below)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            PlayerSurfaceWithOverlay(
                                state = state,
                                streamTitle = stream.title,
                                streamAuthor = stream.uploaderName,
                                viewsFormatted = stream.viewsFormatted,
                                controlsVisible = controlsVisible,
                                seekFeedbackDirection = seekFeedbackDirection,
                                toastMessage = toastMessage,
                                onToggleControls = { controlsVisible = !controlsVisible },
                                onHideControls = { controlsVisible = false },
                                onBackClick = onCollapseClick,
                                onToggleSubtitles = {
                                    val next = !state.subtitlesEnabled
                                    PlaybackController.setSubtitlesEnabled(next)
                                    showToast(if (next) "Sample captions on" else "Sample captions off")
                                },
                                onOpenOptions = { picker ->
                                    initialSheetPicker = picker
                                    isOptionsSheetOpen = true
                                },
                                onSeekDelta = { deltaSec ->
                                    val currentPos = PlaybackController.currentPositionMs.value
                                    val target = currentPos + (deltaSec * 1000L)
                                    PlaybackController.seekTo(target)
                                    seekFeedbackDirection = deltaSec > 0
                                    showToast(if (deltaSec > 0) "+$deltaSec seconds" else "−${Math.abs(deltaSec)} seconds")
                                },
                                onSeekAbsolute = { posMs ->
                                    PlaybackController.seekTo(posMs)
                                },
                                onToggleFullscreen = {
                                    PlaybackController.setFullscreen(true)
                                    showToast("Theater preview")
                                },
                                onClearSeekFeedback = { seekFeedbackDirection = null }
                            )
                        }

                        // Scrollable Details Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            WatchDetailsSection(
                                stream = stream,
                                isSubscribed = isSubscribed,
                                isLiked = isLiked,
                                isSaved = isSaved,
                                onSubscribeClick = { isSubscribed = !isSubscribed },
                                onLikeClick = { isLiked = !isLiked },
                                onShareClick = { showToast("Share link copied") },
                                onSaveClick = {
                                    isSaved = !isSaved
                                    showToast(if (isSaved) "Saved to library" else "Removed from library")
                                },
                                onSponsorBlockClick = { showToast("SponsorBlock active") }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Up Next recommendations
                            if (stream.relatedVideos.isNotEmpty()) {
                                Text(
                                    text = "Up next",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineFlow.tokens.ink
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                stream.relatedVideos.forEach { related ->
                                    VideoCard(
                                        item = related,
                                        onClick = { onRelatedVideoClick(related) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Options Sheet ("Your viewing space")
        CineFlowOptionsSheet(
            isOpen = isOptionsSheetOpen,
            playbackState = state,
            onDismiss = { isOptionsSheetOpen = false },
            onToast = { showToast(it) },
            initialPicker = initialSheetPicker
        )
    }
}

/**
 * Isolated Watch Details (Title, Creator Row, Action Pills, Description)
 * Completely decoupled from the 200ms progress ticks!
 */
@Composable
private fun WatchDetailsSection(
    stream: com.cineflow.tube.data.model.StreamDetails,
    isSubscribed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    onSubscribeClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSponsorBlockClick: () -> Unit
) {
    Column {
        // Video Title
        Text(
            text = stream.title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = CineFlow.tokens.ink,
            lineHeight = 25.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Channel Row: Avatar + Name/Subs + Subscribe Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!stream.uploaderAvatarUrl.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = stream.uploaderAvatarUrl,
                        contentDescription = stream.uploaderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .specularBorder(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CineFlow.tokens.soft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stream.uploaderName.firstOrNull()?.uppercase() ?: "▶",
                            color = CineFlow.tokens.accent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stream.uploaderName,
                        color = CineFlow.tokens.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (stream.uploaderSubscriberCount.isNotEmpty()) stream.uploaderSubscriberCount else "Creator · Concept",
                        color = CineFlow.tokens.muted,
                        fontSize = 12.sp
                    )
                }
            }

            // Subscribe Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isSubscribed) CineFlow.tokens.soft else CineFlow.tokens.ink)
                    .springPress(onClick = onSubscribeClick)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Text(
                    text = if (isSubscribed) "Subscribed ✓" else "Subscribe",
                    color = if (isSubscribed) CineFlow.tokens.ink else CineFlow.tokens.bg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Pill Row: ♡ Like, ↗ Share, ＋ Save, ✦ SponsorBlock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionPill(
                text = if (isLiked) "♥ Liked" else "♡ Like",
                isSelected = isLiked,
                onClick = onLikeClick
            )

            ActionPill(
                text = "↗ Share",
                onClick = onShareClick
            )

            ActionPill(
                text = if (isSaved) "✓ Saved" else "＋ Save",
                isSelected = isSaved,
                onClick = onSaveClick
            )

            ActionPill(
                text = "✦ SponsorBlock",
                isSelected = true,
                onClick = onSponsorBlockClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(CineFlow.tokens.soft)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "${stream.viewsFormatted} · ${stream.uploadedDateFormatted.ifEmpty { "Recently uploaded" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CineFlow.tokens.ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stream.description.ifEmpty {
                        "Explore the world through a more beautiful viewing experience. Powered by CineFlow Tube."
                    },
                    fontSize = 13.sp,
                    color = CineFlow.tokens.ink.copy(alpha = 0.88f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Encapsulated Video Surface with Overlay Chrome directly following cineflow_immersive_player.html
 */
@Composable
private fun PlayerSurfaceWithOverlay(
    state: PlaybackState,
    streamTitle: String,
    streamAuthor: String,
    viewsFormatted: String,
    controlsVisible: Boolean,
    seekFeedbackDirection: Boolean?,
    toastMessage: String?,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    onBackClick: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenOptions: (PickerCategory?) -> Unit,
    onSeekDelta: (Int) -> Unit,
    onSeekAbsolute: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onClearSeekFeedback: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            onSeekDelta(-10)
                        } else {
                            onSeekDelta(10)
                        }
                    },
                    onLongPress = {
                        PlaybackController.setHoldTo2x(true)
                    }
                )
            }
    ) {
        // 1. ExoPlayer Surface View (Persistent attachment)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = PlaybackController.player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != PlaybackController.player) {
                    playerView.player = PlaybackController.player
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Buffering Spinner
        if (state.isBuffering) {
            CircularProgressIndicator(
                color = Color(0xFFA4EAFF),
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.Center)
            )
        }

        // 3. Double-Tap Seek Feedback Ripple
        if (seekFeedbackDirection != null) {
            Box(
                modifier = Modifier
                    .align(if (seekFeedbackDirection == true) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 28.dp)
            ) {
                SeekFeedbackOverlay(
                    isForward = seekFeedbackDirection == true,
                    onAnimationEnd = onClearSeekFeedback
                )
            }
        }

        // 4. Toast Overlay (Centered at 25% height)
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE80A2436))
                    .specularBorder(RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = toastMessage ?: "",
                    color = Color(0xFFE6F5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 5. Captions Subtitle Capsule
        if (state.subtitlesEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-74).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xC900101E))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "The quiet beyond the mountains.",
                    color = Color(0xFFE6F5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 6. Immersive UI Controls Layer
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x8C031019),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xCF020B16)
                            )
                        )
                    )
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerIconButton(onClick = onBackClick) {
                        BackChevronIcon()
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = streamTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFFE6F5FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$streamAuthor · $viewsFormatted",
                            fontSize = 10.sp,
                            color = Color(0xFFC1D8E2),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    PlayerIconButton(
                        onClick = onToggleSubtitles,
                        isActive = state.subtitlesEnabled
                    ) {
                        CaptionsIcon(tint = if (state.subtitlesEnabled) Color(0xFFA4EAFF) else Color(0xFFF0FBFF))
                    }

                    PlayerIconButton(onClick = { onOpenOptions(null) }) {
                        MoreDotsIcon()
                    }
                }

                // CENTER CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    RotatableSeekButton(
                        isForward = false,
                        onClick = { onSeekDelta(-10) }
                    )

                    MorphingPlayPauseButton(
                        isPlaying = state.isPlaying,
                        onClick = { PlaybackController.togglePlayPause() },
                        size = 76.dp,
                        backgroundColor = Color(0x65123646)
                    )

                    RotatableSeekButton(
                        isForward = true,
                        onClick = { onSeekDelta(10) }
                    )
                }

                // BOTTOM BAR: Timeline + Chips (Isolated Progress Observation)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Isolated Trackrow Progress Row
                    TrackrowProgressRow(
                        durationMs = state.durationMs,
                        isPlaying = state.isPlaying,
                        timelineStyle = state.timelineStyle,
                        onSeek = onSeekAbsolute
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Quality & Speed Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlayerPillChip(
                                label = state.selectedQuality,
                                onClick = { onOpenOptions(PickerCategory.QUALITY) }
                            )

                            PlayerPillChip(
                                label = "${state.playbackSpeed.let { if (it % 1 == 0f) it.toInt() else it }}×",
                                onClick = { onOpenOptions(PickerCategory.SPEED) }
                            )
                        }

                        // Right: Hide Controls & Fullscreen
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlayerIconButton(
                                onClick = onHideControls,
                                size = 42.dp
                            ) {
                                HideChevronDownIcon(size = 20.dp)
                            }

                            PlayerIconButton(
                                onClick = onToggleFullscreen,
                                size = 42.dp
                            ) {
                                FullscreenCornersIcon(size = 20.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Isolated Progress Row:
 * ONLY this composable function observes PlaybackController.currentPositionMs!
 * This prevents the entire watch page, title, channel info, and up next list from
 * recomposing on every 200ms progress tick.
 */
@Composable
private fun TrackrowProgressRow(
    durationMs: Long,
    isPlaying: Boolean,
    timelineStyle: com.cineflow.tube.player.TimelineStyle,
    onSeek: (Long) -> Unit
) {
    val currentPositionMs by PlaybackController.currentPositionMs.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = formatClockTime(currentPositionMs),
            color = Color(0xFFDCF8FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        WavyProgressTimeline(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            timelineStyle = timelineStyle,
            onSeek = onSeek,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatClockTime(durationMs),
            color = Color(0xFFC1D8E2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PlayerPillChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x2B122735))
            .springPress(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD9EDF8)
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isSelected) CineFlow.tokens.ink else CineFlow.tokens.soft)
            .springPress(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CineFlow.tokens.bg else CineFlow.tokens.ink
        )
    }
}

private fun formatClockTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
