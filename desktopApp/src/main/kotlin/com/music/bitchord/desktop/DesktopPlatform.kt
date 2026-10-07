package com.music.bitchord.desktop

/** Which desktop this is running on. */
internal object DesktopPlatform {
    private val name: String = System.getProperty("os.name").orEmpty()
    val isWindows: Boolean = name.startsWith("Windows", ignoreCase = true)
    val isLinux: Boolean = name.contains("linux", ignoreCase = true)

    /**
     * Legacy Compose-drawn caption controls are retired. Both desktop platforms now use native
     * controls. This also disables the old inline-caption slots and Title bar preference row in
     * DesktopApp without changing its playback, flyout or shortcut code.
     * Native DWM frame availability is a separate capability: DesktopWindowsFrame.available.
     */
    val drawsOwnWindowFrame: Boolean = false
}
