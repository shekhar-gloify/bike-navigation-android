package com.dopetechindia.bikenavigation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * High-Contrast Dark Color Scheme for night riding & high-visibility HUD displays.
 */
private val HighContrastDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = ElectricBlue,
    secondary = NeonYellow,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF332B00),
    onSecondaryContainer = NeonYellow,
    tertiary = VividGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = HighContrastTextPrimary,
    surface = DarkSurface,
    onSurface = HighContrastTextPrimary,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = HighContrastTextSecondary,
    outline = DarkBorder,
    error = DangerRed
)

/**
 * Clean & Crisp Light Color Scheme for daytime legibility.
 */
private val LightColorScheme = lightColorScheme(
    primary = BrightPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF003366),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = VividGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = CrispDarkText,
    surface = LightSurface,
    onSurface = CrispDarkText,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = DangerRed
)

/**
 * Main application theme wrapper supporting Light, High-Contrast Dark, and Dynamic Color modes.
 *
 * @param darkTheme Whether dark mode styling should be applied.
 * @param dynamicColor Whether Android 12+ dynamic wallpaper colors should be enabled.
 * @param highContrastMode Whether high-contrast night HUD mode is explicitly forced.
 * @param content Composable child layout elements.
 */
@Composable
fun BikeNavigationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    highContrastMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastMode -> HighContrastDarkColorScheme
        darkTheme -> HighContrastDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
