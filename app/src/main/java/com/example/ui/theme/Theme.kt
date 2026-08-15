package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CfOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = CfOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = CfOrangeSecondary,
    onSecondary = Color.Black,
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    background = DarkSlateBackground,
    onBackground = OffWhiteText,
    surface = DarkSlateSurface,
    onSurface = OffWhiteText,
    surfaceVariant = DarkSlateCard,
    onSurfaceVariant = MutedText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sleek tech dark theme by default
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

