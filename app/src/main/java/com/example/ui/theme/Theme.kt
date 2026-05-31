package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFFFD700),
    background = Color(0xFF0C0F14),
    surface = Color(0xFF161B22),
    onPrimary = Color(0xFF0C0F14),
    onSecondary = Color(0xFF0C0F14),
    onBackground = Color(0xFFF0F4F8),
    onSurface = Color(0xFFF0F4F8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B8D4),
    secondary = Color(0xFFFFC400),
    background = Color(0xFFF9FBFD),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFF0C0F14),
    onSurface = Color(0xFF0C0F14)
)

@Composable
fun SFISpectralLabTheme(
    darkTheme: Boolean = true, // Force Dark mode as requested by context theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
