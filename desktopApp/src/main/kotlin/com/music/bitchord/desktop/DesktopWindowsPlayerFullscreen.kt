package com.music.bitchord.desktop

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Frame
import java.awt.Window

/**
 * Borderless player-only fullscreen for Windows.
 *
 * The existing decorated AWT/Skiko peer must not be recreated: switching to Compose's
 * WindowPlacement.Fullscreen painted a blank white surface on some Windows systems.
 * Change the Win32 non-client frame in place and use the monitor's full bounds (including
 * its taskbar); preserve and restore the previous style and position on exit.
 */
internal object DesktopWindowsPlayerFullscreen {
    private data class SavedWindow(
        val window: Window,
        val handle: WinDef.HWND,
        val style: Int,
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val wasMaximized: Boolean,
    )

    private var saved: SavedWindow? = null

    fun set(window: Window, fullscreen: Boolean): Boolean {
        if (!DesktopPlatform.isWindows) return true
        return runCatching {
            if (fullscreen) enter(window) else leave(window)
        }.onFailure {
            DesktopTrackLog.log("Windows player fullscreen unavailable: " + (it.message ?: it.javaClass.simpleName))
        }.getOrDefault(false)
    }

    private fun enter(window: Window): Boolean {
        if (saved?.window === window) return true
        if (saved != null || !window.isDisplayable) return false

        val user32 = User32.INSTANCE
        val handle = WinDef.HWND(Native.getWindowPointer(window))
        val bounds = WinDef.RECT()
        if (!user32.GetWindowRect(handle, bounds)) return false
        val monitor = user32.MonitorFromWindow(handle, WinUser.MONITOR_DEFAULTTONEAREST)
            ?: return false
        val monitorInfo = WinUser.MONITORINFO()
        if (user32.GetMonitorInfo(monitor, monitorInfo).intValue() == 0) return false

        val originalStyle = user32.GetWindowLong(handle, WinUser.GWL_STYLE)
        val borderless = originalStyle and (WinUser.WS_CAPTION or WinUser.WS_THICKFRAME).inv()
        if (user32.SetWindowLong(handle, WinUser.GWL_STYLE, borderless) == 0) return false

        val screen = monitorInfo.rcMonitor
        val changed = user32.SetWindowPos(
            handle, null, screen.left, screen.top,
            screen.right - screen.left, screen.bottom - screen.top, FRAME_CHANGE,
        )
        if (!changed) {
            user32.SetWindowLong(handle, WinUser.GWL_STYLE, originalStyle)
            user32.SetWindowPos(
                handle, null, bounds.left, bounds.top,
                bounds.right - bounds.left, bounds.bottom - bounds.top, FRAME_CHANGE,
            )
            return false
        }

        saved = SavedWindow(
            window, handle, originalStyle,
            bounds.left, bounds.top, bounds.right - bounds.left, bounds.bottom - bounds.top,
            (((window as? Frame)?.extendedState ?: 0) and Frame.MAXIMIZED_BOTH) != 0,
        )
        return true
    }

    private fun leave(window: Window): Boolean {
        val before = saved ?: return true
        if (before.window !== window) return false

        val user32 = User32.INSTANCE
        user32.SetWindowLong(before.handle, WinUser.GWL_STYLE, before.style)
        val restored = user32.SetWindowPos(
            before.handle, null, before.left, before.top, before.width, before.height, FRAME_CHANGE,
        )
        if (!restored) return false
        if (before.wasMaximized) user32.ShowWindow(before.handle, WinUser.SW_MAXIMIZE)
        saved = null
        return true
    }

    private val FRAME_CHANGE =
        WinUser.SWP_FRAMECHANGED or WinUser.SWP_NOZORDER or WinUser.SWP_NOOWNERZORDER
}
