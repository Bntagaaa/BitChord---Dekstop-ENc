package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo

private val LocalDesktopFlyouts = staticCompositionLocalOf<DesktopFlyoutState?> { null }

/**
 * Sits above DesktopFrame's preview handler so a flyout's Escape cannot close the player beneath.
 * This adds no native popup and deliberately adds no focus target: the requester's first target
 * remains DesktopFrame's existing shortcut root, not an outer node that would bypass shortcuts.
 */
@Composable
internal fun DesktopFlyoutHost(content: @Composable () -> Unit) {
    val state = remember { DesktopFlyoutState() }
    val root = remember { FocusRequester() }
    var rootHasFocus by remember { mutableStateOf(false) }
    val currentRootHasFocus by rememberUpdatedState(rootHasFocus)
    val windowInfo = LocalWindowInfo.current
    val snapshot by state.snapshot.collectAsState()
    var restoredRevision by remember { mutableStateOf(0L) }

    LaunchedEffect(windowInfo.isWindowFocused) {
        if (!windowInfo.isWindowFocused) state.cancelPendingEscape()
    }
    LaunchedEffect(snapshot.revision, windowInfo.isWindowFocused, rootHasFocus) {
        if (snapshot.count != 0 || snapshot.closedRevision <= restoredRevision || !windowInfo.isWindowFocused) {
            return@LaunchedEffect
        }
        val expectedRevision = snapshot.revision
        repeat(3) {
            withFrameNanos { }
            if (!windowInfo.isWindowFocused || state.snapshot.value.revision != expectedRevision) {
                return@LaunchedEffect
            }
            // Preserve a real editor or another focused control. Only repair missing focus.
            if (currentRootHasFocus || runCatching { root.requestFocus() }.getOrDefault(false)) {
                restoredRevision = snapshot.closedRevision
                return@LaunchedEffect
            }
        }
    }

    CompositionLocalProvider(LocalDesktopFlyouts provides state) {
        Box(
            Modifier.fillMaxSize()
                .focusRequester(root)
                .onFocusChanged { rootHasFocus = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> state.handle(event.key.nativeKeyCode, pressed = true)
                        KeyEventType.KeyUp -> state.handle(event.key.nativeKeyCode, pressed = false)
                        else -> false
                    }
                },
        ) {
            content()
        }
    }
}

/** Null onDismiss is an Escape barrier, for an unrelated dialog whose behavior we must preserve. */
@Composable
internal fun DesktopRegisterFlyout(
    layer: DesktopFlyoutLayer,
    onDismiss: (() -> Unit)? = null,
    delegateEscape: Boolean = false,
) {
    val state = LocalDesktopFlyouts.current
    val token = remember { Any() }
    val currentDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(state, token, layer, delegateEscape, onDismiss != null) {
        state?.register(
            token = token,
            layer = layer,
            delegateEscape = delegateEscape,
            onDismiss = if (onDismiss == null) null else { { currentDismiss?.invoke() } },
        )
        onDispose { state?.unregister(token) }
    }
}
