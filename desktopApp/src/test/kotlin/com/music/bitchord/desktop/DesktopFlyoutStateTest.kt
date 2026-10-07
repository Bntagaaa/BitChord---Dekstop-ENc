package com.music.bitchord.desktop

import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DesktopFlyoutStateTest {
    private val esc = KeyEvent.VK_ESCAPE

    @Test fun noFlyoutLeavesPlayerEscapeAlone() {
        val state = DesktopFlyoutState()
        assertFalse(state.handle(esc, true))
        assertFalse(state.handle(esc, false))
    }

    @Test fun audioOutputDismissesOnlyOnRelease() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++ })
        assertTrue(state.handle(esc, true))
        assertEquals(0, dismissed)
        assertTrue(state.handle(esc, false))
        assertEquals(1, dismissed)
        assertTrue(state.handle(esc, false))
        assertEquals(1, dismissed)
    }

    @Test fun accountPickerDoesNotSelectOrRemoveAnAccountOnEscape() {
        val state = DesktopFlyoutState()
        var open = true
        val currentAccount = "account-1"
        val savedAccounts = listOf("account-1", "account-2")
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { open = false })
        state.handle(esc, true)
        state.handle(esc, false)
        assertFalse(open)
        assertEquals("account-1", currentAccount)
        assertEquals(listOf("account-1", "account-2"), savedAccounts)
    }

    @Test fun keyRepeatCannotDismissMultipleLayers() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++ })
        repeat(30) { assertTrue(state.handle(esc, true)) }
        assertEquals(0, dismissed)
        assertTrue(state.handle(esc, false))
        assertEquals(1, dismissed)
    }

    @Test fun onlyTopPaintedLayerIsClosed() {
        val state = DesktopFlyoutState()
        val account = Any()
        val audio = Any()
        val calls = mutableListOf<String>()
        state.register(account, DesktopFlyoutLayer.ACCOUNT, onDismiss = { calls += "account"; state.unregister(account) })
        state.register(audio, DesktopFlyoutLayer.DIALOG, onDismiss = { calls += "audio"; state.unregister(audio) })
        state.handle(esc, true)
        state.handle(esc, false)
        assertEquals(listOf("audio"), calls)
        assertEquals(1, state.snapshot.value.count)
        state.handle(esc, true)
        state.handle(esc, false)
        assertEquals(listOf("audio", "account"), calls)
    }

    @Test fun paintOrderWinsOverRegistrationOrder() {
        val state = DesktopFlyoutState()
        val calls = mutableListOf<String>()
        state.register(Any(), DesktopFlyoutLayer.SHORTCUTS, onDismiss = { calls += "shortcuts" })
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { calls += "account" })
        state.handle(esc, true)
        state.handle(esc, false)
        assertEquals(listOf("shortcuts"), calls)
    }

    @Test fun sameLayerUsesMostRecentEntry() {
        val state = DesktopFlyoutState()
        val calls = mutableListOf<String>()
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { calls += "parent" })
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { calls += "child" })
        state.handle(esc, true)
        state.handle(esc, false)
        assertEquals(listOf("child"), calls)
    }

    @Test fun mouseDismissalStillConsumesOwnedRelease() {
        val state = DesktopFlyoutState()
        val owner = Any()
        var dismissed = 0
        state.register(owner, DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++ })
        state.handle(esc, true)
        state.unregister(owner)
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
        assertFalse(state.handle(esc, true))
        assertFalse(state.handle(esc, false))
    }

    @Test fun releaseCannotDismissReplacementFlyout() {
        val state = DesktopFlyoutState()
        val owner = Any()
        var dismissed = 0
        state.register(owner, DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++ })
        state.handle(esc, true)
        state.unregister(owner)
        // Even a caller that reuses its token must create a new press for the new registration.
        state.register(owner, DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed += 10 })
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
    }

    @Test fun newTopFlyoutCancelsOldDismissIntent() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { dismissed++ })
        state.handle(esc, true)
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed += 10 })
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
    }

    @Test fun unmatchedReleaseDoesNotCloseNewFlyout() {
        val state = DesktopFlyoutState()
        assertFalse(state.handle(esc, true))
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++ })
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
    }

    @Test fun focusLossCancelsDismissButConsumesRelease() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { dismissed++ })
        state.handle(esc, true)
        state.cancelPendingEscape()
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
        state.handle(esc, true)
        state.handle(esc, false)
        assertEquals(1, dismissed)
    }

    @Test fun nonDismissibleDialogShieldsPlayerAndAccount() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { dismissed++ })
        state.register(Any(), DesktopFlyoutLayer.DIALOG)
        assertTrue(state.handle(esc, true))
        assertTrue(state.handle(esc, false))
        assertEquals(0, dismissed)
    }

    @Test fun nonEscapeKeysAreNeverIntercepted() {
        val state = DesktopFlyoutState()
        state.register(Any(), DesktopFlyoutLayer.SHORTCUTS, onDismiss = { error("Unexpected dismiss") })
        listOf(KeyEvent.VK_K, KeyEvent.VK_SLASH, KeyEvent.VK_SPACE, KeyEvent.VK_ENTER,
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_TAB, KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_ALT).forEach {
            assertFalse(state.handle(it, true))
            assertFalse(state.handle(it, false))
        }
    }

    @Test fun quickSearchEscapeRemainsOwnedByExistingRouter() {
        val state = DesktopFlyoutState()
        val quick = DesktopQuickSearchKeys()
        state.register(Any(), DesktopFlyoutLayer.QUICK_SEARCH, delegateEscape = true)
        assertFalse(state.handle(esc, true))
        assertEquals(DesktopQuickSearchKeyAction.CONSUME, quick.handle(esc, true, true))
        assertFalse(state.handle(esc, false))
        assertEquals(DesktopQuickSearchKeyAction.DISMISS, quick.handle(esc, false, true))
    }

    @Test fun quickSearchMouseDismissalDrainsItsOriginalRelease() {
        val state = DesktopFlyoutState()
        val quick = DesktopQuickSearchKeys()
        val owner = Any()
        var underlyingDismissed = 0
        state.register(owner, DesktopFlyoutLayer.QUICK_SEARCH, delegateEscape = true)
        assertFalse(state.handle(esc, true))
        quick.handle(esc, true, true)
        state.unregister(owner)
        state.register(Any(), DesktopFlyoutLayer.ACCOUNT, onDismiss = { underlyingDismissed++ })
        assertFalse(state.handle(esc, false))
        assertEquals(DesktopQuickSearchKeyAction.CONSUME, quick.handle(esc, false, false))
        assertEquals(0, underlyingDismissed)
    }

    @Test fun quickSearchEditingAndPlaybackKeysKeepTheirBehavior() {
        val state = DesktopFlyoutState()
        val quick = DesktopQuickSearchKeys()
        state.register(Any(), DesktopFlyoutLayer.QUICK_SEARCH, delegateEscape = true)
        assertFalse(state.handle(KeyEvent.VK_SPACE, true))
        assertEquals(DesktopQuickSearchKeyAction.PASS, quick.handle(KeyEvent.VK_SPACE, true, true))
        assertFalse(state.handle(KeyEvent.VK_DOWN, true))
        assertEquals(DesktopQuickSearchKeyAction.NEXT, quick.handle(KeyEvent.VK_DOWN, true, true))
        assertFalse(state.handle(KeyEvent.VK_DOWN, false))
        quick.handle(KeyEvent.VK_DOWN, false, true)
        assertFalse(state.handle(KeyEvent.VK_ENTER, true))
        quick.handle(KeyEvent.VK_ENTER, true, true)
        assertFalse(state.handle(KeyEvent.VK_ENTER, false))
        assertEquals(DesktopQuickSearchKeyAction.CONSUME, quick.handle(KeyEvent.VK_ENTER, false, true))
        assertFalse(state.handle(KeyEvent.VK_ENTER, true))
        quick.handle(KeyEvent.VK_ENTER, true, true, shift = true)
        assertFalse(state.handle(KeyEvent.VK_ENTER, false))
        assertEquals(DesktopQuickSearchKeyAction.PLAY, quick.handle(KeyEvent.VK_ENTER, false, true))
    }

    @Test fun independentWindowsDoNotShareEscapeOwnership() {
        val first = DesktopFlyoutState()
        val second = DesktopFlyoutState()
        var calls = 0
        first.register(Any(), DesktopFlyoutLayer.DIALOG, onDismiss = { calls++ })
        assertTrue(first.handle(esc, true))
        assertFalse(second.handle(esc, true))
        assertFalse(second.handle(esc, false))
        assertEquals(0, calls)
        first.handle(esc, false)
        assertEquals(1, calls)
    }

    @Test fun closeRevisionSupportsDeferredFocusRepair() {
        val state = DesktopFlyoutState()
        val token = Any()
        state.register(token, DesktopFlyoutLayer.ACCOUNT)
        assertEquals(0L, state.snapshot.value.closedRevision)
        state.unregister(token)
        val closed = state.snapshot.value
        assertEquals(0, closed.count)
        assertTrue(closed.closedRevision > 0)
        state.unregister(token)
        assertEquals(closed, state.snapshot.value)
        state.register(Any(), DesktopFlyoutLayer.DIALOG)
        assertEquals(closed.closedRevision, state.snapshot.value.closedRevision)
        assertEquals(1, state.snapshot.value.count)
    }

    @Test fun duplicateRegistrationIsRejectedWithoutCorruptingState() {
        val state = DesktopFlyoutState()
        val token = Any()
        state.register(token, DesktopFlyoutLayer.ACCOUNT)
        assertFailsWith<IllegalStateException> { state.register(token, DesktopFlyoutLayer.ACCOUNT) }
        assertEquals(1, state.snapshot.value.count)
    }

    @Test fun repeatedOpenDismissCyclesDoNotLeaveStuckEscape() {
        val state = DesktopFlyoutState()
        var dismissed = 0
        repeat(250) {
            val token = Any()
            state.register(token, DesktopFlyoutLayer.DIALOG, onDismiss = { dismissed++; state.unregister(token) })
            assertTrue(state.handle(esc, true))
            assertTrue(state.handle(esc, false))
            assertEquals(0, state.snapshot.value.count)
            assertFalse(state.handle(esc, false))
        }
        assertEquals(250, dismissed)
    }
}
