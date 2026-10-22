package com.cineflow.tube.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cineflow.tube.ui.components.specularBorder
import com.cineflow.tube.ui.theme.CineFlowMotion
import com.cineflow.tube.ui.theme.springPress

/**
 * Pattern 01: Icon Morph (Play becomes pause)
 * Continuous 300ms vector interpolation between play triangle and pause bars
 * with stable touch target and CineFlow spring press response.
 */
@Composable
fun MorphingPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0x9910233E)
) {
    // Progress 0f = Play (triangle), 1f = Pause (double bars)
    val morphProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = CineFlowMotion.IconMorphTween,
        label = "playPauseMorph"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(30.dp))
            .background(backgroundColor)
            .specularBorder(RoundedCornerShape(30.dp))
            .springPress(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.44f)) {
            val w = this.size.width
            val h = this.size.height

            // Left wing: morphs from left half of play triangle to left pause bar
            // Play Left: (10% x, 0% y) -> (55% x, 25% y) -> (55% x, 75% y) -> (10% x, 100% y)
            // Pause Left: (8% x, 0% y) -> (38% x, 0% y) -> (38% x, 100% y) -> (8% x, 100% y)
            val p = morphProgress

            val l1x = lerp(0.10f * w, 0.08f * w, p)
            val l1y = lerp(0.00f * h, 0.00f * h, p)

            val l2x = lerp(0.55f * w, 0.38f * w, p)
            val l2y = lerp(0.25f * h, 0.00f * h, p)

            val l3x = lerp(0.55f * w, 0.38f * w, p)
            val l3y = lerp(0.75f * h, 1.00f * h, p)

            val l4x = lerp(0.10f * w, 0.08f * w, p)
            val l4y = lerp(1.00f * h, 1.00f * h, p)

            val leftPath = Path().apply {
                moveTo(l1x, l1y)
                lineTo(l2x, l2y)
                lineTo(l3x, l3y)
                lineTo(l4x, l4y)
                close()
            }
            drawPath(path = leftPath, color = tint)

            // Right wing: morphs from right apex of play triangle to right pause bar
            // Play Right: (55% x, 25% y) -> (100% x, 50% y) -> (100% x, 50% y) -> (55% x, 75% y)
            // Pause Right: (62% x, 0% y) -> (92% x, 0% y) -> (92% x, 100% y) -> (62% x, 100% y)
            val r1x = lerp(0.55f * w, 0.62f * w, p)
            val r1y = lerp(0.25f * h, 0.00f * h, p)

            val r2x = lerp(1.00f * w, 0.92f * w, p)
            val r2y = lerp(0.50f * h, 0.00f * h, p)

            val r3x = lerp(1.00f * w, 0.92f * w, p)
            val r3y = lerp(0.50f * h, 1.00f * h, p)

            val r4x = lerp(0.55f * w, 0.62f * w, p)
            val r4y = lerp(0.75f * h, 1.00f * h, p)

            val rightPath = Path().apply {
                moveTo(r1x, r1y)
                lineTo(r2x, r2y)
                lineTo(r3x, r3y)
                lineTo(r4x, r4y)
                close()
            }
            drawPath(path = rightPath, color = tint)
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
