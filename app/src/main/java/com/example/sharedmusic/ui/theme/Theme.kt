package com.example.sharedmusic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.sharedmusic.data.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = SharyPink,
    onPrimary = White,
    primaryContainer = SharyPinkDeep,
    onPrimaryContainer = White,
    secondary = SharyPinkSoft,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ErrorRed,
    onError = White
)

private val LightColors = lightColorScheme(
    primary = SharyPink,
    onPrimary = White,
    primaryContainer = SharyPinkSoft,
    onPrimaryContainer = LightTextPrimary,
    secondary = SharyPinkDeep,
    onSecondary = White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ErrorRed,
    onError = White
)

/**
 * Тема приложения: светлая и тёмная схема, единая типографика.
 * Тёмная тема используется по умолчанию, как в макетах.
 */
@Composable
fun SharyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect

            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SharyTypography,
        content = content
    )
}
