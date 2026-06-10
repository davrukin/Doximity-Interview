package com.davrukin.watchlist.data

import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketDataModeRepositoryImpl(
    apiKey: String,
) : MarketDataModeRepository {
    override val isLiveAvailable: Boolean = apiKey.isNotBlank()

    private val mutableMode =
        MutableStateFlow(
            value = if (isLiveAvailable) {
                MarketDataMode.LIVE
            } else {
                MarketDataMode.DEMO
            },
        )

    override val mode: StateFlow<MarketDataMode> = mutableMode.asStateFlow()

    override fun toggle() {
        if (!isLiveAvailable) {
            return
        }
        mutableMode.update { current ->
            when (current) {
                MarketDataMode.LIVE -> MarketDataMode.DEMO
                MarketDataMode.DEMO -> MarketDataMode.LIVE
            }
        }
    }
}
