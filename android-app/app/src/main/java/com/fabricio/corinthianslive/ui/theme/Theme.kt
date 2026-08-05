package com.fabricio.corinthianslive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CorinthiansColors.Red,
    onPrimary = CorinthiansColors.White,
    background = CorinthiansColors.Background,
    onBackground = CorinthiansColors.OnSurface,
    surface = CorinthiansColors.Surface,
    onSurface = CorinthiansColors.OnSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F0F3),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF515159),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD5D5DB)
)

private val DarkColors = darkColorScheme(
    primary = CorinthiansColors.Red,
    onPrimary = CorinthiansColors.White,
    background = CorinthiansColors.DarkBackground,
    onBackground = CorinthiansColors.White,
    surface = CorinthiansColors.DarkSurface,
    onSurface = CorinthiansColors.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF252529),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD0D0D6),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3C3C42)
)

@Composable
fun CorinthiansTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
