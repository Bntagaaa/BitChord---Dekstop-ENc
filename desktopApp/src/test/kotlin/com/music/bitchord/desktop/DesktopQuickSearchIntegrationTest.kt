package com.music.bitchord.desktop

import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopQuickSearchIntegrationTest {
    @Test fun quickSearchIsAvailableFromAnExistingSearchEditor() {
        val dispatcher = DesktopShortcutDispatcher()
        assertEquals(
            DesktopShortcut.QUICK_SEARCH,
            dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_K, ctrl = true), true, false, false),
        )
        dispatcher.release(KeyEvent.VK_K)
        assertNull(dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SPACE), true, false, false))
        assertNull(dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_LEFT, ctrl = true), true, false, false))
    }

    @Test fun quickSearchNeverOpensOverAnUnrelatedModal() {
        val dispatcher = DesktopShortcutDispatcher()
        val key = DesktopShortcutKey(KeyEvent.VK_K, ctrl = true)
        assertNull(dispatcher.dispatch(key, false, false, true))
        assertNull(dispatcher.dispatch(key, false, true, false))
    }

    @Test fun oldSearchBindingAndCanonicalShortcutCountAreUnchanged() {
        assertEquals(14, DesktopShortcut.entries.size)
        assertEquals(DesktopShortcut.SEARCH_PAGE,
            DesktopShortcut.matching(KeyEvent.VK_L, ctrl = true, alt = false, shift = true))
        assertEquals(DesktopShortcut.QUICK_SEARCH,
            DesktopShortcut.matching(KeyEvent.VK_K, ctrl = true, alt = false, shift = false))
    }
}
