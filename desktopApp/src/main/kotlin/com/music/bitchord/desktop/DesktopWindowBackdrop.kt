package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Requested native caption material. The opaque Compose client remains unchanged. */
internal enum class DesktopBackdrop(val nativeKind: Int, val label: String) {
    OFF(0, "Off"),
    MICA(2, "Mica"),
    ACRYLIC(3, "Acrylic"),
}

internal object DesktopWindowBackdrop {
    internal const val KEY = "window_backdrop"

    private val _selected = MutableStateFlow(
        runCatching { DesktopBackdrop.valueOf(DesktopPersistence().string(KEY, DesktopBackdrop.MICA.name)) }
            .getOrDefault(DesktopBackdrop.MICA),
    )
    val selected: StateFlow<DesktopBackdrop> = _selected
    private val _captionActive = MutableStateFlow(DesktopBackdrop.OFF)
    val captionActive: StateFlow<DesktopBackdrop> = _captionActive

    fun set(value: DesktopBackdrop) {
        DesktopPersistence().saveString(KEY, value.name)
        _selected.value = value
        apply()
    }

    fun apply() {
        val wanted = _selected.value
        val applied = DesktopWindowsCaptionAppearance.setBackdrop(wanted.nativeKind)
        _captionActive.value = if (applied) wanted else DesktopBackdrop.OFF
        if (!applied && wanted != DesktopBackdrop.OFF && DesktopPlatform.isWindows) {
            DesktopTrackLog.log("native title bar material unavailable: ${wanted.label}")
        }
    }

    fun clearApplied() {
        _captionActive.value = DesktopBackdrop.OFF
    }
}
