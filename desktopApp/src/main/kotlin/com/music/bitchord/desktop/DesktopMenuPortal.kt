package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Renders menus in the main window so Haze can sample the actual page below them. */
internal class DesktopMenuPortalState {
    internal val entry: MutableState<DesktopMenuPortalEntry?> = mutableStateOf(null)
    internal var onClosed: () -> Unit = {}
}

internal data class DesktopMenuPortalEntry(
    val owner: Any,
    val anchor: Rect,
    val onDismiss: () -> Unit,
    val content: @Composable () -> Unit,
)

internal val LocalDesktopMenuPortal = staticCompositionLocalOf<DesktopMenuPortalState?> { null }

@Composable
internal fun DesktopMenuPortal(state: DesktopMenuPortalState) {
    val entry = state.entry.value ?: return
    val focusRequester = remember(entry.owner) { FocusRequester() }
    LaunchedEffect(entry.owner) { focusRequester.requestFocus() }
    Box(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        Box(
            Modifier.fillMaxSize()
                .background(Color.Transparent)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                        entry.onDismiss()
                        true
                    } else if (it.key == Key.Spacebar) {
                        // The backdrop is focusable for Escape but Space must not click it.
                        true
                    } else false
                }
                .focusable()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { entry.onDismiss() },
        )
        val margin = with(density) { 12.dp.roundToPx() }
        Layout(content = entry.content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
            val child = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val below = entry.anchor.bottom.roundToInt() + margin
            val above = entry.anchor.top.roundToInt() - child.height - margin
            val y = if (below + child.height + margin <= constraints.maxHeight || above < margin) below else above
            val x = entry.anchor.left.roundToInt().coerceIn(
                margin, (constraints.maxWidth - child.width - margin).coerceAtLeast(margin),
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                child.place(x, y.coerceIn(margin, (constraints.maxHeight - child.height - margin).coerceAtLeast(margin)))
            }
        }
    }
}
