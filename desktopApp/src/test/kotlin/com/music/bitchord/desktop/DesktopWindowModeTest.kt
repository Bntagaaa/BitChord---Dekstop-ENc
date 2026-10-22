package com.music.bitchord.desktop

import androidx.compose.ui.window.WindowPlacement
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWindowModeTest {
    @BeforeTest
    fun setUp() {
        DesktopWindowMode.exit()
        DesktopWindowMode.adopt(WindowPlacement.Floating)
    }

    @AfterTest
    fun tearDown() {
        DesktopWindowMode.exit()
        DesktopWindowMode.adopt(WindowPlacement.Floating)
    }

    @Test
    fun restoresMaximizedWindowAfterFullscreenExit() {
        DesktopWindowMode.adopt(WindowPlacement.Maximized)
        DesktopWindowMode.toggle()

        assertTrue(DesktopWindowMode.fullScreen.value)
        assertEquals(
            if (DesktopPlatform.isWindows) WindowPlacement.Maximized else WindowPlacement.Fullscreen,
            DesktopWindowMode.placement.value,
        )

        DesktopWindowMode.exit()
        assertFalse(DesktopWindowMode.fullScreen.value)
        assertEquals(WindowPlacement.Maximized, DesktopWindowMode.placement.value)
    }

    @Test
    fun togglingPlayerFullscreenIsReversibleWithoutChangingFloatingLayout() {
        DesktopWindowMode.toggle()
        assertTrue(DesktopWindowMode.fullScreen.value)

        DesktopWindowMode.toggle()
        assertFalse(DesktopWindowMode.fullScreen.value)
        assertEquals(WindowPlacement.Floating, DesktopWindowMode.placement.value)
    }
}
