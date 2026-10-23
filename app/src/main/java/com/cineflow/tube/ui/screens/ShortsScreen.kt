package com.cineflow.tube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * CineFlow Shorts Screen (matching jadzumi_stream_v02.html `.shortgrid`):
 * - 9:16 vertical cards with bottom title overlays and subtle gradients
 * - Adaptive layout: 2 columns on phones, 4 columns on tablets
 * - Smooth spring press and touch targets
 */
@Composable
fun ShortsScreen(
    gridState: LazyGridState,
    shorts: List<StreamItem> = emptyList(),
    onShortClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayShorts = remember(shorts) {
        if (shorts.isNotEmpty()) shorts else getDefaultShorts()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val columns = if (maxWidth >= 760.dp) 4 else 2

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Shorts",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.ink
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = CineFlow.tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Vertical cinema in ultra-fluid motion",
                    fontSize = 12.sp,
                    color = CineFlow.tokens.muted
                )
            }

            // 9:16 Vertical Video Grid
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayShorts, key = { it.id }) { shortItem ->
                    ShortCard(
                        item = shortItem,
                        onClick = { onShortClick(shortItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortCard(
    item: StreamItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(22.dp))
            .background(CineFlow.tokens.soft)
            .specularBorder(RoundedCornerShape(22.dp))
            .springPress(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(CineFlow.tokens.soft))
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E3A5F), Color(0xFF0F1E33))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▣", color = Color.White.copy(alpha = 0.5f), fontSize = 48.sp)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Vignette Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1.0f to Color(0xE6050C14)
                    )
                )
        )

        // Overlay Title and Views
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${item.uploaderName} · ${item.viewsFormatted}",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getDefaultShorts(): List<StreamItem> = listOf(
    StreamItem(
        id = "short_01",
        title = "Glacier calving in slow motion 4K",
        uploaderName = "Northbound Films",
        thumbnailUrl = "https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?w=600",
        viewsFormatted = "2.4M views"
    ),
    StreamItem(
        id = "short_02",
        title = "Studio lighting setup in 30 seconds",
        uploaderName = "Pixel Atelier",
        thumbnailUrl = "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=600",
        viewsFormatted = "850K views"
    ),
    StreamItem(
        id = "short_03",
        title = "Morning coffee pour in Kyoto cafe",
        uploaderName = "Mika Journal",
        thumbnailUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600",
        viewsFormatted = "1.8M views"
    ),
    StreamItem(
        id = "short_04",
        title = "Supernova shockwave simulation",
        uploaderName = "Cosmos Notes",
        thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600",
        viewsFormatted = "620K views"
    ),
    StreamItem(
        id = "short_05",
        title = "Norway fjord aerial dive with drone",
        uploaderName = "Wander Studio",
        thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600",
        viewsFormatted = "3.1M views"
    ),
    StreamItem(
        id = "short_06",
        title = "Designing fluid glass UI buttons",
        uploaderName = "CineFlow Studio",
        thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600",
        viewsFormatted = "440K views"
    )
)
