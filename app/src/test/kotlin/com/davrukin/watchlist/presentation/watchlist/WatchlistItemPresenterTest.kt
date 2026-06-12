package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.testing.instrument
import com.davrukin.watchlist.testing.quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WatchlistItemPresenterTest {
    @Test
    fun `presents items with live quotes override and connection status`() {
        runTest {
            val presenter = WatchlistItemPresenter()
            val item =
                WatchlistItem(
                    instrument = instrument(symbol = "AAPL"),
                    cachedQuote = quote(price = 100.0, change = 1.0, percentChange = 1.0, isStale = true),
                )

            val paramsFlow =
                MutableStateFlow(
                    WatchlistItemPresenter.Params(
                        item = item,
                        liveQuote = null,
                        connectionState = ConnectionState.CONNECTING,
                    ),
                )

            moleculeFlow(mode = RecompositionMode.Immediate) {
                val params by paramsFlow.collectAsState()
                presenter.present(params)
            }.test {
                // Initially, with no live quote and CONNECTING state,
                // it should fall back to cached quote and mark it stale
                val first = awaitItem()
                println("TEST 1 FIRST: symbol=${first.symbol}, price=${first.price}, isStale=${first.isStale}")
                assertEquals("AAPL", first.symbol)
                assertEquals("100.00", first.price)
                assertEquals("+1.00 (+1.00%)", first.change)
                assertTrue(first.isStale)

                // When connection state changes to CONNECTED, but still no live quote,
                // it should still be stale if the cached quote is stale
                paramsFlow.value =
                    WatchlistItemPresenter.Params(
                        item = item,
                        liveQuote = null,
                        connectionState = ConnectionState.CONNECTED,
                    )
                runCurrent()
                val second = awaitItem()
                println("TEST 1 SECOND: symbol=${second.symbol}, price=${second.price}, isStale=${second.isStale}")
                assertTrue(second.isStale)

                // When a live quote arrives, it overrides the cached quote
                // and is not stale anymore if connectionState is CONNECTED
                val live = quote(price = 105.0, change = 5.0, percentChange = 5.0, isStale = false)
                paramsFlow.value =
                    WatchlistItemPresenter.Params(
                        item = item,
                        liveQuote = live,
                        connectionState = ConnectionState.CONNECTED,
                    )
                runCurrent()
                val third = awaitItem()
                println("TEST 1 THIRD: symbol=${third.symbol}, price=${third.price}, isStale=${third.isStale}")
                assertEquals("105.00", third.price)
                assertEquals("+5.00 (+5.00%)", third.change)
                assertFalse(third.isStale)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `accumulates live prices in sparkline without consecutive duplicates`() {
        runTest {
            val presenter = WatchlistItemPresenter()
            val item =
                WatchlistItem(
                    instrument = instrument(symbol = "AAPL"),
                    cachedQuote = null,
                )

            val paramsFlow =
                MutableStateFlow(
                    WatchlistItemPresenter.Params(
                        item = item,
                        liveQuote = null,
                        connectionState = ConnectionState.CONNECTED,
                    ),
                )

            moleculeFlow(mode = RecompositionMode.Immediate) {
                val params by paramsFlow.collectAsState()
                presenter.present(params)
            }.test {
                // Initially sparkline is empty because live quote is null (NaN)
                val initial = awaitItem()
                println("TEST 2 INITIAL: sparkline=${initial.sparkline}")
                assertTrue(initial.sparkline.isEmpty())

                // Update with live quote 100.0
                paramsFlow.value =
                    paramsFlow.value.copy(
                        liveQuote =
                            Quote(
                                price = 100.0,
                                lastUpdated = Instant.ofEpochMilli(1000),
                                isStale = false,
                            ),
                    )
                runCurrent()
                val afterFirstQuote = expectMostRecentItemWith { it.sparkline.isNotEmpty() }
                assertEquals(listOf(100.0), afterFirstQuote.sparkline)

                // Update with same price 100.0 but different timestamp (consecutive duplicate)
                paramsFlow.value =
                    paramsFlow.value.copy(
                        liveQuote =
                            Quote(
                                price = 100.0,
                                lastUpdated = Instant.ofEpochMilli(2000),
                                isStale = false,
                            ),
                    )
                runCurrent()
                // Wait to verify sparkline does not duplicate price
                val afterDuplicate = expectMostRecentItemWith { true }
                assertEquals(listOf(100.0), afterDuplicate.sparkline)

                // Update with another price 101.0
                paramsFlow.value =
                    paramsFlow.value.copy(
                        liveQuote =
                            Quote(
                                price = 101.0,
                                lastUpdated = Instant.ofEpochMilli(3000),
                                isStale = false,
                            ),
                    )
                runCurrent()
                val afterSecondQuote = expectMostRecentItemWith { it.sparkline.size == 2 }
                assertEquals(listOf(100.0, 101.0), afterSecondQuote.sparkline)

                // Update with 101.0 again (consecutive duplicate)
                paramsFlow.value =
                    paramsFlow.value.copy(
                        liveQuote =
                            Quote(
                                price = 101.0,
                                lastUpdated = Instant.ofEpochMilli(4000),
                                isStale = false,
                            ),
                    )
                runCurrent()
                val afterSecondDuplicate = expectMostRecentItemWith { true }
                assertEquals(listOf(100.0, 101.0), afterSecondDuplicate.sparkline)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `sparkline respects maximum capacity`() {
        runTest {
            val presenter = WatchlistItemPresenter()
            val item =
                WatchlistItem(
                    instrument = instrument(symbol = "AAPL"),
                    cachedQuote = null,
                )

            val paramsFlow =
                MutableStateFlow(
                    WatchlistItemPresenter.Params(
                        item = item,
                        liveQuote = null,
                        connectionState = ConnectionState.CONNECTED,
                    ),
                )

            moleculeFlow(mode = RecompositionMode.Immediate) {
                val params by paramsFlow.collectAsState()
                presenter.present(params)
            }.test {
                val initial = awaitItem() // initial
                println("TEST 3 INITIAL: sparkline=${initial.sparkline}")

                var lastModel: WatchlistRowUiModel? = null
                // We add 42 unique prices: 1.0 to 42.0
                for (i in 1..42) {
                    paramsFlow.value =
                        paramsFlow.value.copy(
                            liveQuote = quote(price = i.toDouble()),
                        )
                    runCurrent()
                    // Wait for the sparkline to include this price
                    lastModel = expectMostRecentItemWith { it.sparkline.contains(i.toDouble()) }
                }

                // Sparkline capacity is 40. Since we added 42 prices (1..42), it should drop 1.0 and 2.0.
                // So the sparkline should contain [3.0, 4.0, ..., 42.0]
                val finalModel = requireNotNull(lastModel)
                assertEquals(40, finalModel.sparkline.size)
                assertEquals(3.0, finalModel.sparkline.first(), 0.0)
                assertEquals(42.0, finalModel.sparkline.last(), 0.0)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<WatchlistRowUiModel>.expectMostRecentItemWith(
        predicate: (WatchlistRowUiModel) -> Boolean,
    ): WatchlistRowUiModel {
        repeat(times = 150) {
            val item = awaitItem()
            println("EMITTED: symbol=${item.symbol}, price=${item.price}, sparkline=${item.sparkline}")
            if (predicate(item)) {
                return item
            }
        }
        error("No matching model emitted within 150 items")
    }
}
