package com.cineflow.tube.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cineflow.tube.data.model.DownloadItem
import com.cineflow.tube.data.repository.CineFlowDownloadManager
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress

/**
 * CineFlow Downloads Section (Phase 4)
 * ─────────────────────────────────────
 * Renders inside YourSpaceScreen as a section card showing:
 *  - Storage summary (used / total)
 *  - Per-download progress rows with pause, delete and play actions
 *  - Empty state when no downloads exist
 */
@Composable
fun DownloadsSection(
    downloadManager: CineFlowDownloadManager,
    onPlayOffline: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by downloadManager.downloads.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {

        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = CineFlow.tokens.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Downloads",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineFlow.tokens.ink
                )
            }

            if (downloads.isNotEmpty()) {
                val usedMb = downloadManager.totalStorageUsedBytes() / (1024 * 1024)
                Text(
                    text = "${usedMb} MB used",
                    fontSize = 11.sp,
                    color = CineFlow.tokens.muted
                )
            }
        }

        if (downloads.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CineFlow.tokens.soft)
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = CineFlow.tokens.muted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No downloads yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.muted
                    )
                    Text(
                        text = "Long-press any video to download for offline viewing",
                        fontSize = 12.sp,
                        color = CineFlow.tokens.muted.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                downloads.forEach { item ->
                    DownloadItemRow(
                        item = item,
                        onPlay = { onPlayOffline(item) },
                        onPause = { downloadManager.pauseDownload(item.id) },
                        onDelete = { downloadManager.deleteDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemRow(
    item: DownloadItem,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (item.totalBytes > 0) {
        (item.bytesDownloaded.toFloat() / item.totalBytes).coerceIn(0f, 1f)
    } else if (item.isCompleted) 1f else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = 120f),
        label = "downloadProgress_${item.id}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CineFlow.tokens.soft)
            .specularBorder(RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 45.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineFlow.tokens.bg)
            ) {
                if (item.thumbnailUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Completed overlay
                if (item.isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play offline",
                            tint = Color.White,
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
                    fontWeight = FontWeight.SemiBold,
                    color = CineFlow.tokens.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Text(
                    text = "${item.uploaderName}  ·  ${item.quality}  ·  ${item.format}",
                    fontSize = 11.sp,
                    color = CineFlow.tokens.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.isCompleted && item.totalBytes > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CineFlow.tokens.accent,
                        trackColor = CineFlow.tokens.bg,
                        strokeCap = StrokeCap.Round
                    )
                    val dlMb = item.bytesDownloaded / (1024 * 1024)
                    val totalMb = item.totalBytes / (1024 * 1024)
                    Text(
                        text = "$dlMb / $totalMb MB",
                        fontSize = 10.sp,
                        color = CineFlow.tokens.muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (item.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Ready to watch offline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.isCompleted) {
                    // Play button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CineFlow.tokens.accent.copy(alpha = 0.15f))
                            .springPress(onClick = onPlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play offline",
                            tint = CineFlow.tokens.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Pause button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CineFlow.tokens.soft)
                            .springPress(onClick = onPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Pause download",
                            tint = CineFlow.tokens.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF5277))
                        .springPress(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete download",
                        tint = Color(0xFFFF8FA8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
