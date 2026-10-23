package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bitchord.desktopapp.generated.resources.Res
import bitchord.desktopapp.generated.resources.player_fullscreen_enter
import bitchord.desktopapp.generated.resources.player_fullscreen_exit
import org.jetbrains.compose.resources.painterResource

/** BitChord Web's Maximize/Minimize control, rendered over the colourful shared player. */
@Composable
internal fun DesktopPlayerFullscreenButton(modifier: Modifier = Modifier) {
    val isFullscreen by DesktopWindowMode.fullScreen.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    IconButton(
        onClick = DesktopWindowMode::toggle,
        interactionSource = interactionSource,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (hovered) Color.White.copy(alpha = 0.14f) else Color.Transparent),
    ) {
        Icon(
            painter = painterResource(
                if (isFullscreen) Res.drawable.player_fullscreen_exit
                else Res.drawable.player_fullscreen_enter,
            ),
            contentDescription = if (isFullscreen) "Exit full screen" else "Enter full screen",
            tint = Color.White.copy(alpha = if (hovered) 1f else 0.85f),
            modifier = Modifier.size(18.dp),
        )
    }
}
