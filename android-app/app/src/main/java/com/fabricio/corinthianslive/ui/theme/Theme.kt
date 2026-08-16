package com.fabricio.corinthianslive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkPremiumColors = darkColorScheme(
    primary = CorinthiansColors.Red,
    onPrimary = CorinthiansColors.White,
    primaryContainer = CorinthiansColors.Wine,
    onPrimaryContainer = Color(0xFFFFE8ED),
    secondary = CorinthiansColors.Gold,
    onSecondary = CorinthiansColors.Black,
    secondaryContainer = Color(0xFF3A301D),
    onSecondaryContainer = Color(0xFFFFE9B0),
    background = CorinthiansColors.DarkBackground,
    onBackground = CorinthiansColors.White,
    surface = CorinthiansColors.DarkSurfaceElevated,
    onSurface = CorinthiansColors.White,
    surfaceVariant = CorinthiansColors.DarkSurfaceSoft,
    onSurfaceVariant = Color(0xFFC7C8D0),
    outline = Color(0xFF777985),
    outlineVariant = CorinthiansColors.DarkBorder,
    error = Color(0xFFFF5C72)
)

private val LightPremiumColors = lightColorScheme(
    primary = Color(0xFFD8082A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8ED),
    onPrimaryContainer = Color(0xFF4A0010),
    secondary = Color(0xFF785C18),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9B0),
    onSecondaryContainer = Color(0xFF261A00),
    background = Color(0xFFF7F5F4),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFF0ECEB),
    onSurfaceVariant = Color(0xFF565159),
    outline = Color(0xFF7B7478),
    outlineVariant = Color(0xFFD9D2D5),
    error = Color(0xFFBA1A1A)
)

@Composable
fun CorinthiansTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPremiumColors else LightPremiumColors,
        typography = Typography,
        content = content
    )
}
