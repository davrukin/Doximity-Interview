package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveMarketDataModeUseCase(
    private val repository: MarketDataModeRepository,
) {
    val isLiveAvailable: Boolean
        get() = repository.isLiveAvailable

    operator fun invoke(): StateFlow<MarketDataMode> = repository.mode
}
