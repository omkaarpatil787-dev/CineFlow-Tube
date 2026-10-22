package com.cineflow.tube.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.player.TimelineStyle
import com.cineflow.tube.ui.components.specularBorder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Signature CineFlow Wavy Timeline (from cineflow_immersive_player.html)
 *
 * Implements:
 * 1. Animated wave flow formula: y = centerY + sin(x/wavelength * 2π - phase) * amplitude * taper
 * 2. 3 selectable timeline styles:
 *    - WAVY: flowing wave played line with glowing head bead
 *    - CIRCULAR_FLOWER: flowing wave played line with 12-petal rotating rosette scrub handle
 *    - MINIMAL_STRAIGHT: clean minimal straight progress track
 * 3. Scrub preview bubble: floating capsule displaying scrubbed timestamp directly above thumb
 * 4. Tap & Drag gesture interaction with 60/120Hz smooth rendering
 */
@Composable
fun WavyProgressTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = (durationMs * 0.68f).toLong(),
    isPlaying: Boolean,
    timelineStyle: TimelineStyle = TimelineStyle.WAVY,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = durationMs.coerceAtLeast(1L)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    val actualFraction = (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f)
    val displayFraction = if (isScrubbing) scrubFraction else actualFraction

    // Scrub handle thickness animation (3dp normal -> 5dp when scrubbing)
    val thicknessDp by animateFloatAsState(
        targetValue = if (isScrubbing) 5f else 3.5f,
        animationSpec = tween(180),
        label = "thickness"
    )

    // Continuous 1800ms wave flow phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    // Continuous 7000ms flower handle rotation for CIRCULAR_FLOWER style
    val flowerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing)
        ),
        label = "flowerRotation"
    )

    val activePhase = if (isPlaying) wavePhase else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { componentSize = it }
            .pointerInput(totalMs) {
                detectTapGestures(
                    onPress = { offset ->
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            val f = (offset.x / width).coerceIn(0f, 1f)
                            isScrubbing = true
                            scrubFraction = f
                            tryAwaitRelease()
                            onSeek((scrubFraction * totalMs).toLong())
                            isScrubbing = false
                        }
                    }
                )
            }
            .pointerInput(totalMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isScrubbing = true
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            scrubFraction = (offset.x / width).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        onSeek((scrubFraction * totalMs).toLong())
                        isScrubbing = false
                    },
                    onDragCancel = {
                        isScrubbing = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            scrubFraction = (change.position.x / width).coerceIn(0f, 1f)
                        }
                    }
                )
            }
    ) {
        // Scrub Preview Floating Bubble
        if (isScrubbing && componentSize.width > 0) {
            val previewMs = (scrubFraction * totalMs).toLong()
            val bubbleX = (scrubFraction * componentSize.width).coerceIn(36f, (componentSize.width - 36).toFloat())
            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleX.roundToInt(), -42.dp.roundToPx()) }
                    .wrapContentSize(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE6102D40))
                    .specularBorder(RoundedCornerShape(10.dp), strokeWidth = 1.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatTimelineTime(previewMs),
                    color = Color(0xFFDCF8FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Canvas for Rail, Buffer, Played Wave, and Scrub Head
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val strokeWidthPx = thicknessDp.dp.toPx()
            val playedLength = (width * displayFraction).coerceIn(0f, width)

            // 1. Inactive Rail (Straight background)
            val railColor = Color(0x33E4F7FF)
            drawLine(
                color = railColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )

            // 2. Buffer Track (Buffered progress)
            val bufferFraction = (bufferedPositionMs.toFloat() / totalMs).coerceIn(0f, 1f)
            val bufferLength = (width * bufferFraction).coerceIn(0f, width)
            if (bufferLength > 0f) {
                val bufferColor = Color(0x2BD7F4FF)
                drawLine(
                    color = bufferColor,
                    start = Offset(0f, centerY),
                    end = Offset(bufferLength, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Played Track
            val playedColor = Color(0xFFDCF8FF)

            if (timelineStyle == TimelineStyle.MINIMAL_STRAIGHT || playedLength <= 2f) {
                // Minimal straight line
                if (playedLength > 0f) {
                    drawLine(
                        color = playedColor,
                        start = Offset(0f, centerY),
                        end = Offset(playedLength, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                // Signature CineFlow Wavy Sine Wave formula
                val wavePath = Path()
                val step = 3.dp.toPx()
                val amplitude = 4.dp.toPx()
                val wavelength = 38.dp.toPx()

                var x = 0f
                var isFirst = true

                while (x < playedLength) {
                    // Taper down amplitude near beginning and near head
                    val taperStart = x / 12.dp.toPx()
                    val taperEnd = (playedLength - x) / 16.dp.toPx()
                    val taper = minOf(1f, taperStart, taperEnd).coerceAtLeast(0f)

                    val y = centerY + sin((x / wavelength) * (2f * PI.toFloat()) - activePhase) * amplitude * taper

                    if (isFirst) {
                        wavePath.moveTo(x, y)
                        isFirst = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += step
                }
                wavePath.lineTo(playedLength, centerY)

                drawPath(
                    path = wavePath,
                    color = playedColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 4. Scrub Head / Handle
            val headX = playedLength
            val headRadius = if (isScrubbing) 6.5.dp.toPx() else 4.5.dp.toPx()

            // Outer soft bloom glow
            drawCircle(
                color = Color(0x40C7F4FF),
                radius = headRadius + 4.dp.toPx(),
                center = Offset(headX, centerY)
            )

            // Inner solid head bead
            drawCircle(
                color = Color(0xFFEEFFFF),
                radius = headRadius,
                center = Offset(headX, centerY)
            )

            // Optional: Circular 12-petal flower pattern for CIRCULAR_FLOWER style
            if (timelineStyle == TimelineStyle.CIRCULAR_FLOWER) {
                rotate(
                    degrees = if (isPlaying) flowerRotation else 0f,
                    pivot = Offset(headX, centerY)
                ) {
                    drawRosetteFlower(
                        center = Offset(headX, centerY),
                        radius = 12.dp.toPx(),
                        color = Color(0xFFD5F6FF)
                    )
                }
            }
        }
    }
}

/**
 * Draws the 12-petal hypotrochoid rosette flower from the prototype SVG
 */
private fun DrawScope.drawRosetteFlower(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    val petals = 12
    val points = 72
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * (2 * PI.toFloat())
        // R + r rosette equation
        val r = radius * (0.75f + 0.25f * cos(petals * angle))
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.3.dp.toPx())
    )
}

private fun formatTimelineTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
