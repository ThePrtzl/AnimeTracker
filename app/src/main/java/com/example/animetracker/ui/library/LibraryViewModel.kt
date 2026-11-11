package com.example.animetracker.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animetracker.data.AnimeRepository
import com.example.animetracker.domain.Franchise
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val loaded: Boolean = false,
    val items: List<Franchise> = emptyList(),
)

class LibraryViewModel(repository: AnimeRepository) : ViewModel() {
    val ui: StateFlow<LibraryUiState> =
        repository.observeLibrary()
            .map { LibraryUiState(loaded = true, items = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
