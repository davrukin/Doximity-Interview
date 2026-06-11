package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow

class ObserveWatchlistUseCase(
    private val repository: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<WatchlistItem>> {
        return repository.observeWatchlist()
    }
}
