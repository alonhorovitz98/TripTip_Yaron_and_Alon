package com.example.triptip_yaron_and_alon.util

/**
 * Sealed class representing the result of an operation.
 * Used for handling success, error, and loading states.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
    
    /**
     * Returns true if the result is Success
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Returns true if the result is Error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Returns true if the result is Loading
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Gets the data if Success, null otherwise
     */
    fun getOrNull(): T? = if (this is Success) data else null
    
    /**
     * Gets the error message if Error, null otherwise
     */
    fun getErrorMessage(): String? = when (this) {
        is Error -> message ?: exception.message
        else -> null
    }
}

