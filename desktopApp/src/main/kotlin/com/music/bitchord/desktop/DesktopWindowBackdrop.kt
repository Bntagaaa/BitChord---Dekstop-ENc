package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The system material behind the window. */
internal enum class DesktopBackdrop(
    /** What [DesktopWindowsFrame.setBackdrop] takes: DWM's DWMSBT_* value, or 0 for none. */
    val nativeKind: Int,
    val label: String,
) {
    OFF(0, "Off"),
    MICA(2, "Mica"),
    ACRYLIC(3, "Acrylic"),
}

/**
 * Windows 11's Mica and Acrylic, drawn by DWM behind the extended native frame.
 * Transparency is chosen by Main.kt before the peer is created; changing material is still live.
 * Compose decides whether the material also reaches the app background via [appBackground].
 */
internal object DesktopWindowBackdrop {
    internal const val KEY = "window_backdrop"

    /** The app starts after caption initialization, so fallback windows never offer dead controls. */
    val available: Boolean
        get() = DesktopPlatform.isWindows && System.getProperty("os.name").orEmpty().contains("11") &&
            DesktopWindowsFrame.isInstalled

    private val _selected = MutableStateFlow(
        runCatching { DesktopBackdrop.valueOf(DesktopPersistence().string(KEY, DesktopBackdrop.MICA.name)) }
            .getOrDefault(DesktopBackdrop.MICA),
    )
    val selected: StateFlow<DesktopBackdrop> = _selected
    private val _appBackground = MutableStateFlow(
        DesktopPersistence().boolean(KEY_APP_BACKGROUND, true),
    )
    val appBackground: StateFlow<Boolean> = _appBackground
    private val _active = MutableStateFlow(DesktopBackdrop.OFF)
    val active: StateFlow<DesktopBackdrop> = _active

    fun set(value: DesktopBackdrop) {
        DesktopPersistence().saveString(KEY, value.name)
        _selected.value = value
        apply()
    }

    fun setAppBackground(value: Boolean) {
        DesktopPersistence().saveBoolean(KEY_APP_BACKGROUND, value)
        _appBackground.value = value
    }

    fun apply() {
        if (!available) {
            _active.value = DesktopBackdrop.OFF
            return
        }
        val wanted = _selected.value
        val applied = DesktopWindowsFrame.setBackdrop(wanted.nativeKind)
        _active.value = if (applied) wanted else DesktopBackdrop.OFF
        if (!applied && wanted != DesktopBackdrop.OFF) {
            DesktopTrackLog.log("window backdrop: Windows declined ${wanted.label}")
        }
    }

    internal const val KEY_APP_BACKGROUND = "window_backdrop_app_background"
}
