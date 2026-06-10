package com.davrukin.watchlist.data.demo

import com.davrukin.watchlist.data.stream.PriceStreamEvent
import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.random.Random

class DemoMarketDataSourceTest {
    private val source =
        DemoMarketDataSource(
            catalog = DemoInstrumentCatalog(),
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            random = Random(seed = 1),
            priceStream = NoOpStream(),
        )

    @Test
    fun `search matches symbol and description case-insensitively`() =
        runTest {
            val bySymbol = source.search(query = "btc").getOrThrow()
            assertEquals(listOf("BINANCE:BTCUSDT"), bySymbol.map { it.symbol })

            val byDescription = source.search(query = "apple").getOrThrow()
            assertEquals(listOf("AAPL"), byDescription.map { it.symbol })
        }

    @Test
    fun `blank search returns no results`() =
        runTest {
            assertTrue(source.search(query = "  ").getOrThrow().isEmpty())
        }

    @Test
    fun `snapshot stays within one percent of the base price`() =
        runTest {
            val aapl = source.search(query = "AAPL").getOrThrow().single()

            val quote = requireNotNull(source.quoteSnapshot(instrument = aapl).getOrThrow())

            assertTrue(abs(quote.price - 228.40) <= 228.40 * 0.01)
            assertEquals(Instant.EPOCH, quote.lastUpdated)
        }

    @Test
    fun `snapshot for an unknown instrument is null`() =
        runTest {
            val unknown =
                Instrument(
                    symbol = "ZZZZ",
                    displaySymbol = "ZZZZ",
                    description = "",
                    type = InstrumentType.STOCK,
                )

            assertNull(source.quoteSnapshot(instrument = unknown).getOrThrow())
        }

    private class NoOpStream : PriceStreamSource {
        override fun events(symbols: Flow<Set<String>>): Flow<PriceStreamEvent> = emptyFlow()
    }
}
