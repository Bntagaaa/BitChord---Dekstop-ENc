package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Always-visible Windows caption above the application.
 *
 * The empty portion is native HTCAPTION (see native_caption.cpp), so Windows owns dragging,
 * double-click maximize/restore and Aero movement. The three buttons are client controls because
 * AWT's undecorated peer cannot make DWM paint its standard buttons after the client is extended
 * through the full frame. They deliberately copy Windows' layout and use native system commands.
 *
 * With Mica/Acrylic active this surface stays transparent, letting DWM's material show through.
 */
@Composable
internal fun DesktopNativeTitleBar() {
    val metrics by DesktopWindowsFrame.captionMetrics.collectAsState()
    val backdrop by DesktopWindowBackdrop.active.collectAsState()
    val actions = LocalDesktopWindowActions.current
    val maximized by DesktopWindowMode.maximized.collectAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .height(metrics.heightDp.dp)
            .background(if (backdrop == DesktopBackdrop.OFF) WINDOWS_CAPTION_SOLID else Color.Transparent)
            .semantics { paneTitle = "Window title bar" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            "BitChord",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = (metrics.rightInsetDp + 12f).dp),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (actions != null) {
            val buttonWidth = (metrics.rightInsetDp / WINDOWS_BUTTON_COUNT)
                .coerceIn(40f, 64f)
                .dp
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            ) {
                WindowsCaptionButton(
                    kind = WindowsCaptionButtonKind.MINIMIZE,
                    label = "Minimize",
                    width = buttonWidth,
                    onClick = actions.minimize,
                )
                WindowsCaptionButton(
                    kind = if (maximized) WindowsCaptionButtonKind.RESTORE else WindowsCaptionButtonKind.MAXIMIZE,
                    label = if (maximized) "Restore" else "Maximize",
                    width = buttonWidth,
                    onClick = actions.toggleMaximize,
                )
                WindowsCaptionButton(
                    kind = WindowsCaptionButtonKind.CLOSE,
                    label = "Close",
                    width = buttonWidth,
                    onClick = actions.close,
                )
            }
        }
    }
}

private enum class WindowsCaptionButtonKind { MINIMIZE, MAXIMIZE, RESTORE, CLOSE }

@Composable
private fun WindowsCaptionButton(
    kind: WindowsCaptionButtonKind,
    label: String,
    width: Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        kind == WindowsCaptionButtonKind.CLOSE && pressed -> WINDOWS_CLOSE_PRESSED
        kind == WindowsCaptionButtonKind.CLOSE && hovered -> WINDOWS_CLOSE_HOVER
        pressed -> Color.White.copy(alpha = 0.16f)
        hovered -> Color.White.copy(alpha = 0.09f)
        else -> Color.Transparent
    }
    val glyph = Color.White.copy(alpha = if (hovered || pressed) 0.98f else 0.86f)

    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(fill)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .drawBehind {
                val line = 1.dp.toPx().coerceAtLeast(1f)
                val half = 5.dp.toPx()
                when (kind) {
                    WindowsCaptionButtonKind.MINIMIZE -> {
                        drawLine(
                            glyph,
                            Offset(center.x - half, center.y + 2.dp.toPx()),
                            Offset(center.x + half, center.y + 2.dp.toPx()),
                            line,
                            StrokeCap.Square,
                        )
                    }
                    WindowsCaptionButtonKind.MAXIMIZE -> {
                        drawRect(
                            glyph,
                            topLeft = Offset(center.x - half, center.y - half),
                            size = Size(half * 2f, half * 2f),
                            style = Stroke(width = line),
                        )
                    }
                    WindowsCaptionButtonKind.RESTORE -> {
                        val shift = 2.dp.toPx()
                        val box = 8.dp.toPx()
                        drawRect(
                            glyph,
                            topLeft = Offset(center.x - box / 2f + shift, center.y - box / 2f - shift),
                            size = Size(box, box),
                            style = Stroke(width = line),
                        )
                        drawRect(
                            glyph,
                            topLeft = Offset(center.x - box / 2f - shift, center.y - box / 2f + shift),
                            size = Size(box, box),
                            style = Stroke(width = line),
                        )
                    }
                    WindowsCaptionButtonKind.CLOSE -> {
                        drawLine(
                            glyph,
                            Offset(center.x - half, center.y - half),
                            Offset(center.x + half, center.y + half),
                            line,
                            StrokeCap.Square,
                        )
                        drawLine(
                            glyph,
                            Offset(center.x + half, center.y - half),
                            Offset(center.x - half, center.y + half),
                            line,
                            StrokeCap.Square,
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {}
}

/**
 * Source-compatibility facade for old call sites. This is no longer a preference: Windows always
 * has its dedicated caption and any persisted window_title_bar value is ignored.
 */
internal object DesktopTitleBarSetting {
    val enabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    fun set(@Suppress("UNUSED_PARAMETER") value: Boolean) = Unit
}

/** The real Windows caption lives in Main.kt, outside the app and every flyout. */
@Composable
internal fun DesktopTitleBar() = Unit

/** Legacy traffic-light controls are retired. */
@Composable
internal fun DesktopWindowButtons(@Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier) = Unit

/** Application toolbars are ordinary client content; only DesktopNativeTitleBar is a caption. */
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

private const val WINDOWS_BUTTON_COUNT = 3f
private val WINDOWS_CAPTION_SOLID = Color(0xFF202020)
private val WINDOWS_CLOSE_HOVER = Color(0xFFC42B1C)
private val WINDOWS_CLOSE_PRESSED = Color(0xFFA6261C)
