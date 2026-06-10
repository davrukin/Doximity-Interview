package com.davrukin.watchlist.data

import com.davrukin.watchlist.data.local.WatchlistDao
import com.davrukin.watchlist.data.stream.PriceStreamEvent
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.model.PriceTick
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import com.davrukin.watchlist.domain.repository.PriceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Merges REST snapshots with streamed ticks into per-symbol quotes.
 *
 * One socket is shared across all collectors via [shareIn] with `WhileSubscribed`: it connects
 * when the UI starts observing, survives configuration changes, and is torn down shortly after
 * the last collector stops — the lifecycle a ViewModel would otherwise own.
 *
 * On every reconnect the snapshots are re-fetched, covering ticks missed while disconnected.
 * Fresh quotes are persisted (throttled) so a last-known price is available on next launch;
 * demo-mode prices are never persisted.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PriceRepositoryImpl(
    private val selector: MarketDataSelector,
    private val modeRepository: MarketDataModeRepository,
    private val dao: WatchlistDao,
    appScope: CoroutineScope,
) : PriceRepository {
    private val watchedSymbols = MutableStateFlow(value = emptySet<String>())
    private val refreshRequests = MutableSharedFlow<Unit>()

    private val events: SharedFlow<PriceStreamEvent> =
        selector
            .observe()
            .flatMapLatest { source ->
                source.priceStream.events(symbols = watchedSymbols)
            }.shareIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = SOCKET_LINGER_MILLIS),
                replay = 0,
            )

    private val connectionState: StateFlow<ConnectionState> =
        events
            .filterIsInstance<PriceStreamEvent.ConnectionChanged>()
            .map { connectionChanged ->
                connectionChanged.state
            }.stateIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = SOCKET_LINGER_MILLIS),
                initialValue = ConnectionState.CONNECTING,
            )

    override fun observeConnectionState(): Flow<ConnectionState> = connectionState

    override suspend fun refreshQuotes() {
        refreshRequests.emit(value = Unit)
    }

    override fun observeQuotes(instruments: List<Instrument>): Flow<Map<String, Quote>> =
        modeRepository.mode.flatMapLatest { mode: MarketDataMode ->
            quotes(
                source = selector.sourceFor(mode = mode),
                persistFreshQuotes = mode == MarketDataMode.LIVE,
                instruments = instruments,
            )
        }

    private fun quotes(
        source: MarketDataSource,
        persistFreshQuotes: Boolean,
        instruments: List<Instrument>,
    ): Flow<Map<String, Quote>> {
        return channelFlow {
            val symbols: Set<String> = instruments.map { it.symbol }.toSet()
            watchedSymbols.value = symbols
            if (instruments.isEmpty()) {
                send(element = emptyMap())
                return@channelFlow
            }

            val quotesState: MutableStateFlow<Map<String, Quote>> = MutableStateFlow(value = emptyMap<String, Quote>())
            launch {
                quotesState
                    .drop(count = 1)
                    .collect { quotes: Map<String, Quote> ->
                        send(element = quotes)
                    }
            }
            if (persistFreshQuotes) {
                launch {
                    quotesState
                        .drop(count = 1)
                        .sample(period = PERSIST_INTERVAL)
                        .collect { quotes: Map<String, Quote> ->
                            persist(quotes = quotes)
                        }
                }
            }

            // Previous-close baselines derived from snapshots let ticks carry day change too.
            val previousCloses: MutableMap<String, Double> = mutableMapOf<String, Double>()
            val snapshots: Map<String, Quote> = fetchSnapshots(source = source, instruments = instruments)
            recordPreviousCloses(
                snapshots = snapshots,
                into = previousCloses,
            )
            quotesState.value = snapshots

            launch {
                refreshRequests.collect {
                    val fresh: Map<String, Quote> =
                        fetchSnapshots(
                            source = source,
                            instruments = instruments,
                        )
                    recordPreviousCloses(snapshots = fresh, into = previousCloses)
                    quotesState.update { current: Map<String, Quote> ->
                        current + fresh
                    }
                }
            }

            var connectedOnce: Boolean = false
            events.collect { event: PriceStreamEvent ->
                when (event) {
                    is PriceStreamEvent.Ticks -> {
                        quotesState.update { current: Map<String, Quote> ->
                            applyTicks(
                                current = current,
                                ticks = event.ticks,
                                previousCloses = previousCloses,
                                symbols = symbols,
                            )
                        }
                    }

                    is PriceStreamEvent.ConnectionChanged -> {
                        if (event.state != ConnectionState.CONNECTED) {
                            return@collect
                        }
                        if (!connectedOnce) {
                            connectedOnce = true
                            return@collect
                        }
                        val fresh: Map<String, Quote> =
                            fetchSnapshots(
                                source = source,
                                instruments = instruments,
                            )
                        recordPreviousCloses(
                            snapshots = fresh,
                            into = previousCloses,
                        )
                        quotesState.update { current: Map<String, Quote> ->
                            current + fresh
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchSnapshots(
        source: MarketDataSource,
        instruments: List<Instrument>,
    ): Map<String, Quote> =
        coroutineScope {
            instruments
                .map { instrument: Instrument ->
                    async {
                        instrument.symbol to source.quoteSnapshot(instrument = instrument).getOrNull()
                    }
                }.awaitAll()
                .mapNotNull { (symbol: String, quote: Quote?) ->
                    quote?.let { quoteValue: Quote ->
                        symbol to quoteValue
                    }
                }.toMap()
        }

    private fun recordPreviousCloses(
        snapshots: Map<String, Quote>,
        into: MutableMap<String, Double>,
    ) {
        snapshots.forEach { (symbol: String, quote: Quote) ->
            if (quote.change.isNaN()) {
                return@forEach
            }
            into[symbol] = quote.price - quote.change
        }
    }

    private fun applyTicks(
        current: Map<String, Quote>,
        ticks: List<PriceTick>,
        previousCloses: Map<String, Double>,
        symbols: Set<String>,
    ): Map<String, Quote> {
        val updated: MutableMap<String, Quote> = current.toMutableMap()
        ticks.forEach { tick: PriceTick ->
            if (tick.symbol !in symbols) {
                return@forEach
            }
            val previousClose: Double? = previousCloses[tick.symbol]?.takeIf { it != 0.0 }
            val change: Double =
                if (previousClose != null) {
                    tick.price - previousClose
                } else {
                    Double.NaN
                }
            val percentChange: Double =
                if (previousClose != null && previousClose != 0.0) {
                    change / previousClose * 100
                } else {
                    Double.NaN
                }

            updated[tick.symbol] =
                Quote(
                    price = tick.price,
                    change = change,
                    percentChange = percentChange,
                    lastUpdated = tick.timestamp,
                    isStale = false,
                )
        }
        return updated
    }

    private suspend fun persist(quotes: Map<String, Quote>) {
        quotes.forEach { (symbol: String, quote: Quote) ->
            if (quote.price.isNaN()) {
                return@forEach
            }
            dao.updateQuote(
                symbol = symbol,
                price = quote.price,
                change = quote.change.takeUnless { it.isNaN() },
                percentChange = quote.percentChange.takeUnless { it.isNaN() },
                updatedAtEpochMillis = quote.lastUpdated.toEpochMilli(),
            )
        }
    }

    companion object {
        private const val SOCKET_LINGER_MILLIS = 5_000L
        private val PERSIST_INTERVAL = 5.seconds
    }
}
