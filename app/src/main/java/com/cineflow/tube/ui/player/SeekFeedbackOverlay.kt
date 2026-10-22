package com.cineflow.tube.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.ui.theme.CineFlowMotion
import kotlinx.coroutines.launch

/**
 * Pattern 05: Seek Feedback (Ten seconds forward / rewind)
 * Direct implementation of cineflow_motion_lab.html:
 * One short ripple confirms the seek without covering the video or interrupting playback.
 * 360ms ease out ripple (Scale 0.85 -> 2.1, Alpha 0.6 -> 0.0).
 */
@Composable
fun SeekFeedbackOverlay(
    isForward: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringScale = remember { Animatable(0.85f) }
    val ringAlpha = remember { Animatable(0.6f) }
    val badgeScale = remember { Animatable(0.94f) }

    LaunchedEffect(Unit) {
        launch {
            ringScale.animateTo(
                targetValue = 2.1f,
                animationSpec = tween(CineFlowMotion.DurationSeekRipple, easing = FastOutSlowInEasing)
            )
        }
        launch {
            ringAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(CineFlowMotion.DurationSeekRipple, easing = FastOutSlowInEasing)
            )
        }
        launch {
            badgeScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(CineFlowMotion.DurationSeekButton, easing = FastOutSlowInEasing)
            )
        }

        kotlinx.coroutines.delay(CineFlowMotion.DurationSeekRipple.toLong())
        onAnimationEnd()
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding Ripple Ring (Scale 0.85 -> 2.1, Alpha 0.6 -> 0)
        Canvas(
            modifier = Modifier
                .size(82.dp)
                .scale(ringScale.value)
                .alpha(ringAlpha.value)
        ) {
            drawCircle(
                color = Color(0xFFA9E9FF),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Confirmation Badge
        Box(
            modifier = Modifier
                .size(82.dp)
                .scale(badgeScale.value)
                .clip(CircleShape)
                .background(Color(0xD9123447)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = Color(0xFFE3F4FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isForward) "+10s" else "-10s",
                    color = Color(0xFFE3F4FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
