package com.davrukin.watchlist.domain.repository

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<WatchlistItem>>

    suspend fun add(instrument: Instrument)

    suspend fun remove(symbol: String)
}
