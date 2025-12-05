package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.constants.AppConstants
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MangaUiState(
    val mangaList: List<Manga> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedManga: Manga? = null
) {
    companion object {
        fun initial() = MangaUiState()
    }
}

class MangaViewModel(
    private val repository: com.example.yomi_manga.data.repository.MangaRepository = AppContainer.mangaRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MangaUiState())
    val uiState: StateFlow<MangaUiState> = _uiState.asStateFlow()
    
    init {
        loadMangaList()
    }
    
    fun loadMangaList(page: Int = 1, limit: Int = AppConstants.DEFAULT_PAGE_SIZE) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMangaList(page, limit).fold(
                onSuccess = { mangaList ->
                    _uiState.value = _uiState.value.copy(
                        mangaList = mangaList,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            )
        }
    }
    
    fun loadMangaDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMangaDetail(id).fold(
                onSuccess = { manga ->
                    _uiState.value = _uiState.value.copy(
                        selectedManga = manga,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            )
        }
    }
    
    fun searchManga(query: String) {
        if (query.isBlank()) {
            loadMangaList()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.searchManga(query).fold(
                onSuccess = { mangaList ->
                    _uiState.value = _uiState.value.copy(
                        mangaList = mangaList,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            )
        }
    }
    
    fun loadPopularManga(page: Int = 1, limit: Int = AppConstants.DEFAULT_PAGE_SIZE) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getPopularManga(page, limit).fold(
                onSuccess = { mangaList ->
                    _uiState.value = _uiState.value.copy(
                        mangaList = mangaList,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

