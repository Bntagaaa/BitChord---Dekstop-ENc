package com.music.bitchord.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Frame
import java.awt.Window

internal interface DesktopDwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(window: Pointer, attribute: Int, value: IntByReference, size: Int): Int
}

/** Optional DWM appearance on an ordinary, system-decorated Windows frame. */
internal object DesktopWindowsCaptionAppearance {
    private val dwm by lazy {
        if (!DesktopPlatform.isWindows) null else runCatching {
            Native.load("dwmapi", DesktopDwmApi::class.java)
        }.onFailure {
            DesktopTrackLog.log("native caption styling unavailable: ${it.message}")
        }.getOrNull()
    }

    private var attachedWindow: Window? = null
    private var handle: Pointer? = null

    fun attach(window: Window): Boolean {
        if (!DesktopPlatform.isWindows || window !is Frame || window.isUndecorated ||
            !window.isDisplayable || !window.isOpaque || dwm == null
        ) return false
        val pointer = runCatching { Native.getWindowPointer(window) }.getOrNull()
            ?.takeIf { Pointer.nativeValue(it) != 0L } ?: return false
        attachedWindow = window
        handle = pointer
        // Supported on recent Windows 10/11. Failure affects only appearance, never the frame.
        if (!setAttribute(DWMWA_USE_IMMERSIVE_DARK_MODE, 1)) {
            DesktopTrackLog.log("native caption dark mode unavailable; using Windows default")
        }
        return true
    }

    fun detach(window: Window) {
        if (attachedWindow === window) {
            attachedWindow = null
            handle = null
            DesktopWindowBackdrop.clearApplied()
        }
    }

    /** DWM backdrop values: 1 = none, 2 = Mica, 3 = desktop Acrylic. */
    fun setBackdrop(kind: Int): Boolean = setAttribute(
        DWMWA_SYSTEMBACKDROP_TYPE,
        if (kind == 0) 1 else kind,
    )

    private fun setAttribute(attribute: Int, value: Int): Boolean {
        val hwnd = handle ?: return false
        return runCatching {
            dwm?.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), Int.SIZE_BYTES) == 0
        }.getOrDefault(false)
    }

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
}
