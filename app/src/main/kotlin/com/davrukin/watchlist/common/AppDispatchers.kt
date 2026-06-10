package com.davrukin.watchlist.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Standard app dispatchers.
 */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)
