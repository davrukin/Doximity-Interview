package com.davrukin.watchlist.data

import app.cash.turbine.test
import com.davrukin.watchlist.data.local.WatchlistDao
import com.davrukin.watchlist.data.local.WatchlistItemEntity
import com.davrukin.watchlist.data.stream.PriceStreamEvent
import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.model.PriceTick
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PriceRepositoryImplTest {
    private val aapl =
        Instrument(
            symbol = "AAPL",
            displaySymbol = "AAPL",
            description = "APPLE INC",
            type = InstrumentType.STOCK,
        )

    @Test
    fun `emits snapshots then merges ticks with derived day change`() {
        runTest {
            val fixture: Fixture = Fixture(scope = this)
            fixture.liveSource.snapshots = mapOf("AAPL" to quote(price = 100.0, change = 2.0))

            fixture.repository.observeQuotes(instruments = listOf(aapl)).test {
                assertEquals(100.0, requireNotNull(value = awaitItem()["AAPL"]).price, 0.0)
                runCurrent()

                fixture.liveStream.emit(
                    event = PriceStreamEvent.Ticks(
                        ticks = listOf(PriceTick(symbol = "AAPL", price = 101.0, timestamp = Instant.EPOCH)),
                    ),
                )
                val updated: Quote = requireNotNull(value = awaitItem()["AAPL"])
                assertEquals(101.0, updated.price, 0.0)
                // Previous close = 100 - 2 = 98, so a 101 tick is a +3 day change.
                assertEquals(3.0, updated.change, 1e-9)
                assertEquals(3.0 / 98.0 * 100, updated.percentChange, 1e-9)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `refetches snapshots on reconnect`() =
        runTest {
            val fixture = Fixture(scope = this)
            fixture.liveSource.snapshots = mapOf("AAPL" to quote(price = 100.0, change = 2.0))

            fixture.repository.observeQuotes(instruments = listOf(aapl)).test {
                awaitItem()
                runCurrent()

                fixture.liveStream.emit(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED))
                runCurrent()
                expectNoEvents()

                fixture.liveSource.snapshots = mapOf("AAPL" to quote(price = 110.0, change = 12.0))
                fixture.liveStream.emit(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED))
                assertEquals(110.0, requireNotNull(awaitItem()["AAPL"]).price, 0.0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `persists fresh quotes in live mode only`() =
        runTest {
            val liveFixture = Fixture(scope = this, mode = MarketDataMode.LIVE)
            liveFixture.liveSource.snapshots = mapOf("AAPL" to quote(price = 100.0, change = 2.0))
            liveFixture.repository.observeQuotes(instruments = listOf(aapl)).test {
                awaitItem()
                advanceTimeBy(6.seconds)
                runCurrent()
                assertTrue(liveFixture.dao.updatedSymbols.contains("AAPL"))
                cancelAndIgnoreRemainingEvents()
            }

            val demoFixture = Fixture(scope = this, mode = MarketDataMode.DEMO)
            demoFixture.demoSource.snapshots = mapOf("AAPL" to quote(price = 100.0, change = 2.0))
            demoFixture.repository.observeQuotes(instruments = listOf(aapl)).test {
                awaitItem()
                advanceTimeBy(6.seconds)
                runCurrent()
                assertTrue(demoFixture.dao.updatedSymbols.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits empty map for an empty watchlist`() =
        runTest {
            val fixture = Fixture(scope = this)

            fixture.repository.observeQuotes(instruments = emptyList()).test {
                assertEquals(emptyMap<String, Quote>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun quote(
        price: Double,
        change: Double,
    ): Quote {
        return Quote(
            price = price,
            change = change,
            percentChange = Double.NaN,
            lastUpdated = Instant.EPOCH,
            isStale = false,
        )
    }

    private class Fixture(
        scope: kotlinx.coroutines.test.TestScope,
        mode: MarketDataMode = MarketDataMode.LIVE,
    ) {
        val liveStream = FakeStreamSource()
        val demoStream = FakeStreamSource()
        val liveSource = FakeMarketDataSource(priceStream = liveStream)
        val demoSource = FakeMarketDataSource(priceStream = demoStream)
        val dao = FakeWatchlistDao()
        val repository =
            PriceRepositoryImpl(
                selector =
                    MarketDataSelector(
                        modeRepository = FakeModeRepository(initial = mode),
                        live = liveSource,
                        demo = demoSource,
                    ),
                modeRepository = FakeModeRepository(initial = mode),
                dao = dao,
                appScope = scope.backgroundScope,
            )
    }

    private class FakeStreamSource : PriceStreamSource {
        private val sharedEvents = MutableSharedFlow<PriceStreamEvent>(extraBufferCapacity = 64)

        override fun events(symbols: Flow<Set<String>>): Flow<PriceStreamEvent> = sharedEvents

        suspend fun emit(event: PriceStreamEvent) {
            sharedEvents.emit(event)
        }
    }

    private class FakeMarketDataSource(
        override val priceStream: PriceStreamSource,
    ) : MarketDataSource {
        var snapshots: Map<String, Quote?> = emptyMap()

        override suspend fun search(query: String): Result<List<Instrument>> = Result.success(emptyList())

        override suspend fun quoteSnapshot(instrument: Instrument): Result<Quote?> =
            Result.success(snapshots[instrument.symbol])
    }

    private class FakeModeRepository(
        initial: MarketDataMode,
    ) : MarketDataModeRepository {
        override val isLiveAvailable: Boolean = true
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

    private class FakeWatchlistDao : WatchlistDao {
        val updatedSymbols = mutableListOf<String>()
        private val items = MutableStateFlow(emptyList<WatchlistItemEntity>())

        override fun observeAll(): Flow<List<WatchlistItemEntity>> = items

        override suspend fun insert(entity: WatchlistItemEntity) {
            items.update { current ->
                if (current.any { it.symbol == entity.symbol }) current else current + entity
            }
        }

        override suspend fun delete(symbol: String) {
            items.update { current -> current.filterNot { it.symbol == symbol } }
        }

        override suspend fun updateQuote(
            symbol: String,
            price: Double,
            change: Double,
            percentChange: Double,
            updatedAtEpochMillis: Long,
        ) {
            updatedSymbols += symbol
        }
    }
}
