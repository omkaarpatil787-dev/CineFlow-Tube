package com.cineflow.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * Reusable Frosted Channel Details Sheet
 * Surfaces channel avatar, subscriber count, bio, subscribe pill, and recent uploads.
 */
@Composable
fun ChannelDetailSheet(
    channelName: String,
    avatarUrl: String? = null,
    subscriberCount: String = "480K subscribers",
    description: String = "Cinema-grade 4K captures and atmospheric visual experiences.",
    videos: List<StreamItem> = emptyList(),
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var isSubscribed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73000000))
        )

        // Sheet Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(topStart = CineFlow.tokens.sheetRadius, topEnd = CineFlow.tokens.sheetRadius))
                .background(CineFlow.tokens.glass)
                .specularBorder(RoundedCornerShape(topStart = CineFlow.tokens.sheetRadius, topEnd = CineFlow.tokens.sheetRadius))
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Avatar, Names, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft)
                                .specularBorder(CircleShape)
                        ) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                SubcomposeAsyncImage(
                                    model = avatarUrl,
                                    contentDescription = channelName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channelName.firstOrNull()?.uppercase() ?: "▶",
                                        color = CineFlow.tokens.accent,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channelName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineFlow.tokens.ink
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✓",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineFlow.tokens.accent
                                )
                            }
                            Text(
                                text = subscriberCount,
                                fontSize = 12.sp,
                                color = CineFlow.tokens.muted
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CineFlow.tokens.soft)
                            .springPress(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = CineFlow.tokens.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bio Description & Subscribe Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = CineFlow.tokens.muted,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(25.dp))
                            .background(if (isSubscribed) CineFlow.tokens.soft else CineFlow.tokens.ink)
                            .springPress { isSubscribed = !isSubscribed }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed ✓" else "Subscribe",
                            color = if (isSubscribed) CineFlow.tokens.ink else CineFlow.tokens.bg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Uploads",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineFlow.tokens.ink
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Uploads list
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(videos, key = { it.id }) { video ->
                        VideoCard(
                            item = video,
                            onClick = {
                                onDismiss()
                                onVideoClick(video)
                            }
                        )
                    }
                }
            }
        }
    }
}
