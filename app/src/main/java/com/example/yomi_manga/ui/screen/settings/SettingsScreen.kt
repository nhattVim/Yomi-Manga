package com.example.yomi_manga.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yomi_manga.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit,
    onStorageClick: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val user = authUiState.user
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Cài đặt") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (user != null) {
                            user.photoUrl?.let { photoUrl ->
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(64.dp)
                                )
                            } ?: Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName ?: "Người dùng",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = user.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Chưa đăng nhập",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
            
            val settingsItems = listOf(
                SettingsItem(
                    title = "Thông báo",
                    icon = Icons.Default.Notifications,
                    onClick = { }
                ),
                SettingsItem(
                    title = "Chất lượng hình ảnh",
                    icon = Icons.Default.PhotoCamera,
                    onClick = { }
                ),
                SettingsItem(
                    title = "Lưu trữ",
                    icon = Icons.Default.Storage,
                    onClick = onStorageClick
                ),
                SettingsItem(
                    title = "Về ứng dụng",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            )
            
            items(settingsItems) { item ->
                SettingsItemRow(item = item)
            }
            
            if (user != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            authViewModel.signOut()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất")
                    }
                }
            }
        }
    }
}

data class SettingsItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsItemRow(item: SettingsItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
