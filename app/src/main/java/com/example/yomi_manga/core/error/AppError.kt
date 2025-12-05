package com.example.yomi_manga.core.error

/**
 * Sealed class cho các loại lỗi trong app
 */
sealed class AppError(
    val message: String,
    val errorCause: Throwable? = null
) {
    data class NetworkError(val throwable: Throwable?) : AppError(
        message = "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet.",
        errorCause = throwable
    )
    
    data class AuthError(val throwable: Throwable?) : AppError(
        message = "Lỗi xác thực. Vui lòng thử lại.",
        errorCause = throwable
    )
    
    data class FirestoreError(val throwable: Throwable?) : AppError(
        message = "Lỗi lưu trữ dữ liệu.",
        errorCause = throwable
    )
    
    data class UnknownError(val throwable: Throwable?) : AppError(
        message = "Đã xảy ra lỗi không xác định.",
        errorCause = throwable
    )
    
    companion object {
        fun fromThrowable(throwable: Throwable): AppError {
            return when (throwable) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> NetworkError(throwable)
                is com.google.firebase.auth.FirebaseAuthException -> AuthError(throwable)
                is com.google.firebase.firestore.FirebaseFirestoreException -> FirestoreError(throwable)
                else -> UnknownError(throwable)
            }
        }
    }
}

