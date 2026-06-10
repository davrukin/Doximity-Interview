package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.repository.PriceRepository
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Live quotes for whatever is currently on the watchlist, keyed by symbol.
 *
 * Re-subscribes the price stream whenever watchlist membership changes.
 */
class ObserveQuotesUseCase(
    private val watchlistRepository: WatchlistRepository,
    private val priceRepository: PriceRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Map<String, Quote>> =
        watchlistRepository
            .observeWatchlist()
            .map { items ->
                items.map { item ->
                    item.instrument
                }
            }
            .distinctUntilChanged()
            .flatMapLatest { instruments ->
                priceRepository.observeQuotes(instruments)
            }
}
