package com.music.bitchord.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.screens.SearchScreen
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Exercise pointer dispatch and focus transfer; a semantics-only click bypasses both. */
class DesktopSearchPointerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `typeahead plays selected song on mouse release`() = clickResult(false)

    @Test fun `focus transfer to a pressed result cannot cancel playback`() = clickResult(true)

    @Test fun `outside press dismisses suggestions and still activates landing`() = clickResult(false, outside = true)

    private fun clickResult(transferFocus: Boolean, outside: Boolean = false) {
        val session = DesktopSearchSession<SearchResult>()
        val song = Song(videoId = "selected", title = "Selected song", artist = "Artist", thumbnailUrl = null, fromAutoplay = false)
        val played = mutableListOf<Song>()
        var landingClicks = 0
        var outsidePresses = 0
        session.edit("sel")
        compose.setContent {
            MaterialTheme {
                val focusManager = LocalFocusManager.current
                SearchScreen(
                    modifier = Modifier.desktopSearchFocus(session),
                    query = session.query.value,
                    onQueryChange = session::edit,
                    filter = SearchFilter.ALL,
                    currentSong = null,
                    isPlaying = false,
                    onFilterChange = {},
                    results = null,
                    loadingMore = false,
                    onLoadMore = {},
                    listState = rememberLazyListState(),
                    scrollResetTrigger = 0,
                    focusRequested = true,
                    onFocusHandled = {},
                    onSongClick = { songs, index -> played += songs[index] },
                    onSongLongPress = {},
                    onSongSwipe = {},
                    onTopResultPlay = {
                        played += it
                        session.dismissSuggestions()
                        focusManager.clearFocus()
                    },
                    onTopResultPlaylist = {},
                    onBrowseClick = {},
                    history = emptyList(),
                    suggestions = if (session.typing.value) listOf("sel") else emptyList(),
                    typeaheadResults = listOf(SearchResult.Track(song)),
                    onSubmit = {},
                    onSuggestionClick = {},
                    onHistoryClick = {},
                    onHistoryRemove = {},
                    onHistoryClear = {},
                    contentPadding = PaddingValues(0.dp),
                    topPadding = 0.dp,
                    searchField = { requester ->
                        DesktopSearchField(
                            query = session.query.value,
                            onQueryChange = session::edit,
                            onSearch = {},
                            focusRequester = requester,
                            onFocusChanged = session::focus,
                        )
                    },
                    showLanding = true,
                    landingSuggestionsEnabled = session.typing.value,
                    onLandingOutsidePress = {
                        outsidePresses++
                        session.dismissSuggestions()
                        focusManager.clearFocus()
                    },
                    landingContent = {
                        Box(Modifier.fillMaxSize().testTag("landing").clickable { landingClicks++ })
                    },
                )
            }
        }
        if (outside) {
            compose.onNodeWithTag("landing").performMouseInput {
                click(bottomLeft + Offset(8f, -8f))
            }
            compose.onNodeWithText(song.title).assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(1, landingClicks)
                assertEquals(1, outsidePresses)
                assertEquals(emptyList(), played)
                assertFalse(session.inputFocused.value)
            }
            return
        }
        compose.onNodeWithText(song.title).performMouseInput { moveTo(center); press() }
        if (transferFocus) {
            compose.onNodeWithText(song.title).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        }
        compose.waitForIdle()
        compose.onNodeWithText(song.title).assertExists()
        assertEquals(emptyList(), played)
        compose.onNodeWithText(song.title).performMouseInput { release() }
        compose.runOnIdle {
            assertEquals(listOf(song), played)
            assertEquals(0, landingClicks)
            assertEquals(0, outsidePresses)
            assertFalse(session.inputFocused.value)
        }
        compose.onNodeWithText(song.title).assertDoesNotExist()
    }
}
