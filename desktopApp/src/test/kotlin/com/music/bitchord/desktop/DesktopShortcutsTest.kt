package com.music.bitchord.desktop

import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopShortcutsTest {
    @Test
    fun canonicalRegistryHasOnlyTheRequestedBindings() {
        val expected = mapOf(
            DesktopShortcut.QUICK_SEARCH to DesktopShortcutKey(KeyEvent.VK_K, ctrl = true),
            DesktopShortcut.SHOW_SHORTCUTS to DesktopShortcutKey(KeyEvent.VK_SLASH, ctrl = true),
            DesktopShortcut.PLAY_PAUSE to DesktopShortcutKey(KeyEvent.VK_SPACE),
            DesktopShortcut.PREVIOUS to DesktopShortcutKey(KeyEvent.VK_LEFT, ctrl = true),
            DesktopShortcut.NEXT to DesktopShortcutKey(KeyEvent.VK_RIGHT, ctrl = true),
            DesktopShortcut.SEEK_BACKWARD to DesktopShortcutKey(KeyEvent.VK_LEFT, shift = true),
            DesktopShortcut.SEEK_FORWARD to DesktopShortcutKey(KeyEvent.VK_RIGHT, shift = true),
            DesktopShortcut.VOLUME_UP to DesktopShortcutKey(KeyEvent.VK_UP, alt = true),
            DesktopShortcut.VOLUME_DOWN to DesktopShortcutKey(KeyEvent.VK_DOWN, alt = true),
            DesktopShortcut.SHUFFLE to DesktopShortcutKey(KeyEvent.VK_S, alt = true),
            DesktopShortcut.REPEAT to DesktopShortcutKey(KeyEvent.VK_R, alt = true),
            DesktopShortcut.HOME to DesktopShortcutKey(KeyEvent.VK_H, alt = true, shift = true),
            DesktopShortcut.SEARCH_PAGE to DesktopShortcutKey(KeyEvent.VK_L, ctrl = true, shift = true),
            DesktopShortcut.LYRICS to DesktopShortcutKey(KeyEvent.VK_L, ctrl = true, alt = true),
        )
        assertEquals(expected.keys, DesktopShortcut.entries.toSet())
        expected.forEach { (shortcut, key) ->
            assertEquals(shortcut, DesktopShortcut.matching(key.keyCode, key.ctrl, key.alt, key.shift))
        }
    }

    @Test
    fun modifierCombinationsMustMatchExactly() {
        assertNull(DesktopShortcut.matching(KeyEvent.VK_LEFT, ctrl = true, alt = true, shift = false))
        assertNull(DesktopShortcut.matching(KeyEvent.VK_L, ctrl = false, alt = true, shift = true))
        assertNull(DesktopShortcut.matching(KeyEvent.VK_SPACE, ctrl = true, alt = false, shift = false))
        assertNull(DesktopShortcut.matching(KeyEvent.VK_SLASH, ctrl = false, alt = false, shift = false))
    }

    @Test
    fun editableFieldsAndOtherModalsSuppressGlobalActions() {
        val dispatcher = DesktopShortcutDispatcher()
        assertNull(dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SPACE), true, false, false))
        dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SPACE, pressed = false), true, false, false)
        assertEquals(
            DesktopShortcut.SHOW_SHORTCUTS,
            dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SLASH, ctrl = true), true, false, false),
        )
        dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SLASH, pressed = false), true, false, false)
        assertNull(dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SLASH, ctrl = true), false, false, true))
    }

    @Test
    fun shortcutModalAcceptsOnlyItsToggleAndRepeatedKeyDownsDoNotRetoggle() {
        val dispatcher = DesktopShortcutDispatcher()
        val slash = DesktopShortcutKey(KeyEvent.VK_SLASH, ctrl = true)
        assertEquals(DesktopShortcut.SHOW_SHORTCUTS, dispatcher.dispatch(slash, false, false, false))
        assertNull(dispatcher.dispatch(slash, false, true, false))
        dispatcher.dispatch(slash.copy(pressed = false), false, true, false)
        assertNull(dispatcher.dispatch(DesktopShortcutKey(KeyEvent.VK_SPACE), false, true, false))
        assertEquals(DesktopShortcut.SHOW_SHORTCUTS, dispatcher.dispatch(slash, false, true, false))
    }
}
