package com.freedomapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = PrimaryBlueContainer,
    secondary = InfoBlue,
    tertiary = AccentGreen,
    error = AccentOrange
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    secondary = InfoBlue,
    tertiary = AccentGreen,
    error = AccentOrange
)

@Composable
fun FreedomAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
