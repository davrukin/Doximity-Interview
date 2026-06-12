package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.repository.PriceRepository
import com.davrukin.watchlist.testing.FakePriceRepository
import com.davrukin.watchlist.testing.FakeWatchlistRepository
import com.davrukin.watchlist.testing.instrument
import com.davrukin.watchlist.testing.quote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveQuotesUseCaseTest {
    @Test
    fun observeQuotesDoesNotRestartWhenInstrumentsAreUnchanged() =
        runTest {
            val watchlistRepository = FakeWatchlistRepository()
            var observeQuotesCallCount = 0

            val delegate = FakePriceRepository()
            val priceRepository =
                object : PriceRepository by delegate {
                    override fun observeQuotes(instruments: List<Instrument>): Flow<Map<String, Quote>> {
                        observeQuotesCallCount++
                        return delegate.observeQuotes(instruments)
                    }
                }

            val useCase =
                ObserveQuotesUseCase(
                    watchlistRepository = watchlistRepository,
                    priceRepository = priceRepository,
                )

            // Start collecting from the use case
            val job = useCase().onEach {}.launchIn(scope = backgroundScope)
            runCurrent()

            // 1. Initially watchlist is empty, observeQuotes shouldn't be called
            assertEquals(0, observeQuotesCallCount)

            // 2. Add an instrument
            val instrumentA = instrument(symbol = "AAPL")
            watchlistRepository.items.value =
                listOf(
                    WatchlistItem(instrument = instrumentA, cachedQuote = null),
                )
            runCurrent() // Allow collector to run
            assertEquals(1, observeQuotesCallCount)

            // 3. Update watchlist row with a cached quote (simulating a database price update)
            // The instrument list itself (AAPL) is identical.
            watchlistRepository.items.value =
                listOf(
                    WatchlistItem(instrument = instrumentA, cachedQuote = quote(price = 240.0)),
                )
            runCurrent() // Allow collector to run
            // Verify distinctUntilChanged prevented a restart (call count is still 1)
            assertEquals(1, observeQuotesCallCount)

            // 4. Add a second instrument (changing the instrument list)
            val instrumentB = instrument(symbol = "BTC/USDT")
            watchlistRepository.items.value =
                listOf(
                    WatchlistItem(instrument = instrumentA, cachedQuote = quote(price = 240.0)),
                    WatchlistItem(instrument = instrumentB, cachedQuote = null),
                )
            runCurrent() // Allow collector to run
            // Verify quotes stream restarts (call count becomes 2)
            assertEquals(2, observeQuotesCallCount)

            job.cancel()
        }
}
