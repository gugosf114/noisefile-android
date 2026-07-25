package com.noisefile.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = White,
    secondaryContainer = Color(0xFFE7EAF0),
    onSecondaryContainer = Ink,
    tertiary = Signal,
    onTertiary = Ink,
    error = Danger,
    background = Paper,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE8DF),
    onSurfaceVariant = InkSoft,
    outline = Line,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB1FF),
    onPrimary = Color(0xFF10265F),
    primaryContainer = Color(0xFF294797),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFC1C8D7),
    onSecondary = Ink,
    tertiary = Signal,
    onTertiary = Ink,
    error = Color(0xFFFFB3B8),
    background = PaperDark,
    onBackground = Color(0xFFF3F1EA),
    surface = Color(0xFF182235),
    onSurface = Color(0xFFF3F1EA),
    surfaceVariant = Color(0xFF273247),
    onSurfaceVariant = Color(0xFFD1D6E0),
    outline = Color(0xFF434F65),
)

@Composable
fun NoiseFileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NoiseFileTypography,
        shapes = NoiseFileShapes,
        content = content,
    )
}
