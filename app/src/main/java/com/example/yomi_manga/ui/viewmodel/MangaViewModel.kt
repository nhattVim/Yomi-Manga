package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.model.Category
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
    val selectedManga: Manga? = null,
    val categories: List<Category> = emptyList()
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
        loadHomeManga()
    }
    
    fun loadMangaList(slug: String, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMangaList(slug, page).fold(
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
    
    fun loadHomeManga() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getHomeManga().fold(
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
    
    fun loadMangaDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMangaDetail(slug).fold(
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
            loadMangaList(slug = "truyen-moi")
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
    
    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategoryList().fold(
                onSuccess = { categories ->
                    _uiState.value = _uiState.value.copy(
                        categories = categories
                    )
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                     _uiState.value = _uiState.value.copy(
                         error = "Failed to load categories: ${appError.message}"
                     )
                }
            )
        }
    }

    fun loadMangaByCategory(slug: String, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMangaByCategory(slug, page).fold(
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
