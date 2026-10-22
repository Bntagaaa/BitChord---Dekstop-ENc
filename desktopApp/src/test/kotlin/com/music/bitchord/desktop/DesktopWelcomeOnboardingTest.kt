package com.music.bitchord.desktop

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DesktopWelcomeOnboardingTest {
    private fun withPreferences(block: (Preferences) -> Unit) {
        val prefs = Preferences.userRoot().node("bitchord-welcome-test-" + UUID.randomUUID())
        try {
            block(prefs)
        } finally {
            prefs.removeNode()
        }
    }

    @Test fun `fresh profile sees welcome`() = withPreferences { prefs ->
        assertTrue(DesktopWelcomeOnboarding.shouldShow(prefs))
    }

    @Test fun `completion persists even if user skips sign in`() = withPreferences { prefs ->
        DesktopWelcomeOnboarding.markCompleted(prefs)
        assertFalse(DesktopWelcomeOnboarding.shouldShow(prefs))
    }

    @Test fun `existing preferences are not mistaken for a new installation`() = withPreferences { prefs ->
        prefs.putBoolean("autoplay", true)
        assertFalse(DesktopWelcomeOnboarding.shouldShow(prefs))
    }

    @Test fun `existing logged out users are not shown onboarding`() = withPreferences { prefs ->
        prefs.put("volume", "0.8")
        assertFalse(DesktopWelcomeOnboarding.shouldShow(prefs))
    }

    @Test fun `legacy spotify session is treated as an existing installation`() = withPreferences { prefs ->
        prefs.put(DesktopSpotifyToken.KEY_SPDC, "legacy-session")
        assertFalse(DesktopWelcomeOnboarding.shouldShow(prefs))
    }
}
