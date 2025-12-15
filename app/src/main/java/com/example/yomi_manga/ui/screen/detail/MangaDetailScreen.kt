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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
                }
            )
        },
        snackbarHost = {
             // Custom Snackbar handling if needed, or stick to Scaffold's default if we passed SnackbarHostState
        }
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
                LazyColumn(
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
                    
                    item {
                        manga.genres?.let { genres ->
                            if (genres.isNotEmpty()) {
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
                                    genres.forEach { genre ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { },
                                            label = { Text(genre) }
                                        )
                                    }
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

                            // Nút Tải tất cả
                            IconButton(
                                onClick = {
                                    val allChapters = manga.chapters?.flatMap { it.items } ?: emptyList()
                                    viewModel.downloadAllChapters(allChapters)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadForOffline,
                                    contentDescription = "Tải tất cả",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    manga.chapters?.flatMap { server ->
                        server.items
                    }?.forEach { chapterData ->
                        item {
                            val isDownloaded = uiState.downloadedChapters.contains(chapterData.chapterApiData)
                            val isDownloading = uiState.downloadingChapterIds.contains(chapterData.chapterApiData)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onChapterClick(chapterData.chapterApiData)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chapter " + chapterData.chapterName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (!isDownloaded) {
                                                    viewModel.downloadChapter(
                                                        mangaId = manga.id ?: "",
                                                        chapterApiData = chapterData.chapterApiData,
                                                        chapterTitle = chapterData.chapterName,
                                                        chapterNumber = chapterData.chapterName.toFloatOrNull() ?: 0f
                                                    )
                                                }
                                            },
                                            enabled = !isDownloaded
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
