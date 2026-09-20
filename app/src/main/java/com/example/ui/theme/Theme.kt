package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldWarm,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF232A38),
    onPrimaryContainer = GoldWarm,
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF003258),
    background = DarkReaderBackground,
    onBackground = DarkReaderText,
    surface = DarkReaderSurface,
    onSurface = DarkReaderText,
    surfaceVariant = Color(0xFF242830),
    onSurfaceVariant = Color(0xFFB0B4BC),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEF9),
    onPrimaryContainer = RoyalNavy,
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
        "dark" -> DarkColorScheme
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
