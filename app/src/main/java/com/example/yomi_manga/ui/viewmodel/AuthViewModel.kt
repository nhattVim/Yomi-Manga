package com.example.yomi_manga.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yomi_manga.core.error.AppError
import com.example.yomi_manga.data.repository.AuthRepository
import com.example.yomi_manga.di.AppContainer
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
) {
    companion object {
        fun initial() = AuthUiState()
    }
}

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
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
                        error = null
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun setError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(error = errorMessage)
    }
}

