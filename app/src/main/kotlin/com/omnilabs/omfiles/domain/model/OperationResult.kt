package com.omnilabs.omfiles.domain.model

sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : OperationResult<Nothing>()
}
