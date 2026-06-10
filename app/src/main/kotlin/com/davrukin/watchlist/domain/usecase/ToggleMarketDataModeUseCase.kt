package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.repository.MarketDataModeRepository

class ToggleMarketDataModeUseCase(
    private val repository: MarketDataModeRepository,
) {
    operator fun invoke() = repository.toggle()
}
