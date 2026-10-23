package com.cineflow.tube.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.model.StreamItem
import com.cineflow.tube.data.repository.BackendType
import com.cineflow.tube.data.repository.CineFlowDownloadManager
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.data.repository.SubscribedChannel
import com.cineflow.tube.data.repository.YouTubeImportManager
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.player.TimelineStyle
import com.cineflow.tube.ui.components.FrostedSurface
import com.cineflow.tube.ui.theme.AppThemeMode
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.launch

@Composable
fun YourSpaceScreen(
    repository: StreamingRepository,
    listState: LazyListState,
    savedVideos: List<StreamItem> = emptyList(),
    downloadManager: CineFlowDownloadManager? = null,
    importManager: YouTubeImportManager? = null,
    currentThemeMode: AppThemeMode,
    enableDynamicColor: Boolean,
    reducedMotion: Boolean,
    onThemeSelected: (AppThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onVideoClick: (StreamItem) -> Unit,
    onReplaySplash: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playerState by PlaybackController.state.collectAsState()
    var autoSkipSponsors by remember { mutableStateOf(PlaybackController.autoSkipSponsors) }
    var activeBackend by remember { mutableStateOf(repository.activeBackend) }

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
                text = "Settings & Data",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CineFlow.tokens.ink
            )
            Text(
                text = "YouTube & NewPipe import, private local control & preferences",
                fontSize = 12.sp,
                color = CineFlow.tokens.muted
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // YouTube & NewPipe Import/Export Management Card
            item {
                YouTubeImportCard(
                    importManager = importManager
                )
            }

            // Watch Later Section (Watch later ♡)
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = CineFlow.tokens.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Saved for Later",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineFlow.tokens.ink
                                )
                            }
                            Text(
                                text = "${savedVideos.size} items",
                                fontSize = 12.sp,
                                color = CineFlow.tokens.muted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (savedVideos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CineFlow.tokens.soft)
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap ＋ Save on any video to keep it for offline viewing",
                                    fontSize = 12.sp,
                                    color = CineFlow.tokens.muted
                                )
                            }
                        } else {
                            savedVideos.forEach { video ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onVideoClick(video) }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = CineFlow.tokens.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CineFlow.tokens.ink
                                        )
                                        Text(
                                            text = video.uploaderName,
                                            fontSize = 11.sp,
                                            color = CineFlow.tokens.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Downloads Section (Phase 4)
            if (downloadManager != null) {
                item {
                    FrostedSurface(
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = CineFlow.tokens.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            DownloadsSection(
                                downloadManager = downloadManager,
                                onPlayOffline = { dlItem ->
                                    if (dlItem.localFilePath != null) {
                                        PlaybackController.playOffline(
                                            localFilePath = dlItem.localFilePath,
                                            title = dlItem.title,
                                            uploaderName = dlItem.uploaderName,
                                            thumbnailUrl = dlItem.thumbnailUrl
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Instance Selector & Live Ping Tester
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        InstanceSelectorSection(
                            repository = repository
                        )
                    }
                }
            }

            // Atmospheric Theme Appearance Card
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Atmospheric Theme",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineFlow.tokens.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose your preferred ambient lighting",
                            fontSize = 12.sp,
                            color = CineFlow.tokens.muted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Theme Mode Pills (System, Light, Dark, AMOLED)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = mode == currentThemeMode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            if (isSelected) CineFlow.tokens.ink else CineFlow.tokens.soft
                                        )
                                        .springPress { onThemeSelected(mode) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CineFlow.tokens.bg else CineFlow.tokens.ink
                                    )
                                }
                            }
                        }

                        // Dynamic Color Switch (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Material You Dynamic Color",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CineFlow.tokens.ink
                                    )
                                    Text(
                                        text = "Sample wallpaper palette accents",
                                        fontSize = 11.sp,
                                        color = CineFlow.tokens.muted
                                    )
                                }
                                Switch(
                                    checked = enableDynamicColor,
                                    onCheckedChange = onToggleDynamicColor,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CineFlow.tokens.accent
                                    )
                                )
                            }
                        }

                        // Reduced Motion Switch
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reduced Motion",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CineFlow.tokens.ink
                                )
                                Text(
                                    text = "Disable scale springs and wave flow animations",
                                    fontSize = 11.sp,
                                    color = CineFlow.tokens.muted
                                )
                            }
                            Switch(
                                checked = reducedMotion,
                                onCheckedChange = onToggleReducedMotion,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CineFlow.tokens.accent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Replay Glacier Splash Screen Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CineFlow.tokens.soft)
                                .springPress(onClick = onReplaySplash)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = CineFlow.tokens.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Replay Glacier Splash Screen",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CineFlow.tokens.ink
                                    )
                                    Text(
                                        text = "Watch the atmospheric fluid launch experience",
                                        fontSize = 11.sp,
                                        color = CineFlow.tokens.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Immersive Player Experience Card
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Immersive Player Motion",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineFlow.tokens.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customize timeline wave physics and glacier glow",
                            fontSize = 12.sp,
                            color = CineFlow.tokens.muted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Timeline Style Selector
                        Text(
                            text = "Timeline style",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CineFlow.tokens.ink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TimelineStyle.values().forEach { style ->
                                val isSelected = playerState.timelineStyle == style
                                val shortName = when (style) {
                                    TimelineStyle.WAVY -> "Wavy line"
                                    TimelineStyle.CIRCULAR_FLOWER -> "Wavy + flower"
                                    TimelineStyle.MINIMAL_STRAIGHT -> "Straight"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) CineFlow.tokens.ink else CineFlow.tokens.soft
                                        )
                                        .springPress { PlaybackController.setTimelineStyle(style) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CineFlow.tokens.bg else CineFlow.tokens.ink
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ambient Glacier Glow Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ambient glacier glow",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CineFlow.tokens.ink
                                )
                                Text(
                                    text = "Radiant azure backlight during playback",
                                    fontSize = 11.sp,
                                    color = CineFlow.tokens.muted
                                )
                            }
                            Switch(
                                checked = playerState.ambientGlow,
                                onCheckedChange = { PlaybackController.setAmbientGlow(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CineFlow.tokens.accent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auto-hide controls switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-hide controls (3s)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CineFlow.tokens.ink
                                )
                                Text(
                                    text = "Fade chrome during simulated or live playback",
                                    fontSize = 11.sp,
                                    color = CineFlow.tokens.muted
                                )
                            }
                            Switch(
                                checked = playerState.autoHideControls,
                                onCheckedChange = { PlaybackController.setAutoHideControls(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CineFlow.tokens.accent
                                )
                            )
                        }
                    }
                }
            }

            // Streaming Backend Switcher (Piped / Invidious)
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Streaming Engine",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineFlow.tokens.ink
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BackendType.entries.forEach { backend ->
                                val isSelected = backend == activeBackend
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) CineFlow.tokens.ink else CineFlow.tokens.soft
                                        )
                                        .springPress {
                                            activeBackend = backend
                                            repository.activeBackend = backend
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = backend.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CineFlow.tokens.bg else CineFlow.tokens.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SponsorBlock & Ad-Free Shield
            item {
                FrostedSurface(
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = CineFlow.tokens.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✦ SponsorBlock",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineFlow.tokens.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Auto-skip paid video sponsors, intros, and self-promos",
                                fontSize = 12.sp,
                                color = CineFlow.tokens.muted
                            )
                        }

                        Switch(
                            checked = autoSkipSponsors,
                            onCheckedChange = {
                                autoSkipSponsors = it
                                PlaybackController.autoSkipSponsors = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineFlow.tokens.accent
                            )
                        )
                    }
                }
            }

            // Privacy Assurance Note
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = CineFlow.tokens.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% private. Zero tracking or Google Account connection.",
                        fontSize = 12.sp,
                        color = CineFlow.tokens.muted
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeImportCard(
    importManager: YouTubeImportManager?,
    modifier: Modifier = Modifier
) {
    if (importManager == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val subscriptions by importManager.subscriptions.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = importManager.importFromUri(uri)
                statusMessage = result.message
                isSuccess = result.success
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    FrostedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = CineFlow.tokens.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Subscriptions,
                        contentDescription = null,
                        tint = CineFlow.tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "YouTube & NewPipe Data",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.ink
                    )
                }

                Text(
                    text = "${subscriptions.size} channels",
                    fontSize = 12.sp,
                    color = CineFlow.tokens.muted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import subscriptions from Google Takeout (subscriptions.csv) or NewPipe backup (JSON). 100% private, local on-device storage.",
                fontSize = 12.sp,
                color = CineFlow.tokens.muted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Import Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineFlow.tokens.accent)
                        .springPress {
                            importLauncher.launch("*/*")
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import Data",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Export Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineFlow.tokens.soft)
                        .springPress {
                            val json = importManager.exportToJson()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "CineFlow Tube Subscriptions Backup")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Subscriptions Backup"))
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = CineFlow.tokens.ink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export Backup",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CineFlow.tokens.ink
                        )
                    }
                }
            }

            // Status feedback banner
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSuccess) CineFlow.tokens.accent.copy(alpha = 0.15f)
                            else Color(0xFFFF5252).copy(alpha = 0.15f)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        fontSize = 12.sp,
                        color = if (isSuccess) CineFlow.tokens.accent else Color(0xFFFF5252),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Channel management preview list
            if (subscriptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Subscribed Channels",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineFlow.tokens.ink
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewChannels = subscriptions.take(8)
                    previewChannels.forEach { channel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CineFlow.tokens.soft)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.channelName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CineFlow.tokens.ink,
                                    maxLines = 1
                                )
                                Text(
                                    text = channel.channelId,
                                    fontSize = 10.sp,
                                    color = CineFlow.tokens.muted,
                                    maxLines = 1
                                )
                            }

                            // Unsubscribe button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        importManager.removeSubscription(channel.channelId)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove subscription",
                                    tint = CineFlow.tokens.muted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (subscriptions.size > 8) {
                        Text(
                            text = "+ ${subscriptions.size - 8} more channels stored locally",
                            fontSize = 11.sp,
                            color = CineFlow.tokens.muted,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
