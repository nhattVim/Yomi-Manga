package com.example.yomi_manga.ui.navigation

import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.yomi_manga.core.constants.AppConstants
import com.example.yomi_manga.ui.components.BottomNavBar
import com.example.yomi_manga.ui.screen.detail.MangaDetailScreen
import com.example.yomi_manga.ui.screen.explore.ExploreScreen
import com.example.yomi_manga.ui.screen.home.HomeScreen
import com.example.yomi_manga.ui.screen.library.LibraryScreen
import com.example.yomi_manga.ui.screen.login.LoginScreen
import com.example.yomi_manga.ui.screen.reader.ReaderScreen
import com.example.yomi_manga.ui.screen.settings.SettingsScreen
import com.example.yomi_manga.ui.screen.settings.StorageScreen
import com.example.yomi_manga.ui.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Storage : Screen("storage")
    object Detail : Screen("detail/{${AppConstants.NAV_ARG_MANGA_ID}}") {
        fun createRoute(mangaId: String) = "detail/$mangaId"
    }
    object Reader : Screen("reader/{${AppConstants.NAV_ARG_CHAPTER_ID}}?${AppConstants.NAV_ARG_MANGA_SLUG}={${AppConstants.NAV_ARG_MANGA_SLUG}}") {
        fun createRoute(chapterId: String, mangaSlug: String? = null) = 
            if (mangaSlug != null) {
                "reader/$chapterId?${AppConstants.NAV_ARG_MANGA_SLUG}=$mangaSlug"
            } else {
                "reader/$chapterId"
            }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val startDestination = if (authUiState.isAuthenticated) Screen.Home.route else Screen.Login.route
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    
    val showBottomNav = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Library.route,
        Screen.Settings.route
    )
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            MainScreenWrapper(
                showBottomNav = showBottomNav,
                currentRoute = currentRoute,
                navController = navController,
                authViewModel = authViewModel
            ) {
                HomeScreen(
                    authViewModel = authViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.Detail.createRoute(mangaId))
                    }
                )
            }
        }
        
        composable(Screen.Explore.route) {
            MainScreenWrapper(
                showBottomNav = showBottomNav,
                currentRoute = currentRoute,
                navController = navController,
                authViewModel = authViewModel
            ) {
                ExploreScreen(
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.Detail.createRoute(mangaId))
                    }
                )
            }
        }
        
        composable(Screen.Library.route) {
            MainScreenWrapper(
                showBottomNav = showBottomNav,
                currentRoute = currentRoute,
                navController = navController,
                authViewModel = authViewModel
            ) {
                LibraryScreen(
                    authViewModel = authViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.Detail.createRoute(mangaId))
                    }
                )
            }
        }
        
        composable(Screen.Settings.route) {
            MainScreenWrapper(
                showBottomNav = showBottomNav,
                currentRoute = currentRoute,
                navController = navController,
                authViewModel = authViewModel
            ) {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onStorageClick = {
                        navController.navigate(Screen.Storage.route)
                    }
                )
            }
        }

        composable(Screen.Storage.route) {
            StorageScreen(
                onBackClick = { navController.popBackStack() },
                onChapterClick = { chapterId, mangaId ->
                     val encodedUrl = Base64.encodeToString(chapterId.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                     navController.navigate(Screen.Reader.createRoute(encodedUrl, mangaId))
                }
            )
        }
        
        composable(Screen.Detail.route) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString(AppConstants.NAV_ARG_MANGA_ID) ?: ""
            MangaDetailScreen(
                mangaId = mangaId,
                onBackClick = { navController.popBackStack() },
                onChapterClick = { chapterUrl ->
                    val encodedUrl = Base64.encodeToString(chapterUrl.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    navController.navigate(Screen.Reader.createRoute(encodedUrl, mangaId))
                }
            )
        }
        
        composable(Screen.Reader.route) { backStackEntry ->
            val encodedChapterId = backStackEntry.arguments?.getString(AppConstants.NAV_ARG_CHAPTER_ID) ?: ""
            val mangaSlug = backStackEntry.arguments?.getString(AppConstants.NAV_ARG_MANGA_SLUG)
            val chapterId = try {
                String(Base64.decode(encodedChapterId, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                encodedChapterId
            }
            ReaderScreen(
                chapterId = chapterId,
                mangaSlug = mangaSlug,
                onBackClick = { navController.popBackStack() },
                onChapterChange = { newChapterUrl ->
                    val newEncodedUrl = Base64.encodeToString(newChapterUrl.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    navController.navigate(Screen.Reader.createRoute(newEncodedUrl, mangaSlug)) {
                        popUpTo(Screen.Reader.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreenWrapper(
    showBottomNav: Boolean,
    currentRoute: String,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        if (showBottomNav) {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
