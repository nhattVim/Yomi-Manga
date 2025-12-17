package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.data.model.FavoriteManga
import com.example.yomi_manga.data.model.ReadingHistory
import com.example.yomi_manga.data.repository.LibraryRepository
import com.example.yomi_manga.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val favorites: List<FavoriteManga> = emptyList(),
    val history: List<ReadingHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LibraryViewModel(
    private val repository: LibraryRepository = AppContainer.libraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
        loadHistory()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavoriteMangaList().collect { favorites ->
                _uiState.value = _uiState.value.copy(favorites = favorites)
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.getReadingHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }
}