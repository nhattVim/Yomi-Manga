package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.DownloadedManga
import com.example.yomi_manga.data.model.MangaAndChapters
import com.example.yomi_manga.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val downloadedManga: List<MangaAndChapters> = emptyList(),
    val isLoading: Boolean = false,
    val selectedManga: MangaAndChapters? = null,
    val selectedItems: Set<String> = emptySet() // IDs of selected items (mangaId or chapterId depending on context)
)

class SettingsViewModel(
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private var _repository: DownloadRepository? = downloadRepository

    fun setDownloadRepository(repo: DownloadRepository) {
        _repository = repo
        loadDownloadedManga()
    }
    
    private fun loadDownloadedManga() {
        val repo = _repository ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getAllDownloadedManga().collect { mangaList ->
                _uiState.value = _uiState.value.copy(
                    downloadedManga = mangaList,
                    isLoading = false
                )
                
                // Update selected manga if it's currently open
                if (_uiState.value.selectedManga != null) {
                    val updatedSelectedManga = mangaList.find { it.manga.mangaId == _uiState.value.selectedManga!!.manga.mangaId }
                    if (updatedSelectedManga != null) {
                        _uiState.value = _uiState.value.copy(selectedManga = updatedSelectedManga)
                    } else {
                        // Manga was deleted, close selection
                        _uiState.value = _uiState.value.copy(selectedManga = null)
                    }
                }
            }
        }
    }

    fun selectManga(manga: MangaAndChapters) {
        _uiState.value = _uiState.value.copy(selectedManga = manga, selectedItems = emptySet())
    }
    
    fun clearSelectedManga() {
        _uiState.value = _uiState.value.copy(selectedManga = null, selectedItems = emptySet())
    }

    fun toggleSelection(id: String) {
        val currentSelected = _uiState.value.selectedItems.toMutableSet()
        if (currentSelected.contains(id)) {
            currentSelected.remove(id)
        } else {
            currentSelected.add(id)
        }
        _uiState.value = _uiState.value.copy(selectedItems = currentSelected)
    }

    fun deleteSelectedItems() {
        val repo = _repository ?: return
        val selectedIds = _uiState.value.selectedItems
        val selectedManga = _uiState.value.selectedManga

        viewModelScope.launch {
            if (selectedManga == null) {
                // We are in manga list, delete selected mangas
                val mangasToDelete = _uiState.value.downloadedManga.filter { it.manga.mangaId in selectedIds }
                mangasToDelete.forEach { mangaAndChapters ->
                     repo.deleteManga(mangaAndChapters.manga)
                }
            } else {
                // We are in chapter list, delete selected chapters
                val chaptersToDelete = selectedManga.chapters.filter { it.chapterId in selectedIds }
                chaptersToDelete.forEach { chapter ->
                    repo.deleteChapter(chapter)
                }
            }
            _uiState.value = _uiState.value.copy(selectedItems = emptySet())
        }
    }

    fun deleteManga(manga: DownloadedManga) {
        val repo = _repository ?: return
        viewModelScope.launch {
            repo.deleteManga(manga)
        }
    }

    fun deleteChapter(chapter: DownloadedChapter) {
        val repo = _repository ?: return
        viewModelScope.launch {
            repo.deleteChapter(chapter)
        }
    }
    
    fun selectAll() {
        val selectedManga = _uiState.value.selectedManga
        if (selectedManga == null) {
            val allIds = _uiState.value.downloadedManga.map { it.manga.mangaId }.toSet()
            _uiState.value = _uiState.value.copy(selectedItems = allIds)
        } else {
            val allIds = selectedManga.chapters.map { it.chapterId }.toSet()
            _uiState.value = _uiState.value.copy(selectedItems = allIds)
        }
    }
    
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedItems = emptySet())
    }
}