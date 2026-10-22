package com.music.bitchord.desktop

import java.util.prefs.Preferences
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** User-controlled Compose UI zoom; 100% preserves the previous desktop layout. */
internal object DesktopUiScaleSettings {
    internal const val KEY_UI_SCALE_PERCENT = "desktop_ui_scale_percent"
    const val DEFAULT_PERCENT = 100

    /** Discrete choices keep the layout stable during input and provide a clear reset at 100%. */
    val options = listOf(80, 90, 100, 110, 125)

    private val persistence = DesktopPersistence()
    private val _percent = MutableStateFlow(readPercent(persistence.preferences))
    val percent: StateFlow<Int> = _percent

    fun setPercent(value: Int) {
        val normalized = normalizePercent(value)
        if (_percent.value == normalized) return
        persistence.saveInt(KEY_UI_SCALE_PERCENT, normalized)
        _percent.value = normalized
    }

    internal fun normalizePercent(value: Int): Int {
        val bounded = value.coerceIn(options.first(), options.last())
        return options.minBy { abs(it - bounded) }
    }

    internal fun readPercent(preferences: Preferences): Int =
        normalizePercent(preferences.getInt(KEY_UI_SCALE_PERCENT, DEFAULT_PERCENT))
}
