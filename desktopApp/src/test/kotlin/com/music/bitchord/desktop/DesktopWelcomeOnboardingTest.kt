package com.music.bitchord.desktop

import java.nio.file.Files
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    @Test fun `welcome appears even if a YouTube and Spotify profile already exists`() =
        withPreferences { prefs ->
            prefs.put("google_accounts_v1", "existing-account")
            prefs.put("spotify_spdc_token", "existing-session")
            val launch = DesktopWelcomeOnboarding.inspect(prefs, "portable/location-A/v1")
            assertTrue(launch.showWelcome)
        }

    @Test fun `completed welcome stays hidden on later launches of the same app copy`() =
        withPreferences { prefs ->
            val first = DesktopWelcomeOnboarding.inspect(prefs, "installed/location-A/v1")
            assertTrue(first.showWelcome)
            DesktopWelcomeOnboarding.complete(prefs, first)
            val second = DesktopWelcomeOnboarding.inspect(prefs, "installed/location-A/v1")
            assertFalse(second.showWelcome)
            assertEquals(first.markerKey, second.markerKey)
        }

    @Test fun `different portable locations get independent first-run welcome`() =
        withPreferences { prefs ->
            val portableA = DesktopWelcomeOnboarding.inspect(prefs, "C:/Apps/PortableA/BitChord.exe")
            DesktopWelcomeOnboarding.complete(prefs, portableA)
            val portableB = DesktopWelcomeOnboarding.inspect(prefs, "C:/Apps/PortableB/BitChord.exe")
            assertTrue(portableB.showWelcome)
            DesktopWelcomeOnboarding.complete(prefs, portableB)
            assertFalse(DesktopWelcomeOnboarding.inspect(prefs, "C:/Apps/PortableA/BitChord.exe").showWelcome)
            assertFalse(DesktopWelcomeOnboarding.inspect(prefs, "C:/Apps/PortableB/BitChord.exe").showWelcome)
        }

    @Test fun `reinstalled or updated launcher at the same path gets a new welcome`() {
        val executable = Files.createTempFile("bitchord-test-launcher-", ".exe")
        try {
            Files.writeString(executable, "older copy")
            val before = DesktopWelcomeOnboarding.fileIdentity(executable)
            Files.writeString(executable, "new installed executable with different size")
            val after = DesktopWelcomeOnboarding.fileIdentity(executable)
            assertNotEquals(before, after)
            withPreferences { prefs ->
                DesktopWelcomeOnboarding.complete(
                    prefs, DesktopWelcomeOnboarding.inspect(prefs, before)
                )
                assertTrue(DesktopWelcomeOnboarding.inspect(prefs, after).showWelcome)
            }
        } finally {
            Files.deleteIfExists(executable)
        }
    }

    @Test fun `existing account state survives completing onboarding`() = withPreferences { prefs ->
        prefs.put("spotify_spdc_token", "still-signed-in")
        prefs.put("volume", "0.6")
        val launch = DesktopWelcomeOnboarding.inspect(prefs, "new/installer/exe")
        DesktopWelcomeOnboarding.complete(prefs, launch)
        assertEquals("still-signed-in", prefs.get("spotify_spdc_token", ""))
        assertEquals("0.6", prefs.get("volume", ""))
    }

    @Test fun `old profile-wide completed flag does not skip a new installation`() =
        withPreferences { prefs ->
            prefs.putBoolean("first_launch_welcome_completed_v1", true)
            assertTrue(DesktopWelcomeOnboarding.inspect(prefs, "brand-new-installed-copy").showWelcome)
        }

    @Test fun `opt-in developer preview can show onboarding without resetting accounts`() =
        withPreferences { prefs ->
            val first = DesktopWelcomeOnboarding.inspect(prefs, "existing-copy")
            DesktopWelcomeOnboarding.complete(prefs, first)
            assertFalse(DesktopWelcomeOnboarding.inspect(prefs, "existing-copy").showWelcome)
            assertTrue(DesktopWelcomeOnboarding.inspect(prefs, "existing-copy", forced = true).showWelcome)
        }

    @Test fun `the marker is short and never contains local installation paths`() {
        val identity = "C:/Users/Someone/PrivateFolder/BitChord.exe"
        val key = DesktopWelcomeOnboarding.markerKey(identity)
        assertTrue(key.length <= Preferences.MAX_KEY_LENGTH)
        assertFalse(key.contains("Someone"))
        assertFalse(key.contains("PrivateFolder"))
    }

    @Test fun `preview environment flag handles expected values`() {
        assertTrue(DesktopWelcomeOnboarding.forceRequested("1"))
        assertTrue(DesktopWelcomeOnboarding.forceRequested("true"))
        assertTrue(DesktopWelcomeOnboarding.forceRequested("YES"))
        assertFalse(DesktopWelcomeOnboarding.forceRequested("0"))
        assertFalse(DesktopWelcomeOnboarding.forceRequested(null))
    }
}
