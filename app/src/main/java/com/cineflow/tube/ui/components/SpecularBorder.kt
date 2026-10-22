package com.cineflow.tube.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.cineflow.tube.ui.theme.CineFlow

/**
 * Applies Jadzumi's signature 0.75dp specular hairline rim to any Composable shape.
 * Catches ambient light gracefully without heavy boxy visual clutter.
 */
@Composable
fun Modifier.specularBorder(
    shape: Shape,
    width: Dp = CineFlow.tokens.specularRimWidth,
    color: Color = CineFlow.tokens.specularRimColor
): Modifier = this.border(
    width = width,
    color = color,
    shape = shape
)
