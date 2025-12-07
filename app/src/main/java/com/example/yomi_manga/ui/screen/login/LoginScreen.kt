@file:Suppress("DEPRECATION")

package com.example.yomi_manga.ui.screen.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yomi_manga.core.constants.AppConstants
import com.example.yomi_manga.util.FirebaseConfig
import com.example.yomi_manga.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val webClientId = remember {
        FirebaseConfig.getWebClientId(context) 
            ?: FirebaseConfig.getDefaultWebClientId(context)
    }
    
    LaunchedEffect(webClientId) {
        android.util.Log.d(AppConstants.TAG_LOGIN, "Web Client ID: $webClientId")
        if (webClientId.contains("xxxxxxxxxxxxx")) {
            android.util.Log.w(AppConstants.TAG_LOGIN, "Web Client ID chưa được cấu hình đúng!")
        }
    }
    
    val gso = remember(webClientId) {
        try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        } catch (e: Exception) {
            android.util.Log.e("LoginScreen", "Error creating GoogleSignInOptions", e)
            null
        }
    }
    
    val googleSignInClient = remember(gso) { 
        gso?.let { GoogleSignIn.getClient(context, it) } 
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
            android.util.Log.d(AppConstants.TAG_LOGIN, "Activity result received")
        try {
            if (result.data == null) {
                android.util.Log.e(AppConstants.TAG_LOGIN, "Result data is null")
                viewModel.setError("Không nhận được dữ liệu từ Google Sign-In")
                return@rememberLauncherForActivityResult
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            android.util.Log.d(AppConstants.TAG_LOGIN, "Got GoogleSignIn task")
            
            handleSignInResult(task) { idToken ->
                android.util.Log.d(AppConstants.TAG_LOGIN, "Handle sign in result, idToken: ${idToken != null}")
                if (idToken != null) {
                    android.util.Log.d(AppConstants.TAG_LOGIN, "Calling signInWithGoogle")
                    viewModel.signInWithGoogle(idToken) { success, error ->
                        android.util.Log.d(AppConstants.TAG_LOGIN, "Sign in result: success=$success, error=$error")
                        if (!success) {
                            android.util.Log.e(AppConstants.TAG_LOGIN, "Sign in failed: $error")
                        } else {
                            android.util.Log.d(AppConstants.TAG_LOGIN, "Sign in successful!")
                        }
                    }
                } else {
                    android.util.Log.e(AppConstants.TAG_LOGIN, "idToken is null")
                    viewModel.setError("Không thể lấy thông tin đăng nhập. Vui lòng thử lại.")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(AppConstants.TAG_LOGIN, "Error during sign in", e)
            viewModel.setError("Lỗi: ${e.message ?: "Đăng nhập thất bại"}")
        }
    }
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Yomi Manga",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Đăng nhập để tiếp tục",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        if (webClientId.contains("xxxxxxxxxxxxx")) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cảnh báo: Web Client ID chưa được cấu hình!",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Vui lòng bật Google Sign-In trong Firebase Console và cập nhật Web Client ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Button(
            onClick = {
                if (googleSignInClient != null) {
                    googleSignInClient.signOut().addOnCompleteListener {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                } else {
                    viewModel.setError("Không thể khởi tạo Google Sign-In. Vui lòng kiểm tra cấu hình.")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isLoading && googleSignInClient != null
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Đăng nhập với Google",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

fun handleSignInResult(
    completedTask: Task<GoogleSignInAccount>,
    onResult: (String?) -> Unit
) {
    try {
        val account = completedTask.getResult(ApiException::class.java)
        val idToken = account?.idToken
        android.util.Log.d(AppConstants.TAG_LOGIN, "Got idToken: ${idToken != null}")
        if (idToken == null) {
            android.util.Log.e(AppConstants.TAG_LOGIN, "idToken is null")
        }
        onResult(idToken)
    } catch (e: ApiException) {
        android.util.Log.e(AppConstants.TAG_LOGIN, "Sign in failed: ${e.statusCode}", e)
        android.util.Log.e(AppConstants.TAG_LOGIN, "Error message: ${e.message}")
        onResult(null)
    } catch (e: Exception) {
        android.util.Log.e(AppConstants.TAG_LOGIN, "Unexpected error", e)
        onResult(null)
    }
}
