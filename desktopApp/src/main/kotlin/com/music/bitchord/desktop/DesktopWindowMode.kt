package com.music.bitchord.desktop

import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How the window is sitting on the screen: floating, maximized, or filling it. */
internal object DesktopWindowMode {
    private val _placement = MutableStateFlow(WindowPlacement.Floating)
    private val _fullScreen = MutableStateFlow(false)
    private val _maximized = MutableStateFlow(false)
    val placement: StateFlow<WindowPlacement> = _placement
    val fullScreen: StateFlow<Boolean> = _fullScreen
    val maximized: StateFlow<Boolean> = _maximized

    /**
     * Keep the Windows maximize workaround independently of who draws the caption. Switching to
     * native controls must NOT accidentally enable the Skiko fullscreen path that painted white.
     * Linux continues using WindowPlacement.Fullscreen exactly as before.
     */
    private val fillsScreen: WindowPlacement =
        if (DesktopPlatform.isWindows) WindowPlacement.Maximized else WindowPlacement.Fullscreen

    fun toggle() = setFullScreen(!_fullScreen.value)

    fun toggleMaximized() {
        _fullScreen.value = false
        set(if (_placement.value == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized)
    }

    fun exit() {
        if (_fullScreen.value) setFullScreen(false)
    }

    fun adopt(placement: WindowPlacement) {
        if (placement == _placement.value) return
        if (placement == WindowPlacement.Floating) _fullScreen.value = false
        set(placement)
    }

    private fun setFullScreen(on: Boolean) {
        _fullScreen.value = on
        set(if (on) fillsScreen else WindowPlacement.Floating)
    }

    private fun set(placement: WindowPlacement) {
        _placement.value = placement
        _maximized.value = placement == WindowPlacement.Maximized
    }
}
