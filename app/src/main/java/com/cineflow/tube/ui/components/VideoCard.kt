package com.cineflow.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * Universal CineFlow 16:9 Video Card matching jadzumi_stream_v02.html:
 * - Fixed 16:9 aspect ratio reserved space to prevent layout shift during thumbnail load
 * - 0.75dp specular hairline rim
 * - High-contrast duration badge (#D909121C)
 * - Channel avatar (37dp) + Verification tick (✓)
 * - Accessible 48dp minimum touch bounds
 */
@Composable
fun VideoCard(
    item: StreamItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreOptionsClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CineFlow.tokens.cardRadius))
            .springPress(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 16:9 Thumbnail Box with Specular Rim & Duration Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(CineFlow.tokens.cardRadius))
                .background(CineFlow.tokens.soft)
                .specularBorder(RoundedCornerShape(CineFlow.tokens.cardRadius))
        ) {
            SubcomposeAsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CineFlow.tokens.soft)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CineFlow.tokens.soft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            color = CineFlow.tokens.muted,
                            fontSize = 32.sp
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Duration Badge (#09121cbd)
            if (item.durationString.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xD909121C))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.durationString,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        // Metadata Row: Avatar + Title & Details + Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar with touch target
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = onChannelClick != null) { onChannelClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                if (!item.uploaderAvatarUrl.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = item.uploaderAvatarUrl,
                        contentDescription = item.uploaderName,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(37.dp)
                                    .clip(CircleShape)
                                    .background(CineFlow.tokens.soft)
                            )
                        },
                        error = {
                            ChannelInitialFallback(name = item.uploaderName)
                        },
                        modifier = Modifier
                            .size(37.dp)
                            .clip(CircleShape)
                            .specularBorder(CircleShape)
                    )
                } else {
                    ChannelInitialFallback(name = item.uploaderName)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Channel Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineFlow.tokens.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = buildString {
                    append(item.uploaderName)
                    append(" ✓")
                    if (item.viewsFormatted.isNotEmpty()) {
                        append(" · ")
                        append(item.viewsFormatted)
                    }
                    if (item.uploadedDateFormatted.isNotEmpty()) {
                        append(" · ")
                        append(item.uploadedDateFormatted)
                    }
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = CineFlow.tokens.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onMoreOptionsClick != null) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                        .clip(CircleShape)
                        .clickable(onClick = onMoreOptionsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = CineFlow.tokens.muted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelInitialFallback(name: String) {
    Box(
        modifier = Modifier
            .size(37.dp)
            .clip(CircleShape)
            .background(CineFlow.tokens.soft)
            .specularBorder(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val initial = name.firstOrNull()?.uppercase() ?: "▶"
        Text(
            text = initial,
            color = CineFlow.tokens.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
