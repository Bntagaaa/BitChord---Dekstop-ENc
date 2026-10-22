package com.music.bitchord.desktop

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopUiScaleSettingsTest {
    @Test
    fun `normalizes values to safe zoom presets`() {
        assertEquals(listOf(80, 90, 100, 110, 125), DesktopUiScaleSettings.options)
        assertEquals(100, DesktopUiScaleSettings.normalizePercent(100))
        assertEquals(100, DesktopUiScaleSettings.normalizePercent(103))
        assertEquals(110, DesktopUiScaleSettings.normalizePercent(107))
        assertEquals(80, DesktopUiScaleSettings.normalizePercent(-10))
        assertEquals(125, DesktopUiScaleSettings.normalizePercent(1_000))
    }

    @Test
    fun `reads persisted scale and falls back for corrupted values`() {
        val preferences = Preferences.userRoot().node("com.music.bitchord.desktop.test.ui_scale_${UUID.randomUUID()}")
        try {
            assertEquals(100, DesktopUiScaleSettings.readPercent(preferences))
            preferences.putInt(DesktopUiScaleSettings.KEY_UI_SCALE_PERCENT, 125)
            assertEquals(125, DesktopUiScaleSettings.readPercent(preferences))
            preferences.put(DesktopUiScaleSettings.KEY_UI_SCALE_PERCENT, "broken")
            assertEquals(100, DesktopUiScaleSettings.readPercent(preferences))
        } finally {
            preferences.removeNode()
        }
    }
}
