package com.music.bitchord.desktop

import com.music.bitchord.data.model.SearchFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSearchSessionTest {
    @Test fun `typing keeps landing until submit and clearing restores it`() {
        val session = DesktopSearchSession<String>()
        session.focus(true)
        for (text in listOf("s", "so", "song", "son", "song")) {
            session.edit(text)
            assertFalse(session.committed.value)
            assertTrue(session.typing.value)
        }
        val request = session.submit()!!
        assertTrue(session.committed.value)
        assertFalse(session.typing.value)
        assertTrue(session.loading.value)
        assertTrue(session.complete(request, listOf("hit")))
        session.edit("")
        assertFalse(session.committed.value)
        assertFalse(session.loading.value)
        assertEquals(emptyList(), session.rows.value)
        assertNull(session.submit())
    }

    @Test fun `outside press hides suggestions and refocusing preserves editable query`() {
        val session = DesktopSearchSession<String>()
        session.focus(true)
        session.edit("song")
        session.focus(false)
        // Losing input focus to a result must not unmount the pending click.
        assertTrue(session.typing.value)
        session.dismissSuggestions()
        assertFalse(session.typing.value)
        assertFalse(session.inputFocused.value)
        assertFalse(session.committed.value)
        session.focus(true)
        assertTrue(session.typing.value)
        assertEquals("song", session.query.value)
    }

    @Test fun `older completion and failure cannot replace newer submission`() {
        val session = DesktopSearchSession<String>()
        session.edit("old")
        val old = session.submit()!!
        session.edit("new")
        val current = session.submit()!!
        assertFalse(session.complete(old, listOf("stale")))
        session.fail(old, "stale failure")
        assertTrue(session.loading.value)
        assertNull(session.error.value)
        assertTrue(session.complete(current, listOf("current")))
        assertEquals(listOf("current"), session.rows.value)
        session.fail(old, "late failure")
        assertNull(session.error.value)
    }

    @Test fun `reset invalidates request even after same query is submitted again`() {
        val session = DesktopSearchSession<String>()
        session.edit("same")
        val old = session.submit()!!
        session.reset()
        session.edit("same")
        val current = session.submit()!!
        assertFalse(session.complete(old, listOf("stale")))
        assertTrue(session.complete(current, listOf("current")))
    }

    @Test fun `filter submission cannot be overwritten by previous filter`() {
        val session = DesktopSearchSession<String>()
        session.edit("song")
        val all = session.submit()!!
        session.filter.value = SearchFilter.SONGS
        val songs = session.submit()!!
        assertFalse(session.complete(all, listOf("all")))
        assertTrue(session.complete(songs, listOf("song")))
        assertEquals(SearchFilter.SONGS, songs.filter)
    }

    @Test fun `editing or clearing rejects a pending response without another submit`() {
        for (text in listOf("", "edited")) {
            val session = DesktopSearchSession<String>()
            session.edit("old")
            val pending = session.submit()!!
            session.edit(text)
            assertFalse(session.complete(pending, listOf("stale")))
            session.fail(pending, "stale failure")
            assertFalse(session.committed.value)
            assertFalse(session.loading.value)
            assertNull(session.error.value)
        }
    }

    @Test fun `submit focus loss cannot cancel current results or reopen suggestions`() {
        val session = DesktopSearchSession<String>()
        session.edit("song")
        val request = session.submit()!!
        session.focus(false)
        assertTrue(session.complete(request, listOf("hit")))
        session.focus(true)
        assertFalse(session.typing.value)
        assertTrue(session.committed.value)
    }

    @Test fun `failure clears loading and a new edit removes the error`() {
        val session = DesktopSearchSession<String>()
        session.edit("song")
        session.fail(session.submit()!!, "offline")
        assertFalse(session.loading.value)
        assertEquals("offline", session.error.value)
        session.edit("song two")
        assertNull(session.error.value)
        assertFalse(session.committed.value)
    }

    @Test fun `same query submissions and delete then retype keep distinct request identities`() {
        val session = DesktopSearchSession<String>()
        session.edit("song")
        val first = session.submit()!!
        val repeated = session.submit()!!
        session.edit("son")
        session.edit("song")
        val current = session.submit()!!
        assertFalse(session.complete(first, listOf("old")))
        assertFalse(session.complete(repeated, listOf("repeated")))
        assertTrue(session.loading.value)
        assertTrue(session.complete(current, listOf("current")))
    }

    @Test fun `empty results stay in the submitted results view`() {
        val session = DesktopSearchSession<String>()
        session.edit("no matches")
        assertTrue(session.complete(session.submit()!!, emptyList()))
        assertTrue(session.committed.value)
        assertFalse(session.loading.value)
        assertFalse(session.typing.value)
        assertNull(session.error.value)
    }
}
