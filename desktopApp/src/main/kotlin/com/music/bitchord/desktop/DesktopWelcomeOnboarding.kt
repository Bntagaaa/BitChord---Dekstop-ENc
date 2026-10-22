package com.music.bitchord.desktop

import java.util.prefs.Preferences

/**
 * An upgrade is not a first launch. Inspect Preferences before DesktopPresence.install(), which
 * creates 'presence_install_id' during startup even for an otherwise empty, brand-new profile.
 *
 * BITCHORD_SHOW_WELCOME=1 is a non-destructive testing override: it shows the real welcome UI
 * without clearing accounts, downloads, or preferences.
 */
internal object DesktopWelcomeOnboarding {
    private const val KEY_COMPLETED = "first_launch_welcome_completed_v1"
    private const val PREFERENCES_NODE = "com.music.bitchord.desktop"
    private const val FORCE_WELCOME_ENV = "BITCHORD_SHOW_WELCOME"

    /** Call exactly once, at the top of main(), before any other service starts. */
    fun shouldShowOnStartup(): Boolean {
        if (forceRequested(System.getenv(FORCE_WELCOME_ENV))) return true
        return runCatching {
            shouldShowBeforeInitialization(Preferences.userRoot(), PREFERENCES_NODE)
        }.getOrDefault(false)
    }

    /**
     * nodeExists is crucial: reading DesktopPersistence().preferences unconditionally creates
     * the profile node, potentially changing the answer before first-run detection is finished.
     */
    internal fun shouldShowBeforeInitialization(root: Preferences, nodeName: String): Boolean =
        !root.nodeExists(nodeName) || shouldShow(root.node(nodeName))

    internal fun forceRequested(value: String?): Boolean = value?.trim()?.let {
        it == "1" || it.equals("true", ignoreCase = true) || it.equals("yes", ignoreCase = true)
    } == true

    internal fun shouldShow(preferences: Preferences): Boolean {
        // Preserve existing users, including those who signed out. A completed welcome counts
        // as existing data even when the listener skipped both account connections.
        return preferences.get(KEY_COMPLETED, null) == null && preferences.keys().isEmpty()
    }

    fun markCompleted() = markCompleted(DesktopPersistence().preferences)

    internal fun markCompleted(preferences: Preferences) {
        preferences.putBoolean(KEY_COMPLETED, true)
        runCatching { preferences.flush() }
    }
}
