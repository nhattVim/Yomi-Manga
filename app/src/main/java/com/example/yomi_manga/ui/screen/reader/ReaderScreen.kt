package com.example.yomi_manga.ui.screen.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.yomi_manga.data.model.ChapterData
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.di.AppContainer
import com.example.yomi_manga.ui.viewmodel.MangaViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    mangaSlug: String? = null,
    onBackClick: () -> Unit,
    onChapterChange: (String) -> Unit = {},
    viewModel: MangaViewModel = viewModel()
) {
    val repository = AppContainer.mangaRepository
    val context = LocalContext.current
    val downloadRepository = remember { AppContainer.provideDownloadRepository(context) }
    val uiState by viewModel.uiState.collectAsState()

    val images = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showTopBar by remember { mutableStateOf(true) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var manga by remember { mutableStateOf<Manga?>(null) }
    var showChapterList by remember { mutableStateOf(false) }
    var currentChapterUrl by remember { mutableStateOf(chapterId) }
    var isDownloaded by remember { mutableStateOf(false) }

    val downloadedChapters = remember { mutableStateListOf<ChapterData>() }

    val allChapters = remember(manga, downloadedChapters.size) {
        val remoteChapters = manga?.chapters?.flatMap { server -> server.items } ?: emptyList()
        val list = remoteChapters.ifEmpty {
            downloadedChapters.toList()
        }
        // Always sort ascending for consistent navigation (1 -> 6 -> 9)
        list.sortedBy { it.chapterName.toDoubleOrNull() ?: 0.0 }
    }

    val currentChapterIndex = remember(allChapters, currentChapterUrl) {
        allChapters.indexOfFirst { it.chapterApiData == currentChapterUrl }
    }

    val hasPreviousChapter = currentChapterIndex > 0
    val hasNextChapter = currentChapterIndex >= 0 && currentChapterIndex < allChapters.size - 1

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset ->
                val currentOffset = listState.firstVisibleItemIndex * 1000 + offset
                showTopBar = currentOffset <= previousScrollOffset || currentOffset < 100
                previousScrollOffset = currentOffset
            }
    }

    LaunchedEffect(mangaSlug) {
        if (mangaSlug != null) {
            // Try remote first
            repository.getMangaDetail(mangaSlug).fold(
                onSuccess = { mangaDetail ->
                    manga = mangaDetail
                    viewModel.loadMangaDetail(mangaSlug)
                },
                onFailure = {
                    // If remote fails, try to load downloaded chapters for navigation
                    coroutineScope.launch {
                        val downloaded = downloadRepository.getDownloadedChaptersList(mangaSlug)
                        if (downloaded.isNotEmpty()) {
                            downloadedChapters.clear()
                            downloadedChapters.addAll(downloaded.sortedBy { it.chapterNumber }.map {
                                ChapterData(
                                    filename = "",
                                    chapterName = it.chapterTitle.replace("Chapter ", ""),
                                    chapterTitle = it.chapterTitle,
                                    chapterApiData = it.chapterId
                                )
                            })
                        }
                    }
                }
            )

            // Always try to load downloaded chapters as fallback or for offline mode
            val downloaded = downloadRepository.getDownloadedChaptersList(mangaSlug)
            if (downloaded.isNotEmpty() && manga == null) {
                downloadedChapters.clear()
                downloadedChapters.addAll(downloaded.sortedBy { it.chapterNumber }.map {
                    ChapterData(
                        filename = "",
                        chapterName = it.chapterTitle.replace("Chapter ", ""),
                        chapterTitle = it.chapterTitle,
                        chapterApiData = it.chapterId
                    )
                })
            }
        }
    }

    LaunchedEffect(currentChapterUrl, manga) {
        isLoading = true
        error = null
        images.clear()

        // Save history if we have manga info
        if (manga != null) {
            val chapter = allChapters.find { it.chapterApiData == currentChapterUrl }
            if (chapter != null) {
                viewModel.saveReadingHistory(manga!!, chapter.chapterApiData, chapter.chapterName)
            }
        }

        // Check if downloaded
        val downloadedChapter = downloadRepository.getDownloadedChapter(currentChapterUrl)
        if (downloadedChapter != null) {
            isDownloaded = true
            images.addAll(downloadedChapter.imagePaths)
            isLoading = false
        } else {
            isDownloaded = false
            repository.getChapterPages(currentChapterUrl).fold(
                onSuccess = { imageList ->
                    images.addAll(imageList.map { it.imageFile })
                    isLoading = false
                },
                onFailure = { throwable ->
                    error = throwable.message
                    isLoading = false
                }
            )
        }
    }

    fun loadChapterImages() {
        coroutineScope.launch {
            isLoading = true
            error = null
            images.clear()

            val downloadedChapter = downloadRepository.getDownloadedChapter(currentChapterUrl)
            if (downloadedChapter != null) {
                isDownloaded = true
                images.addAll(downloadedChapter.imagePaths)
                isLoading = false
            } else {
                isDownloaded = false
                repository.getChapterPages(currentChapterUrl).fold(
                    onSuccess = { imageList ->
                        images.addAll(imageList.map { it.imageFile })
                        isLoading = false
                    },
                    onFailure = { throwable ->
                        error = throwable.message
                        isLoading = false
                    }
                )
            }
        }
    }

    fun navigateToChapter(chapterData: ChapterData) {
        currentChapterUrl = chapterData.chapterApiData
        onChapterChange(chapterData.chapterApiData)
        showChapterList = false
        coroutineScope.launch {
            listState.animateScrollToItem(0)
        }
    }

    fun navigateToPreviousChapter() {
        if (hasPreviousChapter) {
            val prevChapter = allChapters[currentChapterIndex - 1]
            navigateToChapter(prevChapter)
        }
    }

    fun navigateToNextChapter() {
        if (hasNextChapter) {
            val nextChapter = allChapters[currentChapterIndex + 1]
            navigateToChapter(nextChapter)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Lỗi: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { loadChapterImages() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có hình ảnh")
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = images,
                        key = { it }
                    ) { imageUrl ->
                        val model = if (isDownloaded) File(imageUrl) else imageUrl

                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.FillWidth
                        ) {
                            val state = painter.state
                            when (state) {
                                is AsyncImagePainter.State.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                is AsyncImagePainter.State.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Không thể tải ảnh",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                else -> {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showTopBar,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text("Đọc truyện") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showChapterList = true },
                        enabled = allChapters.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Danh sách chapter"
                        )
                    }
                    IconButton(
                        onClick = { navigateToPreviousChapter() },
                        enabled = hasPreviousChapter
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Chapter trước"
                        )
                    }
                    IconButton(
                        onClick = { navigateToNextChapter() },
                        enabled = hasNextChapter
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Chapter sau"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }

        if (showChapterList && allChapters.isNotEmpty()) {
            ChapterListBottomSheet(
                chapters = allChapters,
                currentChapterUrl = currentChapterUrl,
                onDismiss = { showChapterList = false },
                onChapterClick = { chapterData ->
                    navigateToChapter(chapterData)
                },
                isDescending = uiState.isChaptersDescending,
                onToggleSort = { viewModel.toggleChapterSort() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListBottomSheet(
    chapters: List<ChapterData>,
    currentChapterUrl: String,
    onDismiss: () -> Unit,
    onChapterClick: (ChapterData) -> Unit,
    isDescending: Boolean,
    onToggleSort: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayChapters = remember(chapters, isDescending) {
        if (isDescending) {
            chapters.sortedByDescending { it.chapterName.toDoubleOrNull() ?: 0.0 }
        } else {
            chapters.sortedBy { it.chapterName.toDoubleOrNull() ?: 0.0 }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh sách chương",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                IconButton(onClick = onToggleSort) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sắp xếp",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayChapters.size) { index ->
                    val chapter = displayChapters[index]
                    val isCurrentChapter = chapter.chapterApiData == currentChapterUrl

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(chapter) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentChapter) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Text(
                            text = "Chapter " + chapter.chapterName,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentChapter) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}
