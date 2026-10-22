package com.cineflow.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cineflow.tube.ui.theme.CineFlow

/**
 * Frosted glass container featuring:
 * - Dynamic alpha adaptation
 * - Specular 0.75dp light rim
 * - Clean boundary clipping
 */
@Composable
fun FrostedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = CineFlow.tokens.glass,
    borderWidth: Dp = CineFlow.tokens.specularRimWidth,
    borderColor: Color = CineFlow.tokens.specularRimColor,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .specularBorder(shape = shape, width = borderWidth, color = borderColor),
        content = content
    )
}
