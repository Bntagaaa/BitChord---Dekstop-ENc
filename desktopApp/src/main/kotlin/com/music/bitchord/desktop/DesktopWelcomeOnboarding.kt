package com.music.bitchord.desktop

import java.util.prefs.Preferences

/**
 * Show the welcome only on a genuinely empty desktop profile. A missing onboarding flag is
 * insufficient: users upgrading from older BitChord versions have never had that flag either.
 *
 * Query this before initializing the rest of the desktop app. Source configuration migration
 * writes defaults to Preferences during composition, even for a fresh installation.
 */
internal object DesktopWelcomeOnboarding {
    private const val KEY_COMPLETED = "first_launch_welcome_completed_v1"

    fun shouldShowOnStartup(): Boolean = runCatching {
        shouldShow(DesktopPersistence().preferences)
    }.getOrDefault(false)

    internal fun shouldShow(preferences: Preferences): Boolean {
        // Any previously saved setting, queue, account or Spotify session denotes an existing user.
        // In particular, a logged-out existing user must not see first-run onboarding again.
        return preferences.get(KEY_COMPLETED, null) == null && preferences.keys().isEmpty()
    }

    fun markCompleted() = markCompleted(DesktopPersistence().preferences)

    internal fun markCompleted(preferences: Preferences) {
        preferences.putBoolean(KEY_COMPLETED, true)
        runCatching { preferences.flush() }
    }
}
