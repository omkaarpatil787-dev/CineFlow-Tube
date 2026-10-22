package com.cineflow.tube.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.ui.components.FrostedSurface
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class MiniplayerMode {
    EXPANDED,
    COLLAPSED,
    HIDDEN
}

/**
 * CineFlow Miniplayer directly matching jadzumi_stream_v02.html `.mini`:
 * - 54x88dp thumbnail preview
 * - Adaptive layout: 370dp docked on bottom-right on tablets, full width on phones
 * - Horizontal swipe-to-dismiss gesture
 * - Preserves ExoPlayer instance without recreation
 */
@Composable
fun CineFlowMiniplayerContainer(
    mode: MiniplayerMode,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onRelatedVideoClick: (StreamItem) -> Unit,
    onCycleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by PlaybackController.state.collectAsState()
    val stream = state.currentStream ?: return
    val coroutineScope = rememberCoroutineScope()

    if (mode == MiniplayerMode.HIDDEN) return

    val isExpanded = mode == MiniplayerMode.EXPANDED
    val swipeOffsetX = remember { Animatable(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isExpanded) {
            TheaterPlayerView(
                onCollapseClick = onCollapse,
                onRelatedVideoClick = onRelatedVideoClick,
                onCycleTheme = onCycleTheme,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Floating Docked Miniplayer Capsule (Responsive placement)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTabletWide = maxWidth >= 760.dp

                val capsuleModifier = if (isTabletWide) {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 24.dp)
                        .width(370.dp)
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 78.dp, start = 10.dp, end = 10.dp)
                        .fillMaxWidth()
                }

                Box(
                    modifier = capsuleModifier
                        .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                        .draggable(
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    swipeOffsetX.snapTo(swipeOffsetX.value + delta)
                                }
                            },
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                if (Math.abs(swipeOffsetX.value) > 200f || Math.abs(velocity) > 1000f) {
                                    val target = if (swipeOffsetX.value > 0) 1000f else -1000f
                                    swipeOffsetX.animateTo(target, tween(200))
                                    onDismiss()
                                    PlaybackController.pause()
                                    swipeOffsetX.snapTo(0f)
                                } else {
                                    swipeOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                }
                            }
                        )
                ) {
                    FrostedSurface(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = CineFlow.tokens.glass
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .springPress(onClick = onExpand)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Miniature Preview Box (54x88dp)
                                Box(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(CineFlow.tokens.soft)
                                        .specularBorder(RoundedCornerShape(11.dp))
                                ) {
                                    SubcomposeAsyncImage(
                                        model = stream.thumbnailUrl,
                                        contentDescription = stream.title,
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize().background(CineFlow.tokens.soft))
                                        },
                                        error = {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(CineFlow.tokens.soft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("▶", color = CineFlow.tokens.muted, fontSize = 20.sp)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Title & Subtitle
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stream.title,
                                        color = CineFlow.tokens.ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (state.isPlaying) stream.uploaderName else "Preview paused",
                                        color = CineFlow.tokens.muted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Expand Button (⛶)
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                                        .springPress(onClick = onExpand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Expand",
                                        tint = CineFlow.tokens.ink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Close Button (✕)
                                Box(
                                    modifier = Modifier
                                        .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                                        .springPress {
                                            PlaybackController.pause()
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = CineFlow.tokens.muted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Progress Indicator (isolated progress observation)
                            MiniplayerProgressBar(durationMs = state.durationMs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniplayerProgressBar(durationMs: Long) {
    val currentPositionMs by PlaybackController.currentPositionMs.collectAsState()
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LinearProgressIndicator(
        progress = { progress },
        color = CineFlow.tokens.accent,
        trackColor = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    )
}
