package com.cineflow.tube.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.player.PlaybackController
import com.cineflow.tube.player.PlaybackState
import com.cineflow.tube.player.TimelineStyle
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.springPress
import kotlin.math.roundToInt

enum class PickerCategory(val title: String) {
    QUALITY("Quality"),
    SPEED("Playback speed"),
    AUDIO("Audio"),
    SUBTITLES("Subtitles"),
    TIMELINE("Timeline style")
}

/**
 * Options Sheet ("Your viewing space") directly matching cineflow_immersive_player.html:
 * - Frosted glass container (#0B2031F5 with specular border #C0E7F82C)
 * - 2-Level Hierarchical Animated Navigation (Main Settings -> Sub-Picker)
 * - Playback & Experience controls with live toast feedback
 */
@Composable
fun CineFlowOptionsSheet(
    isOpen: Boolean,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit,
    initialPicker: PickerCategory? = null,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var activePicker by remember(initialPicker) { mutableStateOf(initialPicker) }

    BackHandler(enabled = isOpen) {
        if (activePicker != null) {
            activePicker = null
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        // Scrim backdrop (#00000073)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73000000))
        )

        // Sheet Content Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xF50B2031))
                .specularBorder(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) {
                    // Prevent dismiss when tapping inside the sheet
                    detectTapGestures { }
                }
                .padding(20.dp)
        ) {
            AnimatedContent(
                targetState = activePicker,
                transitionSpec = {
                    if (targetState != null) {
                        // Enter sub-picker: slide in from right
                        (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it / 4 } + fadeOut()
                        )
                    } else {
                        // Return to main sheet: slide in from left
                        (slideInHorizontally { -it / 4 } + fadeIn()).togetherWith(
                            slideOutHorizontally { it / 2 } + fadeOut()
                        )
                    }
                },
                label = "sheetNavigation"
            ) { picker ->
                if (picker != null) {
                    // Level 2: Sub-Picker Page
                    SubPickerView(
                        category = picker,
                        state = playbackState,
                        onBack = { activePicker = null },
                        onSelectOption = {
                            activePicker = null
                        },
                        onToast = onToast
                    )
                } else {
                    // Level 1: Main Settings Page
                    MainSettingsView(
                        state = playbackState,
                        onOpenPicker = { activePicker = it },
                        onClose = onDismiss,
                        onToast = onToast
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSettingsView(
    state: PlaybackState,
    onOpenPicker: (PickerCategory) -> Unit,
    onClose: () -> Unit,
    onToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Sheet Head
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your viewing space",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6F5FF)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x38102D3C))
                    .springPress(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFF0FBFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION: PLAYBACK
        SectionLabel(text = "PLAYBACK")

        // Quality
        OptionTriggerRow(
            label = "Quality",
            valueText = state.selectedQuality,
            onClick = { onOpenPicker(PickerCategory.QUALITY) }
        )

        // Speed
        OptionTriggerRow(
            label = "Speed",
            valueText = "${state.playbackSpeed.let { if (it % 1 == 0f) it.toInt() else it }}×",
            onClick = { onOpenPicker(PickerCategory.SPEED) }
        )

        // Audio
        OptionTriggerRow(
            label = "Audio",
            valueText = state.selectedAudioTrack,
            onClick = { onOpenPicker(PickerCategory.AUDIO) }
        )

        // Subtitles
        OptionTriggerRow(
            label = "Subtitles",
            valueText = if (state.subtitlesEnabled) "English · sample" else "Off",
            onClick = { onOpenPicker(PickerCategory.SUBTITLES) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION: EXPERIENCE
        SectionLabel(text = "EXPERIENCE")

        // Timeline Style
        OptionTriggerRow(
            label = "Timeline style",
            valueText = state.timelineStyle.displayName,
            onClick = { onOpenPicker(PickerCategory.TIMELINE) }
        )

        // Auto-hide controls
        SwitchRow(
            label = "Auto-hide controls",
            checked = state.autoHideControls,
            onCheckedChange = {
                PlaybackController.setAutoHideControls(it)
                onToast(if (it) "Auto-hide enabled (3s)" else "Auto-hide disabled")
            }
        )

        // Repeat video (Loop)
        SwitchRow(
            label = "Repeat video",
            checked = state.repeatVideo,
            onCheckedChange = {
                PlaybackController.setRepeatVideo(it)
                onToast(if (it) "Loop enabled" else "Loop disabled")
            }
        )

        // Ambient glacier glow
        SwitchRow(
            label = "Ambient glacier glow",
            checked = state.ambientGlow,
            onCheckedChange = {
                PlaybackController.setAmbientGlow(it)
                onToast(if (it) "Ambient glow enabled" else "Ambient glow disabled")
            }
        )

        // Volume
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Volume",
                    fontSize = 13.sp,
                    color = Color(0xFFDCEEF9)
                )
                Text(
                    text = "${(state.volume * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA4EAFF)
                )
            }
            Slider(
                value = state.volume,
                onValueChange = { PlaybackController.setVolume(it) },
                onValueChangeFinished = {
                    onToast("Volume: ${(state.volume * 100).roundToInt()}%")
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFDCF8FF),
                    activeTrackColor = Color(0xFF94D9F0),
                    inactiveTrackColor = Color(0x33A4EAFF)
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Reset Preferences Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF183A4E))
                .specularBorder(RoundedCornerShape(14.dp))
                .springPress {
                    PlaybackController.resetPreferences()
                    onToast("Player preferences reset")
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Reset player preferences",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE3F6FF)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SubPickerView(
    category: PickerCategory,
    state: PlaybackState,
    onBack: () -> Unit,
    onSelectOption: () -> Unit,
    onToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Picker Header with Back Arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x38102D3C))
                    .springPress(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackChevronIcon(tint = Color(0xFFF0FBFF), size = 20.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = category.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6F5FF)
            )
        }

        SectionLabel(text = "CHOOSE YOUR PREFERENCE")

        when (category) {
            PickerCategory.QUALITY -> {
                val qualities = listOf("Auto", "2160p (4K)", "1440p (2K)", "1080p (FHD)", "720p (HD)", "480p", "360p")
                qualities.forEach { q ->
                    val cleanQuality = q.substringBefore(" ")
                    val isSelected = state.selectedQuality == cleanQuality || (cleanQuality == "Auto" && state.selectedQuality == "Auto")
                    ChoiceItem(
                        title = q,
                        isSelected = isSelected,
                        onClick = {
                            PlaybackController.setQuality(cleanQuality)
                            onToast("Preview quality: $cleanQuality")
                            onSelectOption()
                        }
                    )
                }
            }

            PickerCategory.SPEED -> {
                val speeds = listOf(
                    0.25f to "0.25×",
                    0.5f to "0.5×",
                    0.75f to "0.75×",
                    1.0f to "1×",
                    1.25f to "1.25×",
                    1.5f to "1.5×",
                    1.75f to "1.75×",
                    2.0f to "2×"
                )
                speeds.forEach { (sp, label) ->
                    val isSelected = state.playbackSpeed == sp
                    ChoiceItem(
                        title = label,
                        isSelected = isSelected,
                        onClick = {
                            PlaybackController.setSpeed(sp)
                            onToast("Speed: $label")
                            onSelectOption()
                        }
                    )
                }
            }

            PickerCategory.AUDIO -> {
                val audioTracks = listOf("Original · Stereo", "English · Stereo")
                audioTracks.forEach { track ->
                    val isSelected = state.selectedAudioTrack == track
                    ChoiceItem(
                        title = track,
                        isSelected = isSelected,
                        onClick = {
                            PlaybackController.setAudioTrack(track)
                            onToast("Preview audio: $track")
                            onSelectOption()
                        }
                    )
                }
            }

            PickerCategory.SUBTITLES -> {
                val subs = listOf("off" to "Off", "en" to "English · sample")
                subs.forEach { (code, label) ->
                    val isSelected = if (code == "off") !state.subtitlesEnabled else state.subtitlesEnabled
                    ChoiceItem(
                        title = label,
                        isSelected = isSelected,
                        onClick = {
                            PlaybackController.setSelectedSubtitle(code)
                            onToast("Subtitles: $label")
                            onSelectOption()
                        }
                    )
                }
            }

            PickerCategory.TIMELINE -> {
                TimelineStyle.values().forEach { style ->
                    val isSelected = state.timelineStyle == style
                    ChoiceItem(
                        title = style.displayName,
                        isSelected = isSelected,
                        onClick = {
                            PlaybackController.setTimelineStyle(style)
                            onToast("Timeline: ${style.displayName}")
                            onSelectOption()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Color(0xFF91B4C9),
        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
    )
}

@Composable
private fun OptionTriggerRow(
    label: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFFDCEEF9)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF17384A))
                .specularBorder(RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE3F4FC)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFA4EAFF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFFDCEEF9)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFE6F5FF),
                checkedTrackColor = Color(0xFF377CDD),
                uncheckedThumbColor = Color(0xFF91B4C9),
                uncheckedTrackColor = Color(0xFF17384A)
            )
        )
    }
}

@Composable
private fun ChoiceItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (isSelected) Color(0xFF214A60) else Color(0x00000000))
            .specularBorder(
                RoundedCornerShape(15.dp),
                width = if (isSelected) 1.dp else 0.dp
            )
            .springPress(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFE6F5FF) else Color(0xFFDCEEF9)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color(0xFFA7E6F9),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
