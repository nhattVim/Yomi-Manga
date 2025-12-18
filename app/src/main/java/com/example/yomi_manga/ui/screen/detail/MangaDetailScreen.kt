package com.example.yomi_manga.ui.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yomi_manga.di.AppContainer
import com.example.yomi_manga.ui.viewmodel.MangaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    viewModel: MangaViewModel = viewModel(),
    onBackClick: () -> Unit,
    onChapterClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val repo = AppContainer.provideDownloadRepository(context)
        viewModel.setDownloadRepository(repo)
    }

    LaunchedEffect(mangaId) {
        viewModel.loadMangaDetail(mangaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.lastReadChapterId != null) {
                FloatingActionButton(
                    onClick = {
                        val manga = uiState.selectedManga
                        if (manga != null) {
                            val allChapters = manga.chapters?.flatMap { it.items } ?: emptyList()
                            val sortedChapters = if (uiState.isChaptersDescending) {
                                allChapters.sortedByDescending { it.chapterName.toDoubleOrNull() ?: 0.0 }
                            } else {
                                allChapters.sortedBy { it.chapterName.toDoubleOrNull() ?: 0.0 }
                            }
                            
                            val index = sortedChapters.indexOfFirst { it.chapterApiData == uiState.lastReadChapterId }
                            if (index != -1) {
                                var offset = 3 // Header + Description + Chapter List Header
                                if (manga.genres?.isNotEmpty() == true) {
                                    offset += 1
                                }

                                coroutineScope.launch {
                                    listState.animateScrollToItem(index + offset)
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.History, contentDescription = "Go to Last Read")
                }
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Lỗi: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadMangaDetail(mangaId) }) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            uiState.selectedManga != null -> {
                val manga = uiState.selectedManga!!
                val allChapters = manga.chapters?.flatMap { it.items } ?: emptyList()
                val sortedChapters = remember(allChapters, uiState.isChaptersDescending) {
                    if (uiState.isChaptersDescending) {
                        allChapters.sortedByDescending { it.chapterName.toDoubleOrNull() ?: 0.0 }
                    } else {
                        allChapters.sortedBy { it.chapterName.toDoubleOrNull() ?: 0.0 }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AsyncImage(
                                model = manga.cover ?: "",
                                contentDescription = manga.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = manga.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                manga.authorName?.let {
                                    Text(
                                        text = "Tác giả: $it",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                manga.status?.let {
                                    Text(
                                        text = "Trạng thái: $it",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        manga.description?.let {
                            Text(
                                text = "Mô tả",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (manga.genres?.isNotEmpty() == true) {
                        item {
                            Text(
                                text = "Thể loại",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                manga.genres!!.forEach { genre ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { },
                                        label = { Text(genre) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Danh sách chương",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                IconButton(onClick = { viewModel.toggleChapterSort() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sắp xếp",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (allChapters.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier.size(42.dp),
                                        onClick = {
                                            viewModel.downloadAllChapters(allChapters)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Downloading,
                                            contentDescription = "Tải tất cả",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (allChapters.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có chap đang cập nhật",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        sortedChapters.forEach { chapterData ->
                            item {
                                val isDownloaded =
                                    uiState.downloadedChapters.contains(chapterData.chapterApiData)
                                val isDownloading =
                                    uiState.downloadingChapterIds.contains(chapterData.chapterApiData)
                                val isLastRead =
                                    uiState.lastReadChapterId == chapterData.chapterApiData

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onChapterClick(chapterData.chapterApiData)
                                        },
                                    colors = if (isLastRead) {
                                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    } else {
                                        CardDefaults.cardColors()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Chapter " + chapterData.chapterName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isLastRead) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isLastRead) {
                                                Text(
                                                    text = "Đang đọc",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        if (isDownloading) {
                                            IconButton(
                                                modifier = Modifier.size(42.dp),
                                                onClick = {
                                                    viewModel.cancelDownload(chapterData.chapterApiData)
                                                },
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                modifier = Modifier.size(42.dp),
                                                onClick = {
                                                    viewModel.downloadChapter(
                                                        mangaId = manga.id,
                                                        chapterApiData = chapterData.chapterApiData,
                                                        chapterTitle = chapterData.chapterName,
                                                        chapterNumber = chapterData.chapterName.toFloatOrNull()
                                                            ?: 0f
                                                    )
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                                    contentDescription = if (isDownloaded) "Downloaded" else "Download",
                                                    tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.downloadMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Snackbar(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = uiState.downloadMessage ?: "")
                        }
                    }
                }
            }
        }
    }
}
