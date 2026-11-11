package com.example.animetracker.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.data.BrowseItem
import com.example.animetracker.domain.LibraryStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val query: String = "",
    val genre: String? = null,
    /** AniList format filter (MOVIE, OVA...); null = everything. */
    val format: String? = null,
    val items: List<BrowseItem> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class BrowseViewModel(private val repository: AnimeRepository) : ViewModel() {

    private val _ui = MutableStateFlow(BrowseUiState())
    val ui: StateFlow<BrowseUiState> = _ui

    /** rootId -> library status, used to badge tiles that are already in the library. */
    val libraryStatuses: StateFlow<Map<Int, LibraryStatus>> =
        repository.observeLibraryStatuses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private var nextPage = 1
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        reload()
    }

    fun onQueryChange(query: String) {
        _ui.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            reload()
        }
    }

    fun onGenreChange(genre: String?) {
        _ui.update { it.copy(genre = genre) }
        reload()
    }

    fun onFormatChange(format: String?) {
        _ui.update { it.copy(format = format) }
        reload()
    }

    fun loadNext() {
        val state = _ui.value
        if (state.loading || state.endReached || state.error != null) return
        fetch()
    }

    fun retry() {
        _ui.update { it.copy(error = null) }
        fetch()
    }

    private fun reload() {
        loadJob?.cancel()
        nextPage = 1
        _ui.update { it.copy(items = emptyList(), endReached = false, error = null, loading = false) }
        fetch()
    }

    private fun fetch() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val state = _ui.value
                val result = repository.browse(nextPage, state.query, state.genre, state.format)
                nextPage = result.nextPage
                _ui.update { current ->
                    current.copy(
                        items = (current.items + result.items).distinctBy { it.id },
                        endReached = !result.hasNext,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Couldn't load anime.") }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
