package com.example.yomi_manga.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yomi_manga.ui.viewmodel.AuthViewModel
import com.example.yomi_manga.ui.viewmodel.SettingsViewModel
import com.example.yomi_manga.util.FirebaseConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onLogout: () -> Unit,
    onStorageClick: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    
    val user = authUiState.user
    val themeMode = settingsUiState.themeMode
    val context = LocalContext.current
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Google Sign-In Setup for Re-auth ---
    val webClientId = remember {
        FirebaseConfig.getWebClientId(context) ?: FirebaseConfig.getDefaultWebClientId(context)
    }
    
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }
    
    val reauthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                authViewModel.reauthenticateAndDelete(idToken) { success, _ ->
                    if (success) onLogout()
                }
            }
        } catch (e: Exception) {
            authViewModel.setError("Xác thực thất bại: ${e.message}")
        }
    }
    // ----------------------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsGroup(title = "Hồ sơ") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (user != null) {
                            AsyncImage(
                                model = user.photoUrl ?: Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = user.displayName ?: "Người dùng",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Chưa đăng nhập",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Ứng dụng") {
                    SettingsItemRow(
                        title = "Giao diện",
                        subtitle = when(themeMode) {
                            "dark" -> "Tối"
                            "light" -> "Sáng"
                            else -> "Theo hệ thống"
                        },
                        icon = Icons.Default.Palette,
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsItemRow(
                        title = "Quản lý tải xuống",
                        icon = Icons.Default.Download,
                        onClick = onStorageClick
                    )
                }
            }

            if (user != null) {
                item {
                    SettingsGroup(title = "Tài khoản") {
                        SettingsItemRow(
                            title = "Đăng xuất",
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                authViewModel.signOut()
                                onLogout()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        SettingsItemRow(
                            title = "Xoá tài khoản",
                            icon = Icons.Default.DeleteForever,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Chọn giao diện") },
            text = {
                Column {
                    ThemeOption("light", "Sáng", themeMode == "light") {
                        settingsViewModel.setThemeMode("light")
                        showThemeDialog = false
                    }
                    ThemeOption("dark", "Tối", themeMode == "dark") {
                        settingsViewModel.setThemeMode("dark")
                        showThemeDialog = false
                    }
                    ThemeOption("system", "Theo hệ thống", themeMode == "system") {
                        settingsViewModel.setThemeMode("system")
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Xoá tài khoản?") },
            text = { 
                Text("Hành động này không thể hoàn tác. Mọi dữ liệu cá nhân của bạn sẽ bị xoá khỏi hệ thống.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.deleteAccount { success, error ->
                            if (success) {
                                onLogout()
                            } else if (error == "REQUIRES_REAUTH") {
                                // Kích hoạt ngay popup Google Sign-In để re-auth
                                googleSignInClient.signOut().addOnCompleteListener {
                                    reauthLauncher.launch(googleSignInClient.signInIntent)
                                }
                            }
                        }
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xoá vĩnh viễn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    LaunchedEffect(authUiState.error) {
        if (authUiState.error != null) {
            snackbarHostState.showSnackbar(authUiState.error!!)
            authViewModel.clearError()
        }
    }

    if (authUiState.isLoading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Đang xử lý...")
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThemeOption(
    mode: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
