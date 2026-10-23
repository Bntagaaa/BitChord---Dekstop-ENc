package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopTopBarWidthPolicyTest {
    @Test
    fun `full toolbar requires enough space beside sidebar`() {
        assertFalse(DesktopTopBarWidthPolicy.showFullControls(719f))
        assertTrue(DesktopTopBarWidthPolicy.showFullControls(720f))
    }

    @Test
    fun `compact toolbar removes transport extras before track information`() {
        assertFalse(DesktopTopBarWidthPolicy.showCompactSkipControls(429f))
        assertTrue(DesktopTopBarWidthPolicy.showCompactSkipControls(430f))
        assertFalse(DesktopTopBarWidthPolicy.showCompactSidePanels(349f))
        assertTrue(DesktopTopBarWidthPolicy.showCompactSidePanels(350f))
    }

    @Test
    fun `compact toolbar does not depend on sidebar rail breakpoint`() {
        assertTrue(DesktopTopBarWidthPolicy.showFullControls(800f))
        assertFalse(DesktopTopBarWidthPolicy.showFullControls(600f))
        assertFalse(DesktopTopBarWidthPolicy.showCompactSkipControls(300f))
    }
}
