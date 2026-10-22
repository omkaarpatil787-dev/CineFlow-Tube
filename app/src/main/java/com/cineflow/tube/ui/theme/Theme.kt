package com.cineflow.tube.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

// Unified Design Tokens mapped directly from Jadzumi Stream v0.2 and CineFlow prototypes
data class CineFlowDesignTokens(
    val specularRimWidth: Dp = 0.75.dp,
    val specularRimColor: Color = DarkSapphireLine,
    val accent: Color = StreamAccentDark,
    val bg: Color = DarkSapphireBg,
    val surface: Color = DarkSapphireSurface,
    val soft: Color = DarkSapphireSoft,
    val glass: Color = DarkSapphireGlass,
    val ink: Color = DarkSapphireInk,
    val muted: Color = DarkSapphireMuted,
    val line: Color = DarkSapphireLine,
    val cardRadius: Dp = 18.dp,
    val buttonRadius: Dp = 14.dp,
    val pillRadius: Dp = 100.dp,
    val sheetRadius: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp,
    val elevationLow: Dp = 4.dp,
    val elevationMedium: Dp = 12.dp,
    val elevationHigh: Dp = 24.dp,
    val skeletonBase: Color = SkeletonBaseDark,
    val skeletonHighlight: Color = SkeletonHighlightDark,
    val isDark: Boolean = true,
    val isAmoled: Boolean = false
)

val LocalCineFlowTokens = staticCompositionLocalOf { CineFlowDesignTokens() }
val LocalReducedMotion = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = StreamAccentLight,
    onPrimary = Color.White,
    primaryContainer = LightSoft,
    onPrimaryContainer = StreamAccentLight,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSoft,
    onBackground = LightInk,
    onSurface = LightInk,
    onSurfaceVariant = LightMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = StreamAccentDark,
    onPrimary = Color.Black,
    primaryContainer = DarkSapphireSoft,
    onPrimaryContainer = StreamAccentDark,
    background = DarkSapphireBg,
    surface = DarkSapphireSurface,
    surfaceVariant = DarkSapphireSoft,
    onBackground = DarkSapphireInk,
    onSurface = DarkSapphireInk,
    onSurfaceVariant = DarkSapphireMuted
)

private val AmoledColorScheme = darkColorScheme(
    primary = StreamAccentAmoled,
    onPrimary = Color.Black,
    primaryContainer = AmoledSoft,
    onPrimaryContainer = StreamAccentAmoled,
    background = AmoledBg,
    surface = AmoledSurface,
    surfaceVariant = AmoledSoft,
    onBackground = AmoledInk,
    onSurface = AmoledInk,
    onSurfaceVariant = AmoledMuted
)

@Composable
fun CineFlowTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    enableDynamicColor: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val effectiveDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }

    val isAmoled = themeMode == AppThemeMode.AMOLED

    // Dynamic Material You support on Android 12+ (if enabled by user)
    val dynamicScheme = if (enableDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val colorScheme = when {
        dynamicScheme != null -> dynamicScheme
        isAmoled -> AmoledColorScheme
        effectiveDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val tokens = when {
        isAmoled -> CineFlowDesignTokens(
            specularRimWidth = 0.75.dp,
            specularRimColor = AmoledLine,
            accent = dynamicScheme?.primary ?: StreamAccentAmoled,
            bg = AmoledBg,
            surface = AmoledSurface,
            soft = AmoledSoft,
            glass = AmoledGlass,
            ink = AmoledInk,
            muted = AmoledMuted,
            line = AmoledLine,
            skeletonBase = SkeletonBaseAmoled,
            skeletonHighlight = SkeletonHighlightAmoled,
            isDark = true,
            isAmoled = true
        )
        effectiveDark -> CineFlowDesignTokens(
            specularRimWidth = 0.75.dp,
            specularRimColor = DarkSapphireLine,
            accent = dynamicScheme?.primary ?: StreamAccentDark,
            bg = DarkSapphireBg,
            surface = DarkSapphireSurface,
            soft = DarkSapphireSoft,
            glass = DarkSapphireGlass,
            ink = DarkSapphireInk,
            muted = DarkSapphireMuted,
            line = DarkSapphireLine,
            skeletonBase = SkeletonBaseDark,
            skeletonHighlight = SkeletonHighlightDark,
            isDark = true,
            isAmoled = false
        )
        else -> CineFlowDesignTokens(
            specularRimWidth = 0.75.dp,
            specularRimColor = LightLine,
            accent = dynamicScheme?.primary ?: StreamAccentLight,
            bg = LightBg,
            surface = LightSurface,
            soft = LightSoft,
            glass = LightGlass,
            ink = LightInk,
            muted = LightMuted,
            line = LightLine,
            skeletonBase = SkeletonBaseLight,
            skeletonHighlight = SkeletonHighlightLight,
            isDark = false,
            isAmoled = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !effectiveDark
                controller.isAppearanceLightNavigationBars = !effectiveDark
            }
        }
    }

    CompositionLocalProvider(
        LocalCineFlowTokens provides tokens,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CineFlowTypography,
            content = content
        )
    }
}

object CineFlow {
    val tokens: CineFlowDesignTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCineFlowTokens.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}
