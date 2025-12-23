package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.DownloadedManga
import com.example.yomi_manga.data.model.MangaAndChapters
import com.example.yomi_manga.data.repository.DownloadRepository
import com.example.yomi_manga.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val downloadedManga: List<MangaAndChapters> = emptyList(),
    val isLoading: Boolean = false,
    val selectedManga: MangaAndChapters? = null,
    val selectedItems: Set<String> = emptySet(),
    val themeMode: String = "system"
)

class SettingsViewModel(
    private var downloadRepository: DownloadRepository? = null,
    private var settingsRepository: SettingsRepository? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun init(downloadRepo: DownloadRepository, settingsRepo: SettingsRepository) {
        if (this.downloadRepository == null) {
            this.downloadRepository = downloadRepo
            loadDownloadedManga()
        }
        if (this.settingsRepository == null) {
            this.settingsRepository = settingsRepo
            observeTheme()
        }
    }

    private fun observeTheme() {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.themeMode.collect { mode ->
                    _uiState.value = _uiState.value.copy(themeMode = mode)
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository?.setThemeMode(mode)
        }
    }

    private fun loadDownloadedManga() {
        val repo = downloadRepository ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getAllDownloadedManga().collect { mangaList ->
                _uiState.value = _uiState.value.copy(
                    downloadedManga = mangaList,
                    isLoading = false
                )
                
                // Update selected manga if it's currently open
                val currentSelected = _uiState.value.selectedManga
                if (currentSelected != null) {
                    val updated = mangaList.find { it.manga.mangaId == currentSelected.manga.mangaId }
                    _uiState.value = _uiState.value.copy(selectedManga = updated)
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

    fun deleteSelectedItems() {
        val repo = downloadRepository ?: return
        val selectedIds = _uiState.value.selectedItems
        val selectedManga = _uiState.value.selectedManga

        viewModelScope.launch {
            if (selectedManga == null) {
                val mangasToDelete = _uiState.value.downloadedManga.filter { it.manga.mangaId in selectedIds }
                mangasToDelete.forEach { repo.deleteManga(it.manga) }
            } else {
                val chaptersToDelete = selectedManga.chapters.filter { it.chapterId in selectedIds }
                chaptersToDelete.forEach { repo.deleteChapter(it) }
            }
            _uiState.value = _uiState.value.copy(selectedItems = emptySet())
        }
    }

    fun deleteManga(manga: DownloadedManga) {
        viewModelScope.launch {
            downloadRepository?.deleteManga(manga)
        }
    }

    fun deleteChapter(chapter: DownloadedChapter) {
        viewModelScope.launch {
            downloadRepository?.deleteChapter(chapter)
        }
    }
}
