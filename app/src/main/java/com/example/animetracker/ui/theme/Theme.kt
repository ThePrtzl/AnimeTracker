package com.example.animetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** "Deep Ink" palette from the approved designs. */
object Ink {
    val Background = Color(0xFF06060C)
    val Surface = Color(0xFF0B0B16)
    val NavBar = Color(0xFF0A0A14)
    val Text = Color(0xFFF1F2FA)
    val TextSoft = Color(0xFFD0D3EA)
    val TextMuted = Color(0xFFB6BCD6)
    val Hint = Color(0xFF9AA0B8)
    val Accent = Color(0xFF79BFFF)
    /** Text/icons drawn on top of Accent and status colours. */
    val OnAccent = Color(0xFF06101A)

    val Started = Color(0xFF8AB9FF)
    val Completed = Color(0xFF6FE3A6)
    val Dropped = Color(0xFFFFA3A3)
    val NotStarted = Color(0xFFB6BCD6)

    /** White at 5%, 8% and 12–14% for glass fills and borders. */
    val GlassFill = Color(0x0DFFFFFF)
    val GlassBorder = Color(0x24FFFFFF)
    val GlassBorderSoft = Color(0x1FFFFFFF)
    /** Dark panel drawn over posters (Compose has no cheap backdrop blur). */
    val PosterPanel = Color(0xAD080812)
}

@Composable
fun AnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.Accent,
            onPrimary = Ink.OnAccent,
            background = Ink.Background,
            onBackground = Ink.Text,
            surface = Ink.Surface,
            onSurface = Ink.Text,
            surfaceContainerLow = Ink.Surface,
            error = Ink.Dropped,
        ),
        content = content,
    )
}
