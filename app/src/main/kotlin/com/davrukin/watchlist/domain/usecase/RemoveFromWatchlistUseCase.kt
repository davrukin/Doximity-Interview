package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.repository.WatchlistRepository

class RemoveFromWatchlistUseCase(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(symbol: String) {
        repository.remove(symbol = symbol)
    }
}
