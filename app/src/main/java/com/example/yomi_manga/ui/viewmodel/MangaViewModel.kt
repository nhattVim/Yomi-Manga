package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.model.Category
import com.example.yomi_manga.data.model.ChapterData
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.data.repository.DownloadRepository
import com.example.yomi_manga.data.repository.MangaRepository
import com.example.yomi_manga.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MangaUiState(
    val mangaList: List<Manga> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedManga: Manga? = null,
    val categories: List<Category> = emptyList(),
    val downloadedChapters: Set<String> = emptySet(),
    val downloadingChapterIds: Set<String> = emptySet(), // Track individual chapters downloading
    val downloadMessage: String? = null,
    val downloadMessageId: Long = 0 // Used to trigger snackbar recomposition/show
) {
    companion object {
        fun initial() = MangaUiState()
    }
}

class MangaViewModel(
    private val repository: MangaRepository = AppContainer.mangaRepository,
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MangaUiState())
    val uiState: StateFlow<MangaUiState> = _uiState.asStateFlow()
    
    private var _downloadRepository: DownloadRepository? = downloadRepository
    private var messageJob: Job? = null

    fun setDownloadRepository(repo: DownloadRepository) {
        _downloadRepository = repo
    }

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
                    checkDownloadedChapters(manga.id ?: "")
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
    
    private fun checkDownloadedChapters(mangaId: String) {
        val repo = _downloadRepository ?: return
        viewModelScope.launch {
            repo.getDownloadedChapters(mangaId).collect { chapters ->
                _uiState.value = _uiState.value.copy(
                    downloadedChapters = chapters.map { it.chapterId }.toSet()
                )
            }
        }
    }

    fun downloadChapter(mangaId: String, chapterApiData: String, chapterTitle: String, chapterNumber: Float) {
        val repo = _downloadRepository
        if (repo == null) {
            showTemporaryMessage("Download repository not initialized")
            return
        }

        if (_uiState.value.downloadingChapterIds.contains(chapterApiData)) return

        viewModelScope.launch {
            try {
                // Add to downloading set
                val currentDownloading = _uiState.value.downloadingChapterIds.toMutableSet()
                currentDownloading.add(chapterApiData)
                _uiState.value = _uiState.value.copy(downloadingChapterIds = currentDownloading)
                
                // We need the full manga object to save its details.
                val manga = _uiState.value.selectedManga
                if (manga == null) {
                    showTemporaryMessage("Manga details not loaded")
                    removeFromDownloading(chapterApiData)
                    return@launch
                }

                val chapterContent = repository.getChapterPages(chapterApiData) 
                
                chapterContent.fold(
                     onSuccess = { pages -> 
                         repo.downloadChapter(
                             manga = manga,
                             chapterId = chapterApiData,
                             chapterTitle = chapterTitle,
                             chapterNumber = chapterNumber,
                             imageUrls = pages.map { it.imageFile }
                         )
                         showTemporaryMessage("Đã tải xong: $chapterTitle")
                         removeFromDownloading(chapterApiData)
                     },
                     onFailure = {
                         showTemporaryMessage("Lỗi tải $chapterTitle: ${it.message}")
                         removeFromDownloading(chapterApiData)
                     }
                )

            } catch (e: Exception) {
                showTemporaryMessage("Lỗi: ${e.message}")
                removeFromDownloading(chapterApiData)
            }
        }
    }

    fun downloadAllChapters(chapters: List<ChapterData>) {
        val manga = _uiState.value.selectedManga ?: return
        val downloaded = _uiState.value.downloadedChapters
        val downloading = _uiState.value.downloadingChapterIds
        
        val chaptersToDownload = chapters.filter { 
            !downloaded.contains(it.chapterApiData) && !downloading.contains(it.chapterApiData)
        }

        if (chaptersToDownload.isEmpty()) {
            showTemporaryMessage("Tất cả đã được tải hoặc đang tải")
            return
        }

        showTemporaryMessage("Bắt đầu tải ${chaptersToDownload.size} chapter...")

        // Launch downloads concurrently (or sequentially if you prefer loop)
        // Since we are inside viewModelScope, these launches are children and run asynchronously
        chaptersToDownload.forEach { chapter ->
            downloadChapter(
                mangaId = manga.id ?: "",
                chapterApiData = chapter.chapterApiData,
                chapterTitle = chapter.chapterName,
                chapterNumber = chapter.chapterName.toFloatOrNull() ?: 0f
            )
        }
    }

    private fun removeFromDownloading(chapterId: String) {
        val currentDownloading = _uiState.value.downloadingChapterIds.toMutableSet()
        currentDownloading.remove(chapterId)
        _uiState.value = _uiState.value.copy(downloadingChapterIds = currentDownloading)
    }

    private fun showTemporaryMessage(message: String) {
        // Cancel previous clear job if exists
        messageJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            downloadMessage = message,
            downloadMessageId = System.currentTimeMillis()
        )

        messageJob = viewModelScope.launch {
            delay(3000)
            _uiState.value = _uiState.value.copy(downloadMessage = null)
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