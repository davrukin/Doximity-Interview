package com.davrukin.watchlist.data

import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MarketDataSelector(
    private val modeRepository: MarketDataModeRepository,
    private val live: MarketDataSource,
    private val demo: MarketDataSource,
) {
    val current: MarketDataSource get() = sourceFor(mode = modeRepository.mode.value)

    fun sourceFor(mode: MarketDataMode): MarketDataSource =
        when (mode) {
            MarketDataMode.LIVE -> live
            MarketDataMode.DEMO -> demo
        }

    fun observe(): Flow<MarketDataSource> =
        modeRepository.mode
            .map { sourceFor(mode = it) }
            .distinctUntilChanged()
}
