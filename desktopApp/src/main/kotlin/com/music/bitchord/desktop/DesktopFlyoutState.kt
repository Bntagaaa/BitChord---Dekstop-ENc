package com.music.bitchord.desktop

import java.awt.event.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Matches the in-window paint order in DesktopApp, not the order a user opens panels. */
internal enum class DesktopFlyoutLayer(val order: Int) {
    ACCOUNT(10), DIALOG(20), SHORTCUTS(30), QUICK_SEARCH(40),
}

internal data class DesktopFlyoutSnapshot(
    val revision: Long = 0,
    val count: Int = 0,
    val closedRevision: Long = 0,
)

/**
 * Per-window Escape ownership. Ordinary keys and Quick Search's existing key router are untouched.
 * All calls come from the Compose UI thread; entries live only as long as their flyout composition.
 */
internal class DesktopFlyoutState {
    private data class Entry(
        val token: Any,
        val layer: DesktopFlyoutLayer,
        val sequence: Long,
        val delegateEscape: Boolean,
        val onDismiss: (() -> Unit)?,
    )

    private data class EscapePress(val owner: Entry, var cancelled: Boolean = false)

    private val entries = mutableListOf<Entry>()
    private var sequence = 0L
    private var press: EscapePress? = null
    private val mutable = MutableStateFlow(DesktopFlyoutSnapshot())
    val snapshot: StateFlow<DesktopFlyoutSnapshot> = mutable.asStateFlow()

    fun register(
        token: Any,
        layer: DesktopFlyoutLayer,
        delegateEscape: Boolean = false,
        onDismiss: (() -> Unit)? = null,
    ) {
        check(entries.none { it.token === token }) { "Flyout is already registered" }
        entries += Entry(token, layer, ++sequence, delegateEscape, onDismiss)
        publish(closed = false)
    }

    fun unregister(token: Any) {
        if (entries.removeAll { it.token === token }) publish(closed = true)
        // Retain a claimed press until KeyUp even if the mouse already closed its owner.
    }

    fun cancelPendingEscape() {
        press?.cancelled = true
    }

    /** True means consumed. Never dismiss on KeyDown or on an unmatched release. */
    fun handle(keyCode: Int, pressed: Boolean): Boolean {
        if (keyCode != KeyEvent.VK_ESCAPE) return false
        if (pressed) {
            press?.let { return !it.owner.delegateEscape || it.cancelled }
            val owner = top() ?: return false
            press = EscapePress(owner)
            return !owner.delegateEscape
        }

        val captured = press
        press = null
        if (captured != null) {
            if (captured.cancelled) return true
            // Its router must drain the release even if Quick Search was dismissed by mouse.
            if (captured.owner.delegateEscape) return false
            top()?.takeIf { it === captured.owner }?.onDismiss?.invoke()
            return true
        }
        // A flyout opened mid-press must not inherit that press or expose the player behind it.
        return top()?.delegateEscape == false
    }

    private fun top(): Entry? = entries.maxWithOrNull(compareBy<Entry> { it.layer.order }.thenBy { it.sequence })

    private fun publish(closed: Boolean) {
        val previous = mutable.value
        val revision = previous.revision + 1
        mutable.value = DesktopFlyoutSnapshot(
            revision = revision,
            count = entries.size,
            closedRevision = if (closed) revision else previous.closedRevision,
        )
    }
}
