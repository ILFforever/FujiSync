package com.paeki.fujirecipes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FujiDarkScheme = darkColorScheme(
    background = Bg,
    onBackground = TextPrimary,
    surface = PanelLow,
    onSurface = TextPrimary,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = TextMuted,
    primary = Gold,
    onPrimary = Bg,
    outline = Border,
    outlineVariant = BorderStrong,
    error = androidx.compose.ui.graphics.Color(0xFFD94040),
)

@Composable
fun FujiRecipesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FujiDarkScheme,
        typography = FujiTypography,
        content = content,
    )
}
