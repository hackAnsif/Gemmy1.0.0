package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GemmyDarkColorScheme = darkColorScheme(
    primary = GemmyDarkPrimary,
    onPrimary = GemmyDarkOnPrimary,
    primaryContainer = GemmyDarkPrimaryContainer,
    onPrimaryContainer = GemmyDarkOnPrimaryContainer,
    secondary = GemmyDarkSecondary,
    onSecondary = GemmyDarkOnSecondary,
    secondaryContainer = GemmyDarkSecondaryContainer,
    onSecondaryContainer = GemmyDarkOnSecondaryContainer,
    tertiary = GemmyDarkTertiary,
    background = GemmyDarkBackground,
    onBackground = GemmyDarkOnBackground,
    surface = GemmyDarkSurface,
    onSurface = GemmyDarkOnSurface,
    surfaceVariant = GemmyDarkSurfaceVariant,
    onSurfaceVariant = GemmyDarkOnSurfaceVariant,
    outline = GemmyDarkOutline
)

private val GemmyLightColorScheme = lightColorScheme(
    primary = GemmyIndigoPrimary,
    onPrimary = GemmyIndigoOnPrimary,
    primaryContainer = GemmyIndigoPrimaryContainer,
    onPrimaryContainer = GemmyIndigoOnPrimaryContainer,
    secondary = GemmyMintSecondary,
    onSecondary = GemmyMintOnSecondary,
    secondaryContainer = GemmyMintSecondaryContainer,
    onSecondaryContainer = GemmyMintOnSecondaryContainer,
    tertiary = GemmyVioletTertiary,
    background = GemmyLightBackground,
    onBackground = GemmyLightOnBackground,
    surface = GemmyLightSurface,
    onSurface = GemmyLightOnSurface,
    surfaceVariant = GemmyLightSurfaceVariant,
    onSurfaceVariant = GemmyLightOnSurfaceVariant,
    outline = GemmyLightOutline
)

@Composable
fun GemmyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Gemmy's signature deep-indigo brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GemmyDarkColorScheme
        else -> GemmyLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
