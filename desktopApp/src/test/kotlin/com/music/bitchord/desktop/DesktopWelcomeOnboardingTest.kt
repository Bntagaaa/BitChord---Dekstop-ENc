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

    @Test fun `first launch is detected before the preferences node exists`() {
        val root = Preferences.userRoot().node("bitchord-onboarding-root-" + UUID.randomUUID())
        try {
            assertTrue(DesktopWelcomeOnboarding.shouldShowBeforeInitialization(root, "fresh"))
            assertFalse(root.nodeExists("fresh"))
        } finally {
            root.removeNode()
        }
    }

    @Test fun `old app preferences are not treated as first launch`() {
        val root = Preferences.userRoot().node("bitchord-onboarding-root-" + UUID.randomUUID())
        try {
            root.node("existing").putBoolean("autoplay", true)
            assertFalse(DesktopWelcomeOnboarding.shouldShowBeforeInitialization(root, "existing"))
        } finally {
            root.removeNode()
        }
    }

    @Test fun `presence install id does not impersonate a real first launch`() = withPreferences { prefs ->
        prefs.put("presence_install_id", UUID.randomUUID().toString())
        assertFalse(DesktopWelcomeOnboarding.shouldShow(prefs))
    }

    @Test fun `explicit test override accepts enabled forms only`() {
        assertTrue(DesktopWelcomeOnboarding.forceRequested("1"))
        assertTrue(DesktopWelcomeOnboarding.forceRequested("TRUE"))
        assertFalse(DesktopWelcomeOnboarding.forceRequested("0"))
        assertFalse(DesktopWelcomeOnboarding.forceRequested(null))
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
