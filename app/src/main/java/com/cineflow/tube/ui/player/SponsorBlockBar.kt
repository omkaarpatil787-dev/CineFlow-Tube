package com.cineflow.tube.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cineflow.tube.data.model.SponsorSegment
import com.cineflow.tube.ui.theme.SponsorBlockInteraction
import com.cineflow.tube.ui.theme.SponsorBlockIntro
import com.cineflow.tube.ui.theme.SponsorBlockSelfPromo
import com.cineflow.tube.ui.theme.SponsorBlockSponsor

@Composable
fun SponsorBlockBar(
    segments: List<SponsorSegment>,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    if (durationMs <= 0 || segments.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val width = size.width
        val height = size.height

        segments.forEach { segment ->
            val startRatio = (segment.startSeconds * 1000f) / durationMs
            val endRatio = (segment.endSeconds * 1000f) / durationMs

            val startX = (startRatio * width).coerceIn(0f, width)
            val endX = (endRatio * width).coerceIn(0f, width)
            val segmentWidth = (endX - startX).coerceAtLeast(2f)

            val color = when (segment.category.lowercase()) {
                "sponsor" -> SponsorBlockSponsor
                "intro", "outro" -> SponsorBlockIntro
                "selfpromo" -> SponsorBlockSelfPromo
                "interaction" -> SponsorBlockInteraction
                else -> Color(0xFF00E5FF)
            }

            drawRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = Size(segmentWidth, height)
            )
        }
    }
}
