package com.davrukin.watchlist.common

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] for suspending work: cancellation is rethrown instead of being captured, so
 * coroutine cancellation propagates correctly.
 */
suspend fun <T> resultOf(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
