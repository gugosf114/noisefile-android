package com.noisefile.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** One scheme. The app looks the same at noon and at 2 AM, so every screen can be checked once. */
private val InstrumentColors = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = DeckHigh,
    onPrimaryContainer = Chalk,
    secondary = Sky,
    onSecondary = Ink,
    secondaryContainer = DeckHigh,
    onSecondaryContainer = Chalk,
    tertiary = Brass,
    onTertiary = Ink,
    error = Danger,
    onError = Ink,
    background = Night,
    onBackground = Chalk,
    surface = Deck,
    onSurface = Chalk,
    surfaceVariant = DeckHigh,
    onSurfaceVariant = Color(0xFFD1D6E0),
    surfaceContainer = Deck,
    surfaceContainerHigh = DeckHigh,
    surfaceContainerHighest = DeckHigh,
    surfaceContainerLow = Deck,
    outline = Hairline,
    outlineVariant = Hairline,
)

@Composable
fun NoiseFileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InstrumentColors,
        typography = NoiseFileTypography,
        shapes = NoiseFileShapes,
        content = content,
    )
}
