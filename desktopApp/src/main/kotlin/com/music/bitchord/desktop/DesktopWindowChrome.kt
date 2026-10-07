package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Always-visible caption space, hosted ABOVE DesktopFlyoutHost and the entire app background.
 * Do not add an opaque fill here: DWM paints its real caption buttons over zero-alpha pixels in
 * the extended frame. No Compose clickable/focusable node may cover the caption controls.
 * The native window procedure supplies HTCAPTION for the rest of this strip.
 */
@Composable
internal fun DesktopNativeTitleBar() {
    val metrics by DesktopWindowsFrame.captionMetrics.collectAsState()
    Box(
        Modifier.fillMaxWidth().height(metrics.heightDp.dp)
            .semantics { paneTitle = "Window title bar" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            "BitChord",
            modifier = Modifier.fillMaxWidth()
                .padding(start = 12.dp, end = (metrics.rightInsetDp + 12f).dp),
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Source-compatibility facade for the old DesktopApp call sites. It is NOT a preference: the old
 * window_title_bar value is neither read nor written. The Settings row and inline button slots
 * are unreachable because drawsOwnWindowFrame is false. These facades keep this change separate
 * from the large application host and its recently fixed focus routing.
 */
internal object DesktopTitleBarSetting {
    val enabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    fun set(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
}

/** The native caption lives in Main.kt, outside the app; never add a second strip inside it. */
@Composable
internal fun DesktopTitleBar() = Unit

/** Windows/DWM and Linux's window manager paint their own buttons. */
@Composable
internal fun DesktopWindowButtons(@Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier) = Unit

/** The player toolbar is ordinary client content, no longer a second draggable title bar. */
@Composable
internal fun DesktopTitleBarDragArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier) { content() }
}

internal class DesktopWindowActions(
    val minimize: () -> Unit,
    val toggleMaximize: () -> Unit,
    val close: () -> Unit,
)

internal val LocalDesktopWindowActions = staticCompositionLocalOf<DesktopWindowActions?> { null }
internal val LocalDesktopWindowScope = staticCompositionLocalOf<WindowScope?> { null }
