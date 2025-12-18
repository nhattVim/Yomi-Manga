package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.model.Category
import com.example.yomi_manga.data.model.ChapterData
import com.example.yomi_manga.data.model.FavoriteManga
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.data.model.ReadingHistory
import com.example.yomi_manga.data.repository.DownloadRepository
import com.example.yomi_manga.data.repository.LibraryRepository
import com.example.yomi_manga.data.repository.MangaRepository
import com.example.yomi_manga.di.AppContainer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryData(
    val mangaList: List<Manga> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1
)

data class MangaUiState(
    val mangaList: List<Manga> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedManga: Manga? = null,
    val categories: List<Category> = emptyList(),
    val categoriesData: Map<String, CategoryData> = emptyMap(),
    val downloadedChapters: Set<String> = emptySet(),
    val downloadingChapterIds: Set<String> = emptySet(),
    val downloadMessage: String? = null,
    val downloadMessageId: Long = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val currentSlug: String = "truyen-moi",
    val isCategory: Boolean = false,
    val currentSearchQuery: String? = null,
    val isFavorite: Boolean = false,
    val lastReadChapterId: String? = null,
    val isChaptersDescending: Boolean = true
) {
    companion object {
        fun initial() = MangaUiState()
    }
}

class MangaViewModel(
    private val repository: MangaRepository = AppContainer.mangaRepository,
    private val downloadRepository: DownloadRepository? = null,
    private val libraryRepository: LibraryRepository = AppContainer.libraryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MangaUiState())
    val uiState: StateFlow<MangaUiState> = _uiState.asStateFlow()
    
    private var _downloadRepository: DownloadRepository? = downloadRepository
    private var messageJob: Job? = null

    fun setDownloadRepository(repo: DownloadRepository) {
        _downloadRepository = repo
        observeRunningDownloads()
    }

    init {
        loadHomeManga()
    }
    
    private fun observeRunningDownloads() {
        val repo = _downloadRepository ?: return
        viewModelScope.launch {
            repo.runningDownloads.collect { runningIds ->
                _uiState.value = _uiState.value.copy(downloadingChapterIds = runningIds)
            }
        }
    }
    
    fun loadMangaList(slug: String, page: Int = 1) {
        // Cập nhật state chung
        _uiState.update { it.copy(currentSlug = slug, currentPage = page, isCategory = false, currentSearchQuery = null) }
        
        // Cập nhật cache cho category này
        updateCategoryState(slug) { it.copy(isLoading = true, error = null, currentPage = page) }

        viewModelScope.launch {
            repository.getMangaList(slug, page).fold(
                onSuccess = { mangaList ->
                    _uiState.update { state ->
                        state.copy(
                            mangaList = if (state.currentSlug == slug) mangaList else state.mangaList,
                            isLoading = if (state.currentSlug == slug) false else state.isLoading,
                            categoriesData = state.categoriesData + (slug to CategoryData(
                                mangaList = mangaList,
                                isLoading = false,
                                currentPage = page
                            ))
                        )
                    }
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    updateCategoryState(slug) { it.copy(isLoading = false, error = appError.message) }
                    if (_uiState.value.currentSlug == slug) {
                        _uiState.update { it.copy(isLoading = false, error = appError.message) }
                    }
                }
            )
        }
    }
    
    fun loadHomeManga() {
        loadMangaList("truyen-moi")
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
                    checkDownloadedChapters(manga.id)
                    checkFavorite(manga.id)
                    checkLastReadChapter(manga.id)
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

    private fun checkFavorite(mangaId: String) {
        viewModelScope.launch {
            val isFav = libraryRepository.isFavorite(mangaId)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    private fun checkLastReadChapter(mangaId: String) {
        viewModelScope.launch {
            val history = libraryRepository.getReadingHistoryForManga(mangaId)
            _uiState.value = _uiState.value.copy(lastReadChapterId = history?.chapterId)
        }
    }

    fun toggleFavorite() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            showTemporaryMessage("Vui lòng đăng nhập để sử dụng tính năng này")
            return
        }
        val manga = _uiState.value.selectedManga ?: return
        val isFav = _uiState.value.isFavorite
        val mangaId = manga.id ?: return

        viewModelScope.launch {
            try {
                if (isFav) {
                    libraryRepository.removeFavorite(mangaId)
                    _uiState.value = _uiState.value.copy(isFavorite = false)
                    showTemporaryMessage("Đã xóa khỏi yêu thích")
                } else {
                    val favoriteManga = FavoriteManga(
                        mangaId = mangaId,
                        title = manga.title,
                        coverUrl = manga.cover ?: "",
                        slug = manga.slug
                    )
                    libraryRepository.addFavorite(favoriteManga)
                    _uiState.value = _uiState.value.copy(isFavorite = true)
                    showTemporaryMessage("Đã thêm vào yêu thích")
                }
            } catch (e: Exception) {
                showTemporaryMessage("Lỗi: ${e.message}")
            }
        }
    }

    fun saveReadingHistory(manga: Manga, chapterId: String, chapterTitle: String) {
        if (FirebaseAuth.getInstance().currentUser == null) return

        val mangaId = manga.id

        viewModelScope.launch {
            try {
                val history = ReadingHistory(
                    mangaId = mangaId,
                    title = manga.title,
                    coverUrl = manga.cover ?: "",
                    slug = manga.slug,
                    chapterId = chapterId,
                    chapterTitle = chapterTitle
                )
                libraryRepository.addToHistory(history)
            } catch (_: Exception) {
                // Fail silently for history
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
                val manga = _uiState.value.selectedManga
                if (manga == null) {
                    showTemporaryMessage("Manga details not loaded")
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
                         showTemporaryMessage("Đang tải: $chapterTitle")
                     },
                     onFailure = {
                         showTemporaryMessage("Lỗi tải $chapterTitle: ${it.message}")
                     }
                )

            } catch (e: Exception) {
                showTemporaryMessage("Lỗi: ${e.message}")
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

        chaptersToDownload.forEach { chapter ->
            downloadChapter(
                mangaId = manga.id,
                chapterApiData = chapter.chapterApiData,
                chapterTitle = chapter.chapterName,
                chapterNumber = chapter.chapterName.toFloatOrNull() ?: 0f
            )
        }
    }

    fun cancelDownload(chapterApiData: String) {
        val repo = _downloadRepository ?: return
        repo.cancelDownload(chapterApiData)
        showTemporaryMessage("Đã huỷ tải xuống")
    }

    private fun showTemporaryMessage(message: String) {
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
    
    fun searchManga(query: String, page: Int = 1) {
        if (query.isBlank()) {
            loadMangaList(slug = "truyen-moi")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null,
                currentSearchQuery = query,
                currentPage = page,
                isCategory = false
            )
            repository.searchManga(query, page).fold(
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
        _uiState.update { it.copy(currentSlug = slug, currentPage = page, isCategory = true, currentSearchQuery = null) }
        updateCategoryState(slug) { it.copy(isLoading = true, error = null, currentPage = page) }

        viewModelScope.launch {
            repository.getMangaByCategory(slug, page).fold(
                onSuccess = { mangaList ->
                    _uiState.update { state ->
                        state.copy(
                            mangaList = if (state.currentSlug == slug) mangaList else state.mangaList,
                            isLoading = if (state.currentSlug == slug) false else state.isLoading,
                            categoriesData = state.categoriesData + (slug to CategoryData(
                                mangaList = mangaList,
                                isLoading = false,
                                currentPage = page
                            ))
                        )
                    }
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    updateCategoryState(slug) { it.copy(isLoading = false, error = appError.message) }
                    if (_uiState.value.currentSlug == slug) {
                        _uiState.update { it.copy(isLoading = false, error = appError.message) }
                    }
                }
            )
        }
    }
    
    private fun updateCategoryState(slug: String, update: (CategoryData) -> CategoryData) {
        _uiState.update { state ->
            val currentData = state.categoriesData[slug] ?: CategoryData()
            state.copy(categoriesData = state.categoriesData + (slug to update(currentData)))
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val nextPage = currentState.currentPage + 1
        
        if (currentState.currentSearchQuery != null) {
            searchManga(currentState.currentSearchQuery, nextPage)
        } else if (currentState.isCategory) {
            loadMangaByCategory(currentState.currentSlug, nextPage)
        } else {
            loadMangaList(currentState.currentSlug, nextPage)
        }
    }
    
    fun loadPreviousPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 1) {
            val prevPage = currentState.currentPage - 1
            if (currentState.currentSearchQuery != null) {
                searchManga(currentState.currentSearchQuery, prevPage)
            } else if (currentState.isCategory) {
                loadMangaByCategory(currentState.currentSlug, prevPage)
            } else {
                loadMangaList(currentState.currentSlug, prevPage)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleChapterSort() {
        _uiState.update { it.copy(isChaptersDescending = !it.isChaptersDescending) }
    }
}