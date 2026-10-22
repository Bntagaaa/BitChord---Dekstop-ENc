package com.music.bitchord.ui.player

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Desktop side-panel appearance. The main player and Android continue to use white artwork
 * lyrics; desktop provides a contrasting palette only inside its standalone lyrics sidebar.
 */
data class LyricsPanelColors(
    val primary: Color = Color.White,
    val secondary: Color = Color.White.copy(alpha = 0.65f),
    val inactive: Color = Color.White,
    val iconSurface: Color = Color.White,
    val selection: Color = Color.White,
    val isLight: Boolean = false,
)

val LocalLyricsPanelColors = staticCompositionLocalOf { LyricsPanelColors() }
