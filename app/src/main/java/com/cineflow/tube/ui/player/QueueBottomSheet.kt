package com.cineflow.tube.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.player.QueueManager
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * CineFlow Queue Bottom Sheet (Phase 3)
 * ----------------------------------------
 * Displays the current playback queue with:
 * - Highlighted "Now Playing" row
 * - Tap any item to jump to it immediately
 * - Remove items with the × button
 * - AutoPlay toggle
 * - "Up Next" preview for queue item +1
 *
 * Visual language: matches CommentsBottomSheet (frosted #0B2031F5 glass,
 * specular border, 78% height, 24dp rounded corners).
 */
@Composable
fun QueueBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onJumpTo: (StreamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val queue by QueueManager.queue.collectAsState()
    val queueIndex by QueueManager.queueIndex.collectAsState()
    val autoPlay by QueueManager.autoPlay.collectAsState()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = queueIndex.coerceAtLeast(0))

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        // Scrim
        Box(modifier = Modifier.fillMaxSize().background(Color(0x73000000)))

        // Sheet
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xF50B2031))
                .specularBorder(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Queue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6F5FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${queue.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA4EAFF)
                        )
                    }

                    // AutoPlay toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "AutoPlay",
                            tint = if (autoPlay) Color(0xFFA4EAFF) else Color(0xFF4A7090),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = autoPlay,
                            onCheckedChange = { QueueManager.setAutoPlay(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFA4EAFF),
                                checkedTrackColor = Color(0x4A4EAFF).copy(alpha = 0.38f),
                                uncheckedThumbColor = Color(0xFF4A7090),
                                uncheckedTrackColor = Color(0x22FFFFFF)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x38102D3C))
                                .springPress(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Queue",
                                tint = Color(0xFFF0FBFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (queue.isEmpty()) {
                    // Empty state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF4A7090),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Queue is empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A98B4)
                            )
                            Text(
                                text = "Long-press any video to add it here",
                                fontSize = 12.sp,
                                color = Color(0xFF4A7090)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                            QueueItemRow(
                                item = item,
                                isPlaying = index == queueIndex,
                                onTap = {
                                    QueueManager.jumpTo(index)
                                    onJumpTo(item)
                                    onDismiss()
                                },
                                onRemove = {
                                    QueueManager.removeFromQueue(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    item: StreamItem,
    isPlaying: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isPlaying) Color(0x33A4EAFF) else Color(0x16FFFFFF),
        animationSpec = spring(),
        label = "queueItemBg"
    )
    val borderRadius by animateDpAsState(
        targetValue = if (isPlaying) 14.dp else 10.dp,
        animationSpec = spring(),
        label = "queueItemRadius"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(borderRadius))
            .background(bgColor)
            .springPress(onClick = onTap)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle (visual indicator only – full drag reorder can be added with LazyList drag)
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Reorder",
            tint = Color(0xFF4A7090),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 45.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D2030))
        ) {
            SubcomposeAsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Now playing indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Now playing",
                        tint = Color(0xFFA4EAFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) Color(0xFFE6F5FF) else Color(0xFFB8D8EE),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Text(
                text = item.uploaderName,
                fontSize = 11.sp,
                color = Color(0xFF6A98B4),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Remove button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x22FF5277))
                .springPress(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove from queue",
                tint = Color(0xFFFF8FA8),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
