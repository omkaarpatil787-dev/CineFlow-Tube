package com.cineflow.tube.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * CineFlow Motion System — Direct Implementation of cineflow_motion_lab.html
 * Incorporates the Six Restrained Motion Patterns:
 * 1. Icon Morph (300ms Emphasized)
 * 2. Spring Response (Damping 0.72, Stiffness 380)
 * 3. Spatial Continuity (420ms Gliding Navigation)
 * 4. Container Transform (460ms Bounds Transform)
 * 5. Seek Feedback (360ms Ripple + 200ms Compression)
 * 6. Theme Bloom (580ms Circular Expansion)
 */
object CineFlowMotion {
    // 01 / ICON MORPH
    const val DurationIconMorph = 300
    val IconMorphEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
    val IconMorphTween = tween<Float>(durationMillis = DurationIconMorph, easing = IconMorphEasing)

    // 02 / SPRING RESPONSE (Damping ratio 0.72 · Stiffness 380)
    const val PressDampingRatio = 0.72f
    const val PressStiffness = 380f
    const val PressTargetScale = 0.94f

    val PressSpring = spring<Float>(
        dampingRatio = PressDampingRatio,
        stiffness = PressStiffness
    )

    // 03 / SPATIAL CONTINUITY (420ms with subtle overshoot settle)
    const val DurationNavGlide = 420
    val NavGlideEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1.08f)
    val NavGlideTween = tween<Float>(durationMillis = DurationNavGlide, easing = NavGlideEasing)

    // 04 / CONTAINER TRANSFORM (460ms Bounds Transform)
    const val DurationContainerTransform = 460
    val ContainerTransformEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
    val ContainerTransformTween = tween<Float>(
        durationMillis = DurationContainerTransform,
        easing = ContainerTransformEasing
    )

    // 05 / SEEK FEEDBACK (360ms Ease Out Opacity + Scale)
    const val DurationSeekRipple = 360
    const val DurationSeekButton = 200
    val SeekRippleEasing = FastOutSlowInEasing
    val SeekRippleTween = tween<Float>(durationMillis = DurationSeekRipple, easing = SeekRippleEasing)

    // 06 / THEME BLOOM (580ms Circular Expansion)
    const val DurationThemeBloom = 580
    val ThemeBloomEasing = LinearOutSlowInEasing

    // Shared Entrance & Exit Transitions (from Jadzumi Player)
    val FluidEnter = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val FluidExit = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    const val DurationQuick = 180
    const val DurationMedium = 320
}

/**
 * Pattern 02: Spring Response Modifier
 * Press and hold scales the composable 1.0 -> 0.94.
 * Releasing returns immediately with a damped spring (Damping 0.72, Stiffness 380).
 * Operates purely on GPU graphicsLayer scale with zero layout resizing.
 * Respects CineFlow.reducedMotion.
 */
fun Modifier.springPress(
    enabled: Boolean = true,
    targetScale: Float = CineFlowMotion.PressTargetScale,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val isReducedMotion = CineFlow.reducedMotion
    if (!enabled) return@composed this

    if (isReducedMotion) {
        return@composed if (onClick != null) {
            this.pointerInput(enabled) {
                detectTapGestures(onTap = { onClick() })
            }
        } else this
    }

    val scale = remember { Animatable(1.0f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(targetScale, CineFlowMotion.PressSpring)
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(1.0f, CineFlowMotion.PressSpring)
                    }
                    if (released && onClick != null) {
                        onClick()
                    }
                }
            )
        }
}
