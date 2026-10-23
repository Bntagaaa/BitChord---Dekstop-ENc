package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSidebarWidthPolicyTest {
    @Test
    fun `narrow windows use a rail instead of hiding desktop navigation`() {
        assertTrue(DesktopSidebarWidthPolicy.shouldUseRail(640f))
        assertTrue(DesktopSidebarWidthPolicy.shouldUseRail(979f))
        assertFalse(DesktopSidebarWidthPolicy.shouldUseRail(980f))
        assertFalse(DesktopSidebarWidthPolicy.shouldUseRail(1440f))
    }

    @Test
    fun `automatic collapse does not depend on the saved preference`() {
        assertTrue(DesktopSidebarWidthPolicy.isInlineCollapsed(800f, false))
        assertTrue(DesktopSidebarWidthPolicy.isInlineCollapsed(800f, true))
        assertTrue(DesktopSidebarWidthPolicy.isInlineCollapsed(1200f, true))
        assertFalse(DesktopSidebarWidthPolicy.isInlineCollapsed(1200f, false))
    }

    @Test
    fun `expanded drawer only overlays narrow windows when requested`() {
        assertFalse(DesktopSidebarWidthPolicy.isDrawerVisible(800f, false))
        assertTrue(DesktopSidebarWidthPolicy.isDrawerVisible(800f, true))
        assertFalse(DesktopSidebarWidthPolicy.isDrawerVisible(1200f, true))
    }
}
