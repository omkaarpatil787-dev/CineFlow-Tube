package com.cineflow.tube.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.ui.components.FrostedSurface
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.CineRed

@Composable
fun LibraryScreen(
    onHistoryClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (CineFlow.tokens.isDark) Color.White else Color.Black
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LibraryOptionCard(
                    icon = Icons.Rounded.History,
                    title = "Watch History",
                    subtitle = "Locally cached resume points & watched clips",
                    onClick = onHistoryClick
                )
            }
            item {
                LibraryOptionCard(
                    icon = Icons.Rounded.Bookmark,
                    title = "Bookmarked Playlists",
                    subtitle = "Saved collections and custom playlists",
                    onClick = onBookmarksClick
                )
            }
            item {
                LibraryOptionCard(
                    icon = Icons.Rounded.DownloadDone,
                    title = "Offline Downloads",
                    subtitle = "Cached videos for offline airplane viewing",
                    onClick = onDownloadsClick
                )
            }
        }
    }
}

@Composable
private fun LibraryOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FrostedSurface(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (CineFlow.tokens.isDark) Color(0xFF13161F) else Color(0xFFF1F5F9),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrostedSurface(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = CineRed.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CineRed,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (CineFlow.tokens.isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (CineFlow.tokens.isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
                )
            }
        }
    }
}
