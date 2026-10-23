package com.cineflow.tube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.ui.components.CineFlowErrorView
import com.cineflow.tube.ui.components.CineFlowSkeletonCard
import com.cineflow.tube.ui.components.VideoCard
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.launch

/**
 * CineFlow Home Feed:
 * - Responsive grid: 1 column on phone, 2 on medium tablets, 3 on expanded tablets
 * - Category filter chips with active ink pill
 * - Preserves scroll state and category filter across destination navigation
 * - Content-shaped skeleton loaders during initial and category fetches
 * - Contextual error and retry handling
 */
@Composable
fun TrendingFeedScreen(
    repository: StreamingRepository,
    listState: LazyListState,
    onVideoClick: (StreamItem) -> Unit,
    onSearchClick: () -> Unit,
    onCycleTheme: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videos by remember { mutableStateOf<List<StreamItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("All", "Music", "Gaming", "Travel", "Technology", "Design", "Science", "Lifestyle", "Live")

    fun loadFeed() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val results = repository.getTrending()
                videos = results
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to connect to streaming backend"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (videos.isEmpty()) {
            loadFeed()
        } else {
            isLoading = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val isTabletWide = maxWidth >= 760.dp
        val gridColumns = when {
            maxWidth >= 1150.dp -> 3
            maxWidth >= 760.dp -> 2
            else -> 1
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTabletWide) {
                    // Brand Mark (▶ cineflow tube) - Phone only (Rail handles tablet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF9FD4FF), Color(0xFF4B8FF5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "cineflow",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.8).sp,
                                color = CineFlow.tokens.ink
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "tube",
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.accent
                            )
                        }
                    }
                } else {
                    // Tablet: NavRail has the brand crest, so show section title here
                    Text(
                        text = "Discover",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.ink
                    )
                }

                // Top Controls: Search, Theme, Profile (>= 48dp touch bounds)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Search Button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                            .clip(CircleShape)
                            .springPress(onClick = onSearchClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = CineFlow.tokens.ink,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Theme Switcher Button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                            .clip(CircleShape)
                            .springPress(onClick = onCycleTheme),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CineFlow.tokens.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LightMode,
                                contentDescription = "Switch Theme",
                                tint = CineFlow.tokens.ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Profile Avatar Circle
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = CineFlow.tokens.minTouchTarget, minHeight = CineFlow.tokens.minTouchTarget)
                            .clip(CircleShape)
                            .springPress(onClick = onProfileClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFB9C9EF), Color(0xFF5B8AD6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "J",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (isSelected) CineFlow.tokens.ink else CineFlow.tokens.surface
                            )
                            .specularBorder(
                                shape = RoundedCornerShape(13.dp),
                                color = if (isSelected) Color.Transparent else CineFlow.tokens.specularRimColor
                            )
                            .springPress { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) CineFlow.tokens.bg else CineFlow.tokens.ink
                        )
                    }
                }
            }

            // Section Heading: Recommended for you ✦
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recommended for you",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = CineFlow.tokens.ink
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✦",
                        fontSize = 18.sp,
                        color = CineFlow.tokens.accent
                    )
                }
                Text(
                    text = "Explore what inspires you",
                    fontSize = 12.sp,
                    color = CineFlow.tokens.muted
                )
            }

            // Video Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        // Content-shaped Skeleton Cards
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) {
                                CineFlowSkeletonCard()
                            }
                        }
                    }

                    errorMessage != null -> {
                        CineFlowErrorView(
                            title = "Connection Notice",
                            message = errorMessage ?: "Could not connect to streaming instance.",
                            actionLabel = "Retry",
                            onActionClick = { loadFeed() }
                        )
                    }

                    else -> {
                        if (gridColumns == 1) {
                            // Phone Single-Column List
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 140.dp)
                            ) {
                                items(videos, key = { it.id }) { video ->
                                    VideoCard(
                                        item = video,
                                        onClick = { onVideoClick(video) }
                                    )
                                }
                            }
                        } else {
                            // Tablet Multi-Column Grid (2 or 3 columns)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(videos, key = { it.id }) { video ->
                                    VideoCard(
                                        item = video,
                                        onClick = { onVideoClick(video) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
