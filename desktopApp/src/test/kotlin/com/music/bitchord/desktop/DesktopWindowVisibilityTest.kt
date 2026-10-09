package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWindowVisibilityTest {
    @Test
    fun `raise restores a tray-hidden window and requests focus each time`() {
        val previousTraySetting = DesktopWindowVisibility.keepRunningWhenClosed
        try {
            DesktopWindowVisibility.keepRunningWhenClosed = true
            assertFalse(DesktopWindowVisibility.onCloseRequest())
            assertFalse(DesktopWindowVisibility.visible.value)

            val before = DesktopWindowVisibility.raiseRequest.value
            DesktopWindowVisibility.raise()
            assertTrue(DesktopWindowVisibility.visible.value)
            assertEquals(before + 1L, DesktopWindowVisibility.raiseRequest.value)

            DesktopWindowVisibility.raise()
            assertEquals(before + 2L, DesktopWindowVisibility.raiseRequest.value)
        } finally {
            DesktopWindowVisibility.show()
            DesktopWindowVisibility.keepRunningWhenClosed = previousTraySetting
        }
    }
}
