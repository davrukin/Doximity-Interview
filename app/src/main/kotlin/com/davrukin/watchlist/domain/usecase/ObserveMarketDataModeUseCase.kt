package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveMarketDataModeUseCase(
    private val repository: MarketDataModeRepository,
) {
    val isLiveAvailable: Boolean
        get() {
            return repository.isLiveAvailable
        }

    operator fun invoke(): Flow<MarketDataMode> {
        return repository.mode
    }
}
