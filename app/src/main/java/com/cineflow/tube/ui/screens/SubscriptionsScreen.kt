package com.cineflow.tube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.components.CineFlowEmptyView
import com.cineflow.tube.ui.components.VideoCard
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * CineFlow Subscriptions Screen:
 * - Creator stories avatar strip with specular active rim
 * - Video feed of subscribed creators
 * - Private, local storage with zero Google account connection
 * - Reusable empty state with discovery action
 */
@Composable
fun SubscriptionsScreen(
    listState: LazyListState,
    subscribedVideos: List<StreamItem> = emptyList(),
    onVideoClick: (StreamItem) -> Unit,
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Subscriptions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CineFlow.tokens.ink
            )
            Text(
                text = "Private, local updates from creators you follow",
                fontSize = 12.sp,
                color = CineFlow.tokens.muted
            )
        }

        if (subscribedVideos.isEmpty()) {
            CineFlowEmptyView(
                title = "No Subscriptions Yet",
                message = "Subscribe to creators on any video watch page. Your subscription list stays 100% private, local, and encrypted without needing a Google Account.",
                icon = Icons.Rounded.Subscriptions,
                actionLabel = "Explore Recommended Videos",
                onActionClick = onExploreClick
            )
        } else {
            // Horizontal Creator Story Strip
            val creators = subscribedVideos.map { it.uploaderName to it.uploaderAvatarUrl }.distinctBy { it.first }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(creators) { (name, avatarUrl) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.springPress(onClick = {})
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft)
                                .specularBorder(CircleShape, width = 1.5.dp, color = CineFlow.tokens.accent)
                        ) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                SubcomposeAsyncImage(
                                    model = avatarUrl,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.firstOrNull()?.uppercase() ?: "▶",
                                        color = CineFlow.tokens.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = CineFlow.tokens.ink,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subscribed Feed List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(subscribedVideos, key = { it.id }) { video ->
                    VideoCard(
                        item = video,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        }
    }
}
