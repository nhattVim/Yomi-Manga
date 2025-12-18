package com.example.yomi_manga.ui.screen.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yomi_manga.data.model.Category
import com.example.yomi_manga.ui.components.MangaCard
import com.example.yomi_manga.ui.viewmodel.MangaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MangaViewModel = viewModel(),
    onMangaClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Khám phá") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        val staticTabs = remember {
            listOf(
                Category(id = "latest", name = "Mới nhất", slug = "truyen-moi")
            )
        }

        val tabs = remember(uiState.categories) {
            staticTabs + uiState.categories
        }

        val pagerState = rememberPagerState(pageCount = { tabs.size })

        // Tải dữ liệu nếu trang hiện tại chưa có dữ liệu
        LaunchedEffect(pagerState.currentPage, tabs) {
            if (tabs.isNotEmpty()) {
                val category = tabs[pagerState.currentPage]
                val currentCategoryData = uiState.categoriesData[category.slug]
                
                if (currentCategoryData == null || currentCategoryData.mangaList.isEmpty()) {
                    when (category.id) {
                        "latest" -> viewModel.loadMangaList(category.slug)
                        else -> viewModel.loadMangaByCategory(category.slug)
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(category.name) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val category = tabs.getOrNull(page)
            val categoryData = category?.let { uiState.categoriesData[it.slug] }

            when {
                categoryData?.isLoading == true && categoryData.mangaList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                categoryData?.error != null && categoryData.mangaList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = "Lỗi: ${categoryData.error}", color = MaterialTheme.colorScheme.error)
                            Button(onClick = {
                                category.let {
                                    if (it.id == "latest") viewModel.loadMangaList(it.slug)
                                    else viewModel.loadMangaByCategory(it.slug)
                                }
                            }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
                categoryData != null && categoryData.mangaList.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryData.mangaList) { manga ->
                            MangaCard(
                                manga = manga,
                                onClick = { onMangaClick(manga.slug) }
                            )
                        }

                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        // Cần cập nhật ViewModel để load page cho đúng slug
                                        viewModel.loadMangaByCategory(category.slug, categoryData.currentPage - 1)
                                    },
                                    enabled = categoryData.currentPage > 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Page")
                                }

                                Text(
                                    text = "Trang ${categoryData.currentPage}",
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                IconButton(onClick = { 
                                    if (category.id == "latest") {
                                        viewModel.loadMangaList(category.slug, categoryData.currentPage + 1)
                                    } else {
                                        viewModel.loadMangaByCategory(category.slug, categoryData.currentPage + 1)
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Page")
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
