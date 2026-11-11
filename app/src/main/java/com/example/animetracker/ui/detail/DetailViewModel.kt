package com.example.animetracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.domain.Franchise
import com.example.animetracker.domain.Season
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val franchise: Franchise? = null,
)

class DetailViewModel(
    private val repository: AnimeRepository,
    private val mediaId: Int,
) : ViewModel() {

    private val _ui = MutableStateFlow(DetailUiState())
    val ui: StateFlow<DetailUiState> = _ui

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _ui.value = DetailUiState(loading = true)
        loadJob = viewModelScope.launch {
            try {
                // Fetch (or reuse the cached copy of) the anime and all its seasons, then keep
                // listening so the screen updates whenever progress changes.
                val rootId = repository.ensureFranchise(mediaId)
                repository.observeFranchise(rootId).collect { franchise ->
                    _ui.update { it.copy(loading = false, error = null, franchise = franchise) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Couldn't load this anime.") }
            }
        }
    }

    /** Marks episodes 1..[watched] of the season as watched. 0 clears the season. */
    fun setWatched(season: Season, watched: Int) {
        val root = _ui.value.franchise?.rootId ?: return
        viewModelScope.launch { repository.setWatched(season, root, watched) }
    }

    fun markAll(season: Season) {
        setWatched(season, if (season.totalEpisodes > 0) season.totalEpisodes else season.watched + 1)
    }

    fun markNext(season: Season) {
        season.nextEpisode?.let { setWatched(season, it) }
    }

    fun toggleDrop(season: Season) {
        val root = _ui.value.franchise?.rootId ?: return
        viewModelScope.launch { repository.setDropped(season, root, !season.dropped) }
    }
}
