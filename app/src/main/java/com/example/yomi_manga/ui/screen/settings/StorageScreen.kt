package com.example.yomi_manga.ui.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.MangaAndChapters
import com.example.yomi_manga.di.AppContainer
import com.example.yomi_manga.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit,
    onChapterClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val repo = AppContainer.provideDownloadRepository(context)
        viewModel.setDownloadRepository(repo)
    }

    val isSelectionMode = uiState.selectedItems.isNotEmpty()
    val selectedManga = uiState.selectedManga

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${uiState.selectedItems.size} đã chọn")
                    } else if (selectedManga != null) {
                        Text(selectedManga.manga.title)
                    } else {
                        Text("Quản lý tải xuống") 
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                         IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel Selection")
                        }
                    } else {
                        IconButton(onClick = {
                            if (selectedManga != null) {
                                viewModel.clearSelectedManga()
                            } else {
                                onBackClick()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                         IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = if (isSelectionMode) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.downloadedManga.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Chưa có truyện nào được tải xuống")
            }
        } else {
             if (selectedManga == null) {
                 // Manga List Mode
                 LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.downloadedManga, key = { it.manga.mangaId }) { mangaItem ->
                        val isSelected = uiState.selectedItems.contains(mangaItem.manga.mangaId)
                        DownloadedMangaItem(
                            mangaAndChapters = mangaItem,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(mangaItem.manga.mangaId)
                                } else {
                                    viewModel.selectManga(mangaItem)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(mangaItem.manga.mangaId)
                            },
                            onDeleteClick = {
                                viewModel.deleteManga(mangaItem.manga)
                            }
                        )
                    }
                }
             } else {
                 // Chapter List Mode
                 val chapters = remember(selectedManga) {
                     selectedManga.chapters.sortedByDescending { it.chapterNumber }
                 }
                 
                 LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chapters, key = { it.chapterId }) { chapter ->
                        val isSelected = uiState.selectedItems.contains(chapter.chapterId)
                        DownloadedChapterItem(
                            chapter = chapter,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(chapter.chapterId)
                                } else {
                                    onChapterClick(chapter.chapterId, chapter.mangaId)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(chapter.chapterId)
                            },
                            onDeleteClick = {
                                viewModel.deleteChapter(chapter)
                            }
                        )
                    }
                }
             }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadedMangaItem(
    mangaAndChapters: MangaAndChapters,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
     Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = mangaAndChapters.manga.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mangaAndChapters.manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${mangaAndChapters.chapters.size} chapter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                 mangaAndChapters.manga.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                         maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadedChapterItem(
    chapter: DownloadedChapter,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.chapterTitle.ifEmpty { "Chapter ${chapter.chapterNumber}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ngày tải: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(chapter.downloadDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${chapter.imagePaths.size} trang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
