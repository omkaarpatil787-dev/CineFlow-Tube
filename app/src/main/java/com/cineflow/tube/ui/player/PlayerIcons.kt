package com.cineflow.tube.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.ui.theme.springPress
import kotlinx.coroutines.launch

/**
 * Rotatable Seek Button from cineflow_immersive_player.html
 * 54x54dp circular button with circular arrow + centered "10"
 * Tap triggers 380ms rotation animation: rotates 32deg in direction of seek
 */
@Composable
fun RotatableSeekButton(
    isForward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    tint: Color = Color(0xFFF0FBFF)
) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val targetAngle = if (isForward) 32f else -32f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .springPress {
                scope.launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = targetAngle,
                        animationSpec = tween(130, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
                    )
                    rotation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
                    )
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Circular seek arrow icon
        Canvas(
            modifier = Modifier
                .size(size * 0.62f)
                .rotate(rotation.value)
        ) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = 1.8.dp.toPx()

            if (isForward) {
                // Forward (Clockwise arrow)
                val path = Path().apply {
                    // Arc: 270 deg around circle
                    val arcRect = Rect(0.12f * w, 0.12f * h, 0.88f * w, 0.88f * h)
                    arcTo(arcRect, 300f, 290f, false)
                    // Arrowhead at top right
                    moveTo(0.82f * w, 0.08f * h)
                    lineTo(0.98f * w, 0.24f * h)
                    lineTo(0.78f * w, 0.32f * h)
                }
                drawPath(path, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                // Rewind (Counter-clockwise arrow)
                val path = Path().apply {
                    val arcRect = Rect(0.12f * w, 0.12f * h, 0.88f * w, 0.88f * h)
                    arcTo(arcRect, 240f, -290f, false)
                    // Arrowhead at top left
                    moveTo(0.18f * w, 0.08f * h)
                    lineTo(0.02f * w, 0.24f * h)
                    lineTo(0.22f * w, 0.32f * h)
                }
                drawPath(path, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Center "10" indicator
        Text(
            text = "10",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = tint,
            modifier = Modifier.offset(y = 1.dp)
        )
    }
}

/**
 * Frosted Icon Circle Button (48x48dp) used in player header and toolbar
 */
@Composable
fun PlayerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isActive) Color(0x52214A60) else Color(0x38102D3C))
            .springPress(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Back Chevron Left Icon
 */
@Composable
fun BackChevronIcon(tint: Color = Color(0xFFF0FBFF), size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(0.58f * w, 0.25f * h)
            lineTo(0.33f * w, 0.50f * h)
            lineTo(0.58f * w, 0.75f * h)
        }
        drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Subtitles / Captions CC Icon
 */
@Composable
fun CaptionsIcon(tint: Color = Color(0xFFF0FBFF), size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()

        // Outer rounded rect
        val rect = Rect(0.12f * w, 0.22f * h, 0.88f * w, 0.78f * h)
        drawRoundRect(
            color = tint,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(width = strokeW)
        )

        // CC paths
        val p1 = Path().apply {
            moveTo(0.42f * w, 0.38f * h)
            lineTo(0.30f * w, 0.38f * h)
            lineTo(0.30f * w, 0.62f * h)
            lineTo(0.42f * w, 0.62f * h)
        }
        val p2 = Path().apply {
            moveTo(0.70f * w, 0.38f * h)
            lineTo(0.58f * w, 0.38f * h)
            lineTo(0.58f * w, 0.62f * h)
            lineTo(0.70f * w, 0.62f * h)
        }
        drawPath(p1, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p2, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Three Dots "More" Icon
 */
@Composable
fun MoreDotsIcon(tint: Color = Color(0xFFF0FBFF), size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val cy = this.size.height / 2f
        val r = 1.8.dp.toPx()
        drawCircle(color = tint, radius = r, center = Offset(0.24f * w, cy))
        drawCircle(color = tint, radius = r, center = Offset(0.50f * w, cy))
        drawCircle(color = tint, radius = r, center = Offset(0.76f * w, cy))
    }
}

/**
 * Fullscreen Corners Icon
 */
@Composable
fun FullscreenCornersIcon(tint: Color = Color(0xFFF0FBFF), size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()
        val path = Path().apply {
            // Top Left
            moveTo(0.33f * w, 0.15f * h)
            lineTo(0.15f * w, 0.15f * h)
            lineTo(0.15f * w, 0.33f * h)
            // Top Right
            moveTo(0.67f * w, 0.15f * h)
            lineTo(0.85f * w, 0.15f * h)
            lineTo(0.85f * w, 0.33f * h)
            // Bottom Left
            moveTo(0.15f * w, 0.67f * h)
            lineTo(0.15f * w, 0.85f * h)
            lineTo(0.33f * w, 0.85f * h)
            // Bottom Right
            moveTo(0.67f * w, 0.85f * h)
            lineTo(0.85f * w, 0.85f * h)
            lineTo(0.85f * w, 0.67f * h)
        }
        drawPath(path, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Hide Controls Chevron Down Icon
 */
@Composable
fun HideChevronDownIcon(tint: Color = Color(0xFFF0FBFF), size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(0.25f * w, 0.38f * h)
            lineTo(0.50f * w, 0.63f * h)
            lineTo(0.75f * w, 0.38f * h)
        }
        drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
