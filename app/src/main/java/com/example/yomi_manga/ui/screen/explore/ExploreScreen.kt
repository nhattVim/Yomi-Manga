package com.example.yomi_manga.ui.screen.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yomi_manga.data.model.Category
import com.example.yomi_manga.ui.components.MangaCard
import com.example.yomi_manga.ui.viewmodel.MangaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MangaViewModel = viewModel(),
    onMangaClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCategories()
        viewModel.loadMangaList(slug = "truyen-moi")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            var selectedTabIndex by remember { mutableIntStateOf(0) }

            val staticTabs = remember {
                listOf(
                    Category(id = "latest", name = "Mới nhất", slug = "truyen-moi")
                )
            }

            val tabs = remember(uiState.categories) {
                staticTabs + uiState.categories
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            when (category.id) {
                                "latest" -> viewModel.loadMangaList(category.slug)
                                else -> viewModel.loadMangaByCategory(category.slug)
                            }
                        },
                        text = { Text(category.name) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                            Button(onClick = {
                                if (selectedTabIndex < tabs.size) {
                                    val category = tabs[selectedTabIndex]
                                    when (category.id) {
                                        "latest" -> viewModel.loadMangaList(category.slug)
                                        else -> viewModel.loadMangaByCategory(category.slug)
                                    }
                                } else {
                                    viewModel.loadMangaList(slug = "truyen-moi")
                                }
                            }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.mangaList) { manga ->
                            MangaCard(
                                manga = manga,
                                onClick = { onMangaClick(manga.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}
