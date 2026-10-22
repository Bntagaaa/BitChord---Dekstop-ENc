package com.music.bitchord.desktop

import com.music.bitchord.data.model.SPOTIFY_MISSING_PREFIX
import com.music.bitchord.data.model.SPOTIFY_PENDING_PREFIX
import com.music.bitchord.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spotify metadata must never be mistaken for a playable YouTube video id. */
class DesktopSpotifyPlaybackTest {
    @Test
    fun pendingSpotifyRowCanBeMatchedOnDemand() {
        val song = Song(
            videoId = SPOTIFY_PENDING_PREFIX + "spotify123",
            title = "A Song",
            artist = "An Artist",
            albumName = "An Album",
            durationText = "3:17",
            thumbnailUrl = "https://example.com/cover.jpg",
        )
        val track = song.unresolvedSpotifyTrack()!!
        assertEquals("spotify123", track.id)
        assertEquals("A Song", track.title)
        assertEquals("An Artist", track.artist)
        assertEquals("An Album", track.album)
        assertEquals(197_000, track.durationMs)
        assertEquals("https://example.com/cover.jpg", track.imageUrl)
    }

    @Test
    fun failedSpotifyMatchCanBeRetriedOnTap() {
        val song = Song(
            videoId = SPOTIFY_MISSING_PREFIX + "original",
            title = "Track",
            artist = "Artist",
            thumbnailUrl = null,
            durationText = "1:02:03",
        )
        assertEquals("original", song.unresolvedSpotifyTrack()?.id)
        assertEquals(3_723_000, song.unresolvedSpotifyTrack()?.durationMs)
    }

    @Test
    fun youtubeVideoIdsAreNeverRematchedAsSpotify() {
        val song = Song(videoId = "abc123", title = "Playable", artist = "Artist", thumbnailUrl = null)
        assertNull(song.unresolvedSpotifyTrack())
    }

    @Test
    fun malformedSpotifyDurationsDoNotCrash() {
        val song = Song(
            videoId = SPOTIFY_PENDING_PREFIX + "original",
            title = "Track",
            artist = "Artist",
            thumbnailUrl = null,
            durationText = "unknown",
        )
        assertEquals(0, song.unresolvedSpotifyTrack()?.durationMs)
    }
}
