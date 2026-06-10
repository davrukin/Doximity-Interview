package com.davrukin.watchlist.common

/**
 * Functional-style result type.
 */
sealed interface ResultOf<out T> {
    data class Success<out T>(val value: T) : ResultOf<T>

    data class Failure(val throwable: Throwable) : ResultOf<Nothing>

    fun getOrNull(): T? {
        return when (this) {
            is Success -> {
                this.value
            }

            is Failure -> {
                null
            }
        }
    }
}
