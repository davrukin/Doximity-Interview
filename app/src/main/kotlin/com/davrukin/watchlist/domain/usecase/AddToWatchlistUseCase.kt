package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.repository.WatchlistRepository

class AddToWatchlistUseCase(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(instrument: Instrument) {
        repository.add(instrument = instrument)
    }
}
