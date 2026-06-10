package com.davrukin.watchlist.testing

import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.repository.InstrumentSearchRepository
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import com.davrukin.watchlist.domain.repository.PriceRepository
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

fun instrument(
    symbol: String,
    type: InstrumentType = InstrumentType.STOCK,
): Instrument =
    Instrument(
        symbol = symbol,
        displaySymbol = symbol,
        description = "$symbol description",
        type = type,
    )

fun quote(
    price: Double,
    change: Double? = null,
    percentChange: Double? = null,
    isStale: Boolean = false,
): Quote =
    Quote(
        price = price,
        change = change,
        percentChange = percentChange,
        lastUpdated = Instant.EPOCH,
        isStale = isStale,
    )

class FakeWatchlistRepository(
    initial: List<WatchlistItem> = emptyList(),
) : WatchlistRepository {
    val items = MutableStateFlow(initial)

    override fun observeWatchlist(): Flow<List<WatchlistItem>> = items

    override suspend fun add(instrument: Instrument) {
        items.update { current ->
            if (current.any { it.instrument.symbol == instrument.symbol }) {
                current
            } else {
                current + WatchlistItem(instrument = instrument, cachedQuote = null)
            }
        }
    }

    override suspend fun remove(symbol: String) {
        items.update { current -> current.filterNot { it.instrument.symbol == symbol } }
    }
}

class FakePriceRepository : PriceRepository {
    val quotes = MutableStateFlow(emptyMap<String, Quote>())
    val connectionState = MutableStateFlow(ConnectionState.CONNECTING)
    var refreshCount = 0
        private set

    override fun observeQuotes(instruments: List<Instrument>): Flow<Map<String, Quote>> = quotes

    override fun observeConnectionState(): Flow<ConnectionState> = connectionState

    override suspend fun refreshQuotes() {
        refreshCount++
    }
}

class FakeMarketDataModeRepository(
    initial: MarketDataMode = MarketDataMode.LIVE,
    override val isLiveAvailable: Boolean = true,
) : MarketDataModeRepository {
    private val mutableMode = MutableStateFlow(initial)
    override val mode: StateFlow<MarketDataMode> = mutableMode.asStateFlow()

    override fun toggle() {
        mutableMode.update {
            when (it) {
                MarketDataMode.LIVE -> MarketDataMode.DEMO
                MarketDataMode.DEMO -> MarketDataMode.LIVE
            }
        }
    }
}

class FakeInstrumentSearchRepository : InstrumentSearchRepository {
    var result: Result<List<Instrument>> = Result.success(emptyList())
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): Result<List<Instrument>> {
        queries += query
        return result
    }
}
