package com.cineflow.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ambient blurred theatrical backdrop glow.
 * Softly diffuses radiant accent tones behind video playback or feed surfaces.
 */
@Composable
fun AmbientBackdropGlow(
    accentColor: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 64.dp,
    intensity: Float = 0.28f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(blurRadius)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = intensity),
                        Color.Transparent
                    )
                )
            )
    )
}
