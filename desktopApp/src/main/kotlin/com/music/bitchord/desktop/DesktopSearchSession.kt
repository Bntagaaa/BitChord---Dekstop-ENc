package com.music.bitchord.desktop

import androidx.compose.runtime.mutableStateOf
import com.music.bitchord.data.model.SearchFilter

/** The regular Search page's session; Quick Search deliberately owns a different session. */
internal class DesktopSearchSession<T> {
    val query = mutableStateOf("")
    val filter = mutableStateOf(SearchFilter.ALL)
    val rows = mutableStateOf<List<T>>(emptyList())
    val typing = mutableStateOf(false)
    val committed = mutableStateOf(false)
    val inputFocused = mutableStateOf(false)
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val generation = mutableStateOf(0L)

    data class Request(val generation: Long, val query: String, val filter: SearchFilter)

    fun edit(text: String) {
        generation.value++
        query.value = text
        typing.value = text.isNotBlank()
        committed.value = false
        loading.value = false
        error.value = null
        rows.value = emptyList()
    }

    fun focus(focused: Boolean) {
        inputFocused.value = focused
        // A result can receive focus on mouse-down, before its onClick runs on mouse-up.
        // Keep it mounted during that transfer; dismissal belongs to the whole Search
        // focus group, an outside press, or a completed result action.
        if (focused) typing.value = !committed.value && query.value.isNotBlank()
    }

    fun dismissSuggestions() {
        typing.value = false
    }

    fun reset() {
        edit("")
        focus(false)
    }

    fun submit(): Request? {
        if (query.value.isBlank()) return null
        generation.value++
        typing.value = false
        committed.value = true
        loading.value = true
        error.value = null
        rows.value = emptyList()
        return Request(generation.value, query.value, filter.value)
    }

    fun isCurrent(request: Request): Boolean = request.generation == generation.value &&
        request.query == query.value && request.filter == filter.value && committed.value

    fun complete(request: Request, results: List<T>): Boolean {
        if (!isCurrent(request)) return false
        rows.value = results
        error.value = null
        loading.value = false
        return true
    }

    fun fail(request: Request, message: String) {
        if (!isCurrent(request)) return
        rows.value = emptyList()
        error.value = message
        loading.value = false
    }
}
