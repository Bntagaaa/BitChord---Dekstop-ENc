package com.music.bitchord

import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.spotify.SpotifyConnection
import com.music.bitchord.data.spotify.SpotifyLibraryHttpException
import com.music.bitchord.data.spotify.SpotifyPlaylist
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotifyConnectionTest {
    private val playlists = listOf(SpotifyPlaylist("one", "One", null, null))

    @After
    fun reset() {
        AppSettings.spotifySpdcToken.value = ""
        SpotifyConnection.sessionChanged("")
    }

    @Test
    fun cookieAloneIsNotConnected() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        assertEquals(SpotifyConnection.Status.Validating, SpotifyConnection.status.value)
        assertEquals(playlists, SpotifyConnection.validate("a", load = { playlists }))
        assertEquals(SpotifyConnection.Status.Connected, SpotifyConnection.status.value)
    }

    @Test
    fun oldRequestCannotRestoreDisconnectedSession() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        val pending = CompletableDeferred<List<SpotifyPlaylist>>()
        val request = async { SpotifyConnection.validate("a", load = { pending.await() }) }
        runCurrent()
        SpotifyConnection.sessionChanged("")
        AppSettings.spotifySpdcToken.value = ""
        pending.complete(playlists)
        assertNull(request.await())
        assertEquals(SpotifyConnection.Status.Disconnected, SpotifyConnection.status.value)
    }

    @Test
    fun oldAccountCannotBecomeNewAccountResult() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        val pending = CompletableDeferred<List<SpotifyPlaylist>>()
        val oldRequest = async { SpotifyConnection.validate("a", load = { pending.await() }) }
        runCurrent()
        SpotifyConnection.sessionChanged("b")
        AppSettings.spotifySpdcToken.value = "b"
        val second = listOf(SpotifyPlaylist("two", "Two", null, null))
        val newRequest = async { SpotifyConnection.validate("b", load = { second }) }
        runCurrent()
        assertEquals(second, newRequest.await())
        assertEquals(SpotifyConnection.Status.Connected, SpotifyConnection.status.value)
        pending.complete(playlists)
        assertNull(oldRequest.await())
        assertEquals(SpotifyConnection.Status.Connected, SpotifyConnection.status.value)
    }

    @Test
    fun onlyUnauthorizedMarksNeedsReauth() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        assertNull(SpotifyConnection.validate("a", load = { throw SpotifyLibraryHttpException(401) }))
        assertEquals(SpotifyConnection.Status.NeedsReauth, SpotifyConnection.status.value)
        assertNull(SpotifyConnection.validate("a", load = { throw SpotifyLibraryHttpException(429) }))
        assertEquals(SpotifyConnection.Status.TemporaryError, SpotifyConnection.status.value)
        assertNull(SpotifyConnection.validate("a", load = { throw SpotifyLibraryHttpException(403) }))
        assertEquals(SpotifyConnection.Status.TemporaryError, SpotifyConnection.status.value)
    }

    @Test
    fun cancellationIsNotReportedAsAuthenticationFailure() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        try {
            SpotifyConnection.validate("a", load = { throw CancellationException("screen closed") })
            fail("CancellationException must propagate")
        } catch (_: CancellationException) {
            assertEquals(SpotifyConnection.Status.Validating, SpotifyConnection.status.value)
        }
    }

    @Test
    fun successfulValidationReusesResult() = runTest {
        AppSettings.spotifySpdcToken.value = "a"
        SpotifyConnection.sessionChanged("a")
        val first = SpotifyConnection.validate("a", load = { playlists })
        val cached = SpotifyConnection.validate("a", load = { error("must use cache") })
        assertSame(first, cached)
    }
}
