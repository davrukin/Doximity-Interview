package com.davrukin.watchlist.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatchers injected through Koin so production code never references [kotlinx.coroutines.Dispatchers]
 * directly and tests can substitute a virtual-time dispatcher everywhere.
 */
class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)
