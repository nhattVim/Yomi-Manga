package com.example.yomi_manga.core.util

/**
 * Extension functions cho Result type
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (isSuccess) action(getOrNull()!!)
    return this
}

inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    if (isFailure) action(exceptionOrNull()!!)
    return this
}

