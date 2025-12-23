package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.repository.AuthRepository
import com.example.yomi_manga.data.repository.UserRepository
import com.example.yomi_manga.di.AppContainer
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val requiresReauth: Boolean = false
) {
    companion object {
        fun initial() = AuthUiState()
    }
}

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
    private val userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    fun checkAuthState() {
        viewModelScope.launch {
            authRepository.getCurrentUser()?.let { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isAuthenticated = true
                )
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    user = null,
                    isAuthenticated = false
                )
            }
        }
    }
    
    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        isAuthenticated = true,
                        error = null,
                        requiresReauth = false
                    )
                    onResult(true, null)
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message,
                        isAuthenticated = false
                    )
                    onResult(false, appError.message)
                }
            )
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                onResult(false, "Không tìm thấy thông tin người dùng")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 1. Xóa dữ liệu Firestore trước
            userRepository.deleteUser(user.uid).fold(
                onSuccess = {
                    // 2. Sau đó xóa tài khoản Auth
                    authRepository.deleteAccount().fold(
                        onSuccess = {
                            _uiState.value = AuthUiState()
                            onResult(true, null)
                        },
                        onFailure = { throwable ->
                            handleDeleteFailure(throwable, onResult)
                        }
                    )
                },
                onFailure = { throwable ->
                    // Vẫn cố gắng xóa tài khoản Auth kể cả khi xóa Firestore lỗi
                    authRepository.deleteAccount().fold(
                        onSuccess = {
                            _uiState.value = AuthUiState()
                            onResult(true, null)
                        },
                        onFailure = { innerThrowable ->
                            handleDeleteFailure(innerThrowable, onResult)
                        }
                    )
                }
            )
        }
    }

    private fun handleDeleteFailure(throwable: Throwable, onResult: (Boolean, String?) -> Unit) {
        if (throwable is FirebaseAuthRecentLoginRequiredException) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                requiresReauth = true,
                error = "Vui lòng xác thực lại để xoá tài khoản"
            )
            onResult(false, "REQUIRES_REAUTH")
        } else {
            val appError = AppError.fromThrowable(throwable)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = appError.message
            )
            onResult(false, appError.message)
        }
    }

    fun reauthenticateAndDelete(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.reauthenticate(idToken).fold(
                onSuccess = {
                    deleteAccount(onResult)
                },
                onFailure = { throwable ->
                    val appError = AppError.fromThrowable(throwable)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = appError.message
                    )
                    onResult(false, appError.message)
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(error = errorMessage)
    }

    fun resetReauth() {
        _uiState.value = _uiState.value.copy(requiresReauth = false)
    }
}
