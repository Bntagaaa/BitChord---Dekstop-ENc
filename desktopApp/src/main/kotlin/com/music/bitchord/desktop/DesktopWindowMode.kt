package com.music.bitchord.desktop

import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The regular window's placement and the player's temporary immersive presentation. */
internal object DesktopWindowMode {
    private val _placement = MutableStateFlow(WindowPlacement.Floating)
    private val _fullScreen = MutableStateFlow(false)
    private val _maximized = MutableStateFlow(false)
    private var placementBeforeFullScreen = WindowPlacement.Floating

    val placement: StateFlow<WindowPlacement> = _placement
    val fullScreen: StateFlow<Boolean> = _fullScreen
    val maximized: StateFlow<Boolean> = _maximized

    fun toggle() = setFullScreen(!_fullScreen.value)

    fun toggleMaximized() {
        if (_fullScreen.value) setFullScreen(false)
        set(if (_placement.value == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized)
    }

    fun exit() {
        if (_fullScreen.value) setFullScreen(false)
    }

    /** Pick up OS caption controls, dragging and window-manager changes. */
    fun adopt(placement: WindowPlacement) {
        if (placement == _placement.value) return
        if (!DesktopPlatform.isWindows && _fullScreen.value && placement != WindowPlacement.Fullscreen) {
            _fullScreen.value = false
        }
        set(placement)
    }

    private fun setFullScreen(on: Boolean) {
        if (on == _fullScreen.value) return
        if (on) {
            placementBeforeFullScreen = _placement.value
            _fullScreen.value = true
            // On Windows the native Win32 frame is made borderless instead. Asking Skiko for
            // WindowPlacement.Fullscreen used to turn the entire desktop surface white.
            if (!DesktopPlatform.isWindows) set(WindowPlacement.Fullscreen)
        } else {
            _fullScreen.value = false
            if (!DesktopPlatform.isWindows) set(placementBeforeFullScreen)
        }
    }

    private fun set(placement: WindowPlacement) {
        _placement.value = placement
        _maximized.value = placement == WindowPlacement.Maximized
    }
}
