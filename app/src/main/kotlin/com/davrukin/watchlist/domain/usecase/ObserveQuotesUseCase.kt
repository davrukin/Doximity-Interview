package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.repository.PriceRepository
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveQuotesUseCase(
    private val watchlistRepository: WatchlistRepository,
    private val priceRepository: PriceRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Map<String, Quote>> {
        return watchlistRepository
            .observeWatchlist()
            .map { items ->
                items.map { item ->
                    item.instrument
                }
            }.distinctUntilChanged()
            .flatMapLatest { instruments ->
                if (instruments.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    priceRepository.observeQuotes(instruments = instruments)
                }
            }
    }
}
