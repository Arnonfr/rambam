package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE7AE7D),
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF503621),
    onPrimaryContainer = GoldWarm,
    secondaryContainer = Color(0xFF4B3B2C),
    onSecondaryContainer = Color(0xFFF1DCC6),
    tertiary = Color(0xFFD7B798),
    tertiaryContainer = Color(0xFF4C3826),
    onTertiaryContainer = Color(0xFFF1DCC6),
    surfaceContainer = Color(0xFF28221D),
    surfaceContainerHigh = Color(0xFF352C24),
    surfaceContainerHighest = Color(0xFF41352B),
    secondary = Color(0xFFD4B79A),
    onSecondary = Color(0xFF382718),
    background = DarkReaderBackground,
    onBackground = DarkReaderText,
    surface = DarkReaderSurface,
    onSurface = DarkReaderText,
    surfaceVariant = Color(0xFF242830),
    onSurfaceVariant = Color(0xFFB0B4BC),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF99552D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DCC5),
    onPrimaryContainer = Color(0xFF482A18),
    secondaryContainer = Color(0xFFF0E1CF),
    onSecondaryContainer = Color(0xFF543C27),
    tertiary = Color(0xFF946443),
    tertiaryContainer = Color(0xFFF0DDC4),
    onTertiaryContainer = Color(0xFF422C1C),
    surfaceContainer = Color(0xFFF6EDE2),
    surfaceContainerHigh = Color(0xFFEEE1D2),
    surfaceContainerHighest = Color(0xFFE7D7C5),
    secondary = GoldAccent,
    onSecondary = Color.White,
    background = ParchmentLight,
    onBackground = Color(0xFF1E2124),
    surface = ParchmentSurface,
    onSurface = Color(0xFF1E2124),
    surfaceVariant = Color(0xFFF1EDE6),
    onSurfaceVariant = Color(0xFF5A5852),
    outline = BorderParchment
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF5D4037),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6D7C3),
    onPrimaryContainer = Color(0xFF3E2723),
    secondaryContainer = Color(0xFFF0E1CF),
    onSecondaryContainer = Color(0xFF543C27),
    tertiary = Color(0xFF946443),
    tertiaryContainer = Color(0xFFF0DDC4),
    onTertiaryContainer = Color(0xFF422C1C),
    surfaceContainer = Color(0xFFF6EDE2),
    surfaceContainerHigh = Color(0xFFEEE1D2),
    surfaceContainerHighest = Color(0xFFE7D7C5),
    secondary = GoldAccent,
    onSecondary = Color.White,
    background = SepiaBackground,
    onBackground = SepiaText,
    surface = SepiaSurface,
    onSurface = SepiaText,
    surfaceVariant = Color(0xFFE8DFC9),
    onSurfaceVariant = Color(0xFF5D4A32),
    outline = Color(0xFFD8CCB5)
)

@Composable
fun DailyRambamTheme(
    readerTheme: String = "light", // "light", "sepia", "dark"
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (readerTheme) {
        "dark", "oled" -> DarkColorScheme
        "sepia" -> SepiaColorScheme
        "light" -> LightColorScheme
        else -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
