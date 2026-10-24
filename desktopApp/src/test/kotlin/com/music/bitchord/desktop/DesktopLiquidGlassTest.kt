package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Material policy tests do not require an OpenGL/Skia display or a running compositor. */
class DesktopLiquidGlassTest {
    @Test fun `reduce blur disables all backdrop capture`() {
        assertFalse(desktopGlassSamplesBackdrop(true, true))
        assertFalse(desktopGlassSamplesBackdrop(false, true))
        assertFalse(desktopGlassSamplesBackdrop(false, false))
        assertTrue(desktopGlassSamplesBackdrop(true, false))
    }

    @Test fun `all macOS material variants have useful blur and bounded tint`() {
        for (dark in listOf(true, false)) {
            for (material in DesktopGlassMaterial.entries) {
                val tune = desktopGlassTuning(material, dark)
                assertTrue(tune.blurRadius.value >= 25f)
                assertTrue(tune.tintAlpha in 0f..1f)
                assertTrue(tune.noiseFactor in 0f..0.05f)
                assertTrue(tune.rimAlpha in 0f..1f)
            }
        }
    }

    @Test fun `floating and popup surfaces receive stronger separation than chrome`() {
        assertTrue(
            desktopGlassTuning(DesktopGlassMaterial.POPOVER, true).blurRadius >
                desktopGlassTuning(DesktopGlassMaterial.CHROME, true).blurRadius
        )
        assertTrue(
            desktopGlassTuning(DesktopGlassMaterial.FLOATING, true).rimAlpha >
                desktopGlassTuning(DesktopGlassMaterial.CHROME, true).rimAlpha
        )
    }
}
