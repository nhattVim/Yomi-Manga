package com.example.yomi_manga.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.MangaAndChapters
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
    val selectedManga = uiState.selectedManga

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedManga != null) {
                        Text(selectedManga.manga.title)
                    } else {
                        Text("Quản lý tải xuống") 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedManga != null) {
                            viewModel.clearSelectedManga()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                        DownloadedMangaItem(
                            mangaAndChapters = mangaItem,
                            onClick = {
                                viewModel.selectManga(mangaItem)
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
                        DownloadedChapterItem(
                            chapter = chapter,
                            onClick = {
                                onChapterClick(chapter.chapterId, chapter.mangaId)
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

@Composable
fun DownloadedMangaItem(
    mangaAndChapters: MangaAndChapters,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
     Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

@Composable
fun DownloadedChapterItem(
    chapter: DownloadedChapter,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "${chapter.imagePaths.size} ảnh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
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
