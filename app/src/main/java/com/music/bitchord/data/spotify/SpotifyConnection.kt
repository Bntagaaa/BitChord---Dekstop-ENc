package com.music.bitchord.data.spotify

import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A cookie is only a candidate session; library access is the validation step. */
internal object SpotifyConnection {
    enum class Status { Disconnected, Connecting, Validating, Connected, NeedsReauth, TemporaryError }

    private val mutableStatus = MutableStateFlow(Status.Disconnected)
    val status = mutableStatus.asStateFlow()
    private var validationMutex = Mutex()
    private var generation = 0L
    private var validatedCookie: String? = null
    private var validatedPlaylists: List<SpotifyPlaylist>? = null

    @Synchronized
    fun sessionChanged(cookie: String) {
        generation++
        validationMutex = Mutex()
        validatedCookie = null
        validatedPlaylists = null
        mutableStatus.value = if (cookie.isBlank()) Status.Disconnected else Status.Validating
    }

    @Synchronized
    fun loginStarted() {
        if (AppSettings.spotifySpdcToken.value.isBlank()) mutableStatus.value = Status.Connecting
    }

    @Synchronized
    fun loginCancelled() {
        if (AppSettings.spotifySpdcToken.value.isBlank()) mutableStatus.value = Status.Disconnected
    }

    suspend fun validate(
        cookie: String,
        force: Boolean = false,
        load: suspend () -> List<SpotifyPlaylist> = { SpotifyLibrary.playlists() },
    ): List<SpotifyPlaylist>? = synchronized(this) { validationMutex }.withLock {
        if (cookie.isBlank() || AppSettings.spotifySpdcToken.value != cookie) return@withLock null
        val epoch = synchronized(this) {
            if (!force && validatedCookie == cookie) return@withLock validatedPlaylists
            mutableStatus.value = Status.Validating
            generation
        }
        try {
            val result = load()
            synchronized(this) {
                if (generation != epoch || AppSettings.spotifySpdcToken.value != cookie) return@withLock null
                validatedCookie = cookie
                validatedPlaylists = result
                mutableStatus.value = Status.Connected
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            synchronized(this) {
                if (generation == epoch && AppSettings.spotifySpdcToken.value == cookie) {
                    mutableStatus.value = if (failure is SpotifyLibraryHttpException && failure.code == 401) {
                        Status.NeedsReauth
                    } else {
                        Status.TemporaryError
                    }
                }
            }
            null
        }
    }
}
