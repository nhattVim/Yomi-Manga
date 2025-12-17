package com.example.yomi_manga.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.yomi_manga.ui.navigation.Screen

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        NavigationBar {
            val items = listOf(
                NavigationItem("Trang chủ", Screen.Home.route, Icons.Default.Home),
                NavigationItem("Khám phá", Screen.Explore.route, Icons.Default.Explore),
                NavigationItem("Tủ sách", Screen.Library.route, Icons.AutoMirrored.Filled.LibraryBooks),
                NavigationItem("Cài đặt", Screen.Settings.route, Icons.Default.Settings)
            )
            
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    icon = { 
                        Icon(
                            imageVector = item.icon, 
                            contentDescription = item.title
                        ) 
                    },
                    label = { Text(item.title) }
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
