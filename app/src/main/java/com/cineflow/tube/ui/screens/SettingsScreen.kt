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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stream
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.repository.BackendType
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.ui.components.FrostedSurface
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.CineRed

@Composable
fun SettingsScreen(
    repository: StreamingRepository,
    modifier: Modifier = Modifier
) {
    var activeBackend by remember { mutableStateOf(repository.activeBackend) }
    var autoSkipSponsors by remember { mutableStateOf(PlaybackController.autoSkipSponsors) }

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
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (CineFlow.tokens.isDark) Color.White else Color.Black
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Streaming Backend Section
            item {
                SettingsSectionHeader(title = "Streaming Backend")
                Spacer(modifier = Modifier.height(8.dp))
                FrostedSurface(
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = if (CineFlow.tokens.isDark) Color(0xFF13161F) else Color(0xFFF1F5F9)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        BackendOptionRow(
                            title = "Piped API (Recommended)",
                            subtitle = "Ultra-fast REST endpoints with SponsorBlock & RYD",
                            isSelected = activeBackend == BackendType.PIPED,
                            onClick = {
                                activeBackend = BackendType.PIPED
                                repository.activeBackend = BackendType.PIPED
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BackendOptionRow(
                            title = "Invidious API (Alternative)",
                            subtitle = "Decentralized instance network with instant failover",
                            isSelected = activeBackend == BackendType.INVIDIOUS,
                            onClick = {
                                activeBackend = BackendType.INVIDIOUS
                                repository.activeBackend = BackendType.INVIDIOUS
                            }
                        )
                    }
                }
            }

            // 2. SponsorBlock Section
            item {
                SettingsSectionHeader(title = "SponsorBlock & Ad-Free Shield")
                Spacer(modifier = Modifier.height(8.dp))
                FrostedSurface(
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = if (CineFlow.tokens.isDark) Color(0xFF13161F) else Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Skip Video Sponsors",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (CineFlow.tokens.isDark) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically seek past paid sponsorships, intros, and self-promos with zero interruption",
                                fontSize = 12.sp,
                                color = if (CineFlow.tokens.isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
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
                                checkedTrackColor = CineRed
                            )
                        )
                    }
                }
            }

            // 3. Ergonomics & Motion Section
            item {
                SettingsSectionHeader(title = "CineFlow Ergonomics")
                Spacer(modifier = Modifier.height(8.dp))
                FrostedSurface(
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = if (CineFlow.tokens.isDark) Color(0xFF13161F) else Color(0xFFF1F5F9)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FeatureHighlightRow(
                            icon = Icons.Rounded.Tune,
                            title = "Hold-to-2x Fast Forward",
                            description = "Press and hold video anywhere to double speed with tactile haptic confirmation"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FeatureHighlightRow(
                            icon = Icons.Rounded.Stream,
                            title = "CineFlow Fluid Miniplayer",
                            description = "Continuous 120Hz gesture docking with velocity physics and zero playback interruption"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FeatureHighlightRow(
                            icon = Icons.Rounded.Security,
                            title = "Zero Telemetry Guaranteed",
                            description = "No Google Play Services, no fingerprinting, 100% private offline storage"
                        )
                    }
                }
            }

            // 4. Version Badge
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CineFlow Tube v1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (CineFlow.tokens.isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
                    )
                    Text(
                        text = "Engineered with Jetpack Compose & AndroidX Media3",
                        fontSize = 11.sp,
                        color = if (CineFlow.tokens.isDark) Color(0xFF6B7280) else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CineRed,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun BackendOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) CineRed else if (CineFlow.tokens.isDark) Color.White else Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (CineFlow.tokens.isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = CineRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CineRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (CineFlow.tokens.isDark) Color.White else Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (CineFlow.tokens.isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
            )
        }
    }
}
