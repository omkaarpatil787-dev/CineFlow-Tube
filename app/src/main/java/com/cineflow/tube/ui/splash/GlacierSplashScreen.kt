package com.cineflow.tube.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cineflow.tube.R
import com.cineflow.tube.ui.theme.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Spec colors from cineflow_glacier_splash.html
private val GlacierLightBase = Color(0xFFF4F9FD)
private val GlacierLightInk = Color(0xFF143E56)

private val GlacierDarkBase = Color(0xFF0B1620)
private val GlacierDarkInk = Color(0xFFDEEFFB)

private val GlacierAmoledBase = Color(0xFF000000)
private val GlacierAmoledInk = Color(0xFFE0F0FC)

private val GlacierAccentCyan = Color(0xFF00CCE5)
private val GlacierIceBlue = Color(0xFF80B7FF)
private val GlacierSoftBlue = Color(0xFFB2CEF4)

/**
 * Glacier Atmospheric Ambient Background
 * Hardware-accelerated canvas rendering 3 soft glacier bloom ellipses
 * with gentle floating phase movement matching Jadzumi Player's motion engine.
 */
@Composable
private fun GlacierAtmosphereBackground(
    themeMode: AppThemeMode,
    driftOffset: Float,
    modifier: Modifier = Modifier
) {
    val (baseColor, glowStrength) = when (themeMode) {
        AppThemeMode.LIGHT -> GlacierLightBase to 0.72f
        AppThemeMode.AMOLED -> GlacierAmoledBase to (0.72f * 0.34f)
        AppThemeMode.DARK, AppThemeMode.SYSTEM -> GlacierDarkBase to (0.72f * 0.60f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Solid Base Surface
        drawRect(baseColor)

        if (glowStrength <= 0f) return@Canvas

        // 2. Glacier Ellipse 1: (18% x, 24% y) Cyan Bloom with subtle drift
        val center1 = Offset(w * (0.18f + driftOffset * 0.03f), h * (0.24f + driftOffset * 0.02f))
        val radius1 = (w * 0.55f).coerceAtLeast(180f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GlacierAccentCyan.copy(alpha = 0.32f * glowStrength),
                    Color.Transparent
                ),
                center = center1,
                radius = radius1
            ),
            center = center1,
            radius = radius1
        )

        // 3. Glacier Ellipse 2: (83% x, 50% y) Ice Blue Bloom
        val center2 = Offset(w * (0.83f - driftOffset * 0.02f), h * (0.50f - driftOffset * 0.03f))
        val radius2 = (w * 0.58f).coerceAtLeast(200f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GlacierIceBlue.copy(alpha = 0.33f * glowStrength),
                    Color.Transparent
                ),
                center = center2,
                radius = radius2
            ),
            center = center2,
            radius = radius2
        )

        // 4. Glacier Ellipse 3: (42% x, 85% y) Soft Frost Bloom
        val center3 = Offset(w * 0.42f, h * 0.85f)
        val radius3 = (w * 0.52f).coerceAtLeast(170f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    GlacierSoftBlue.copy(alpha = 0.36f * glowStrength),
                    Color.Transparent
                ),
                center = center3,
                radius = radius3
            ),
            center = center3,
            radius = radius3
        )
    }
}

/**
 * CineFlow Tube Glacier Splash Screen
 * Exact implementation of cineflow_glacier_splash.html
 * Incorporates Jadzumi Player's signature cubic-bezier fluid entrance,
 * micro-loader progression, and seamless exit transition.
 */
@Composable
fun GlacierSplashScreen(
    themeMode: AppThemeMode,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onFinished() }

    val inkColor = when (themeMode) {
        AppThemeMode.LIGHT -> GlacierLightInk
        AppThemeMode.AMOLED -> GlacierAmoledInk
        AppThemeMode.DARK, AppThemeMode.SYSTEM -> GlacierDarkInk
    }

    // Atmospheric Gentle Floating Drift (120 FPS Infinite Loop)
    val infiniteTransition = rememberInfiniteTransition(label = "glacierAtmosphere")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftPhase"
    )

    // Gentle Levitation (-5dp to 0dp)
    val levitateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitateY"
    )

    // Staggered Entrance Animators
    val markAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.94f) }
    val markTranslateY = remember { Animatable(10f) }

    val brandAlpha = remember { Animatable(0f) }
    val brandTranslateY = remember { Animatable(8f) }

    val loaderScaleX = remember { Animatable(0.1f) }
    val loaderAlpha = remember { Animatable(0.1f) }

    // Exit Transition Animators (Hand-off to MainActivity)
    val containerScale = remember { Animatable(1.0f) }
    val containerAlpha = remember { Animatable(1.0f) }

    // Motion Choreography
    LaunchedEffect(Unit) {
        // 1. Mark Entrance: 750ms CubicBezier(0.2, 0.75, 0.2, 1)
        launch {
            markAlpha.animateTo(1f, tween(750, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)))
        }
        launch {
            markScale.animateTo(1f, tween(750, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)))
        }
        launch {
            markTranslateY.animateTo(0f, tween(750, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)))
        }

        // 2. Brand Entrance with 120ms Stagger Delay
        launch {
            delay(120)
            brandAlpha.animateTo(1f, tween(700, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)))
        }
        launch {
            delay(120)
            brandTranslateY.animateTo(0f, tween(700, easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)))
        }

        // 3. Hairline Micro-Loader Progression with 200ms Stagger Delay
        launch {
            delay(200)
            loaderAlpha.animateTo(0.25f, tween(300))
            loaderScaleX.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }

        // 4. Hold Display for Glacier Ambience (~2200ms total)
        delay(2200)

        // 5. Symmetrical Fluid Exit Transition: scales to 1.08x and fades out over 450ms
        launch {
            containerScale.animateTo(1.08f, tween(450, easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)))
        }
        launch {
            containerAlpha.animateTo(0f, tween(450, easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)))
        }

        delay(460)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(containerScale.value)
            .alpha(containerAlpha.value)
    ) {
        // 1. Glacier Atmospheric Background
        GlacierAtmosphereBackground(
            themeMode = themeMode,
            driftOffset = driftPhase
        )

        // 2. Center Content Container with Gentle Levitation
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(0, levitateY.roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Layered Matte Icon Mark (128x128dp) with Soft Shadow
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX = markScale.value
                        scaleY = markScale.value
                        alpha = markAlpha.value
                        translationY = markTranslateY.value
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0x18123B59),
                        spotColor = Color(0x22123B59)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "CineFlow Tube",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Brand Typography
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = brandAlpha.value
                        translationY = brandTranslateY.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CineFlow Tube",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.7).sp,
                    color = inkColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "BY JADZUMI",
                    fontSize = 9.sp,
                    letterSpacing = 3.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = inkColor.copy(alpha = 0.55f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Hairline Micro-Loader (34dp width x 3dp height)
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.dp)
                        .graphicsLayer {
                            scaleX = loaderScaleX.value
                            alpha = loaderAlpha.value
                        }
                        .background(inkColor, RoundedCornerShape(9.dp))
                )
            }
        }

        // 3. Bottom Ecosystem Heritage Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(brandAlpha.value)
        ) {
            Text(
                text = "PART OF JADZUMI",
                fontSize = 8.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
                color = inkColor.copy(alpha = 0.50f)
            )
        }
    }
}
