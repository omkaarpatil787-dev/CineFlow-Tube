package com.cineflow.tube.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.data.model.StreamingInstance
import com.cineflow.tube.data.repository.BackendType
import com.cineflow.tube.data.repository.InstanceHealthManager
import com.cineflow.tube.data.repository.StreamingRepository
import com.cineflow.tube.ui.theme.CineFlow
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.launch

/**
 * CineFlow Instance Selector & Live Ping Tester card (Phase 3 — wired UI)
 * ─────────────────────────────────────────────────────────────────────────
 * Displays:
 *  - Auto-route toggle (automatically pick fastest responding server)
 *  - Live ping latency badges for every Piped + Invidious instance
 *  - ⚡ "Re-test All" button that runs concurrent HEAD pings
 *  - Custom instance URL input with instant ping validation
 *  - Backend selector (Piped / Invidious)
 *
 * Visual: matches YourSpaceScreen card style — FrostedSurface + CineFlow tokens
 */
@Composable
fun InstanceSelectorSection(
    repository: StreamingRepository,
    modifier: Modifier = Modifier
) {
    val healthManager = remember { InstanceHealthManager() }
    val scope = rememberCoroutineScope()

    val pipedInstances by healthManager.pipedInstances.collectAsState()
    val invidiousInstances by healthManager.invidiousInstances.collectAsState()
    val isTesting by healthManager.isTesting.collectAsState()

    var autoRoute by remember { mutableStateOf(true) }
    var selectedBackend by remember { mutableStateOf(repository.activeBackend) }
    var showAddCustom by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customType by remember { mutableStateOf(BackendType.PIPED) }
    var addResult by remember { mutableStateOf<Boolean?>(null) }

    // Spin animation for the refresh icon while testing
    val spinAngle by animateFloatAsState(
        targetValue = if (isTesting) 360f else 0f,
        animationSpec = spring(stiffness = 40f),
        label = "spinRefresh"
    )

    // Auto-run a first ping sweep on first composition
    LaunchedEffect(Unit) {
        scope.launch { healthManager.testAllInstances() }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Section header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    tint = CineFlow.tokens.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Instances & Ping",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineFlow.tokens.ink
                )
            }

            // Re-test button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CineFlow.tokens.soft)
                    .springPress(onClick = {
                        if (!isTesting) scope.launch { healthManager.testAllInstances() }
                    }),
                contentAlignment = Alignment.Center
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CineFlow.tokens.accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Re-test instances",
                        tint = CineFlow.tokens.ink,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(spinAngle)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Auto-route toggle ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CineFlow.tokens.soft)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto-route to fastest",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineFlow.tokens.ink
                )
                Text(
                    text = "Automatically uses the lowest-latency instance",
                    fontSize = 11.sp,
                    color = CineFlow.tokens.muted
                )
            }
            Switch(
                checked = autoRoute,
                onCheckedChange = { autoRoute = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CineFlow.tokens.accent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Backend selector (Piped / Invidious) ──────────────────────────
        if (!autoRoute) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(BackendType.PIPED to "Piped", BackendType.INVIDIOUS to "Invidious").forEach { (type, label) ->
                    val isSelected = selectedBackend == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CineFlow.tokens.accent.copy(alpha = 0.18f) else CineFlow.tokens.soft)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) CineFlow.tokens.accent else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .springPress(onClick = {
                                selectedBackend = type
                                repository.activeBackend = type
                            })
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CineFlow.tokens.accent else CineFlow.tokens.ink
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Piped instances list ───────────────────────────────────────────
        Text(
            text = "Piped",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CineFlow.tokens.muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pipedInstances.forEach { instance ->
                InstanceRow(
                    instance = instance,
                    isAutoSelected = autoRoute && instance == pipedInstances.firstOrNull { it.isHealthy },
                    onRemove = if (instance.isCustom) {
                        { healthManager.removeCustomInstance(instance.url, BackendType.PIPED) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Invidious instances list ───────────────────────────────────────
        Text(
            text = "Invidious",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CineFlow.tokens.muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            invidiousInstances.forEach { instance ->
                InstanceRow(
                    instance = instance,
                    isAutoSelected = autoRoute && instance == invidiousInstances.firstOrNull { it.isHealthy },
                    onRemove = if (instance.isCustom) {
                        { healthManager.removeCustomInstance(instance.url, BackendType.INVIDIOUS) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Add custom instance ────────────────────────────────────────────
        if (!showAddCustom) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CineFlow.tokens.soft)
                    .springPress(onClick = { showAddCustom = true })
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add custom instance",
                        tint = CineFlow.tokens.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Custom Instance",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineFlow.tokens.accent
                    )
                }
            }
        } else {
            // Inline add form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CineFlow.tokens.soft)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it; addResult = null },
                    label = { Text("Instance URL", fontSize = 12.sp) },
                    placeholder = { Text("https://pipedapi.example.com", fontSize = 12.sp, color = CineFlow.tokens.muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineFlow.tokens.accent,
                        unfocusedBorderColor = CineFlow.tokens.muted.copy(alpha = 0.4f),
                        focusedLabelColor = CineFlow.tokens.accent,
                        unfocusedLabelColor = CineFlow.tokens.muted,
                        focusedTextColor = CineFlow.tokens.ink,
                        unfocusedTextColor = CineFlow.tokens.ink
                    )
                )
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Name (optional)", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (customUrl.isNotBlank()) {
                            scope.launch {
                                addResult = healthManager.addCustomInstance(customUrl, customName, customType)
                                if (addResult == true) { customUrl = ""; customName = ""; showAddCustom = false }
                            }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CineFlow.tokens.accent,
                        unfocusedBorderColor = CineFlow.tokens.muted.copy(alpha = 0.4f),
                        focusedLabelColor = CineFlow.tokens.accent,
                        unfocusedLabelColor = CineFlow.tokens.muted,
                        focusedTextColor = CineFlow.tokens.ink,
                        unfocusedTextColor = CineFlow.tokens.ink
                    )
                )

                // Backend type for custom
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(BackendType.PIPED to "Piped", BackendType.INVIDIOUS to "Invidious").forEach { (type, label) ->
                        val sel = customType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) CineFlow.tokens.accent.copy(alpha = 0.18f) else CineFlow.tokens.bg)
                                .border(1.dp, if (sel) CineFlow.tokens.accent else CineFlow.tokens.muted.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .springPress(onClick = { customType = type })
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (sel) CineFlow.tokens.accent else CineFlow.tokens.ink, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Add result feedback
                addResult?.let { success ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (success) Icons.Rounded.Check else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (success) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (success) "Instance reachable and added" else "Could not reach this URL — check it and try again",
                            fontSize = 11.sp,
                            color = if (success) Color(0xFF4CAF50) else Color(0xFFFF5252)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineFlow.tokens.bg)
                            .springPress(onClick = { showAddCustom = false; addResult = null })
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp, color = CineFlow.tokens.muted)
                    }
                    // Test & Add
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineFlow.tokens.accent)
                            .springPress(onClick = {
                                if (customUrl.isNotBlank()) {
                                    scope.launch {
                                        addResult = healthManager.addCustomInstance(customUrl, customName, customType)
                                        if (addResult == true) { customUrl = ""; customName = ""; showAddCustom = false }
                                    }
                                }
                            })
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text("Test & Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CineFlow.tokens.bg)
                    }
                }
            }
        }
    }
}

// ── Single instance row ────────────────────────────────────────────────────

@Composable
private fun InstanceRow(
    instance: StreamingInstance,
    isAutoSelected: Boolean,
    onRemove: (() -> Unit)? = null
) {
    val pingColor = when {
        !instance.isHealthy || instance.pingMs < 0 -> Color(0xFFFF5252)
        instance.pingMs < 400 -> Color(0xFF4CAF50)
        instance.pingMs < 800 -> Color(0xFFFFB300)
        else -> Color(0xFFFF7043)
    }

    val bgColor by animateColorAsState(
        targetValue = if (isAutoSelected) CineFlow.tokens.accent.copy(alpha = 0.12f) else CineFlow.tokens.soft,
        animationSpec = spring(),
        label = "instanceBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (instance.pingMs < 0) Color(0x44FF5252) else pingColor.copy(alpha = 0.85f))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = instance.name,
                    fontSize = 12.sp,
                    fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal,
                    color = CineFlow.tokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isAutoSelected) {
                    Text(
                        text = "AUTO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CineFlow.tokens.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                if (instance.isCustom) {
                    Text(
                        text = "CUSTOM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineFlow.tokens.muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CineFlow.tokens.muted.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = instance.url,
                fontSize = 10.sp,
                color = CineFlow.tokens.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Ping badge
        AnimatedContent(
            targetState = instance.pingMs,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "pingBadge_${instance.url}"
        ) { pingMs ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(pingColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when {
                        pingMs < 0 -> "✗ offline"
                        else -> "${pingMs}ms"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = pingColor
                )
            }
        }

        // Remove button (custom instances only)
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FF5252))
                    .springPress(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8FA8))
            }
        }
    }
}
