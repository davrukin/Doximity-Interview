package com.davrukin.watchlist.presentation.watchlist

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.usecase.ObserveConnectionStateUseCase
import com.davrukin.watchlist.domain.usecase.ObserveMarketDataModeUseCase
import com.davrukin.watchlist.domain.usecase.ObserveQuotesUseCase
import com.davrukin.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.RefreshQuotesUseCase
import com.davrukin.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.ToggleMarketDataModeUseCase
import com.davrukin.watchlist.testing.FakeMarketDataModeRepository
import com.davrukin.watchlist.testing.FakePriceRepository
import com.davrukin.watchlist.testing.FakeWatchlistRepository
import com.davrukin.watchlist.testing.instrument
import com.davrukin.watchlist.testing.quote
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistPresenterTest {
    @Test
    fun `loads then renders cached quotes as stale until connected`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(
                                instrument = instrument(symbol = "AAPL"),
                                cachedQuote = quote(price = 100.0, change = 2.0, isStale = true),
                            ),
                        ),
                )

            fixture.models().test {
                val loading = awaitItem()
                assertTrue(loading.isLoading)

                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                val row = loaded.items.single()
                assertEquals("AAPL", row.symbol)
                assertEquals("100.00", row.price)
                assertTrue(row.isStale)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `live quotes override cached values and clear staleness once connected`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(
                                instrument = instrument(symbol = "AAPL"),
                                cachedQuote = quote(price = 100.0, isStale = true),
                            ),
                        ),
                )

            fixture.models().test {
                awaitItem()
                awaitItem()

                fixture.priceRepository.connectionState.value = ConnectionState.CONNECTED
                fixture.priceRepository.quotes.value =
                    mapOf("AAPL" to quote(price = 101.5, change = 1.5, percentChange = 1.5))

                val updated = expectMostRecentItemWith { !it.items.single().isStale }
                val row = updated.items.single()
                assertEquals("101.50", row.price)
                assertEquals("+1.50 (+1.50%)", row.change)
                assertEquals(true, row.isGain)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tick direction indicator follows live price movement`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "AAPL"), cachedQuote = null),
                        ),
                )

            fixture.models().test {
                awaitItem()
                awaitItem()

                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 100.0))
                expectMostRecentItemWith { it.items.single().price == "100.00" }

                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 101.0))
                val up = expectMostRecentItemWith { it.items.single().movement != null }
                assertEquals(WatchlistRowUiModel.PriceMovement.UP, up.items.single().movement)

                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 100.5))
                val down =
                    expectMostRecentItemWith {
                        it.items.single().movement == WatchlistRowUiModel.PriceMovement.DOWN
                    }
                assertEquals(WatchlistRowUiModel.PriceMovement.DOWN, down.items.single().movement)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `shows missing price when no quote exists anywhere`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "BINANCE:BTCUSDT"), cachedQuote = null),
                        ),
                )

            fixture.models().test {
                awaitItem()
                val loaded = awaitItem()
                assertNull(loaded.items.single().price)
                assertNull(loaded.items.single().change)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `remove event deletes the row`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "AAPL"), cachedQuote = null),
                            WatchlistItem(instrument = instrument(symbol = "MSFT"), cachedQuote = null),
                        ),
                )

            fixture.models().test {
                awaitItem()
                val loaded = awaitItem()

                loaded.eventHandler.onEvent(event = WatchlistUiModel.Event.RequestRemove(symbol = "AAPL"))

                val confirming = expectMostRecentItemWith { it.pendingRemoval != null }
                assertEquals("AAPL", requireNotNull(confirming.pendingRemoval).displaySymbol)

                confirming.eventHandler.onEvent(event = WatchlistUiModel.Event.ConfirmRemoval)

                val updated = expectMostRecentItemWith { it.items.size == 1 && it.pendingRemoval == null }
                assertEquals("MSFT", updated.items.single().symbol)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `row click opens the detail and dismiss closes it`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(
                                instrument = instrument(symbol = "AAPL"),
                                cachedQuote = quote(price = 100.0, change = 2.0, isStale = true),
                            ),
                        ),
                )

            fixture.models().test {
                awaitItem()
                val loaded = awaitItem()

                loaded.eventHandler.onEvent(event = WatchlistUiModel.Event.RowClicked(symbol = "AAPL"))

                val detailed = expectMostRecentItemWith { it.detail != null }
                val detail = requireNotNull(detailed.detail)
                assertEquals("AAPL", detail.displaySymbol)
                assertEquals("100.00", detail.price)

                detailed.eventHandler.onEvent(event = WatchlistUiModel.Event.DismissDetail)
                expectMostRecentItemWith { it.detail == null }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dismissing the removal confirmation keeps the row`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "AAPL"), cachedQuote = null),
                        ),
                )

            fixture.models().test {
                awaitItem()
                val loaded = awaitItem()

                loaded.eventHandler.onEvent(event = WatchlistUiModel.Event.RequestRemove(symbol = "AAPL"))
                val confirming = expectMostRecentItemWith { it.pendingRemoval != null }

                confirming.eventHandler.onEvent(event = WatchlistUiModel.Event.DismissRemoval)

                val dismissed = expectMostRecentItemWith { it.pendingRemoval == null }
                assertEquals("AAPL", dismissed.items.single().symbol)
                assertEquals(1, fixture.watchlistRepository.items.value.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggle event flips the data mode`() =
        runTest {
            val fixture = Fixture(scope = this)

            fixture.models().test {
                val initial = awaitItem()
                assertEquals(MarketDataMode.LIVE, initial.dataMode)

                initial.eventHandler.onEvent(event = WatchlistUiModel.Event.ToggleDataMode)

                val toggled = expectMostRecentItemWith { it.dataMode == MarketDataMode.DEMO }
                assertEquals(MarketDataMode.DEMO, toggled.dataMode)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh event re-fetches quotes and toggles the refreshing flag`() =
        runTest {
            val fixture = Fixture(scope = this)

            fixture.models().test {
                val initial = awaitItem()

                initial.eventHandler.onEvent(event = WatchlistUiModel.Event.Refresh)

                val refreshing = expectMostRecentItemWith { it.isRefreshing }
                assertTrue(refreshing.isRefreshing)
                assertEquals(1, fixture.priceRepository.refreshCount)

                expectMostRecentItemWith { !it.isRefreshing }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cycling sort order reorders rows by symbol then by percent change`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "MSFT"), cachedQuote = null),
                            WatchlistItem(instrument = instrument(symbol = "AAPL"), cachedQuote = null),
                        ),
                )
            fixture.priceRepository.quotes.value =
                mapOf(
                    "MSFT" to quote(price = 500.0, change = 5.0, percentChange = 1.0),
                    "AAPL" to quote(price = 100.0, change = 4.0, percentChange = 4.2),
                )

            fixture.models().test {
                val initial = expectMostRecentItemWith { it.items.size == 2 }
                assertEquals(listOf("MSFT", "AAPL"), initial.items.map { it.symbol })

                initial.eventHandler.onEvent(event = WatchlistUiModel.Event.CycleSortOrder)
                val bySymbol = expectMostRecentItemWith { it.sortOrder == WatchlistUiModel.SortOrder.SYMBOL }
                assertEquals(listOf("AAPL", "MSFT"), bySymbol.items.map { it.symbol })

                bySymbol.eventHandler.onEvent(event = WatchlistUiModel.Event.CycleSortOrder)
                val byChange = expectMostRecentItemWith { it.sortOrder == WatchlistUiModel.SortOrder.CHANGE }
                assertEquals(listOf("AAPL", "MSFT"), byChange.items.map { it.symbol })

                byChange.eventHandler.onEvent(event = WatchlistUiModel.Event.CycleSortOrder)
                val backToAdded = expectMostRecentItemWith { it.sortOrder == WatchlistUiModel.SortOrder.ADDED }
                assertEquals(listOf("MSFT", "AAPL"), backToAdded.items.map { it.symbol })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `accumulates recent live prices into the sparkline`() =
        runTest {
            val fixture =
                Fixture(
                    scope = this,
                    watchlist =
                        listOf(
                            WatchlistItem(instrument = instrument(symbol = "AAPL"), cachedQuote = null),
                        ),
                )

            fixture.models().test {
                expectMostRecentItemWith { it.items.size == 1 }

                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 100.0))
                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 101.0))
                fixture.priceRepository.quotes.value = mapOf("AAPL" to quote(price = 100.5))

                val charted =
                    expectMostRecentItemWith {
                        it.items
                            .single()
                            .sparkline.size == 3
                    }
                assertEquals(listOf(100.0, 101.0, 100.5), charted.items.single().sparkline)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `open search event invokes navigation callback`() =
        runTest {
            var opened = false
            val fixture = Fixture(scope = this, onOpenSearch = { opened = true })

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = WatchlistUiModel.Event.OpenSearch)
                assertTrue(opened)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<WatchlistUiModel>.expectMostRecentItemWith(
        predicate: (WatchlistUiModel) -> Boolean,
    ): WatchlistUiModel {
        repeat(times = 10) {
            val item = awaitItem()
            if (predicate(item)) {
                return item
            }
        }
        error("No matching model emitted within 10 items")
    }

    private class Fixture(
        scope: TestScope,
        watchlist: List<WatchlistItem> = emptyList(),
        onOpenSearch: () -> Unit = {},
    ) {
        val watchlistRepository = FakeWatchlistRepository(initial = watchlist)
        val priceRepository = FakePriceRepository()
        val modeRepository = FakeMarketDataModeRepository()
        private val params = WatchlistPresenter.Params(onOpenSearch = onOpenSearch)
        private val presenter =
            WatchlistPresenter(
                observeWatchlist = ObserveWatchlistUseCase(repository = watchlistRepository),
                observeQuotes =
                    ObserveQuotesUseCase(
                        watchlistRepository = watchlistRepository,
                        priceRepository = priceRepository,
                    ),
                observeConnectionState = ObserveConnectionStateUseCase(repository = priceRepository),
                observeMarketDataMode = ObserveMarketDataModeUseCase(repository = modeRepository),
                toggleMarketDataMode = ToggleMarketDataModeUseCase(repository = modeRepository),
                removeFromWatchlist = RemoveFromWatchlistUseCase(repository = watchlistRepository),
                refreshQuotes = RefreshQuotesUseCase(repository = priceRepository),
                itemPresenter = WatchlistItemPresenter(),
                appScope = scope.backgroundScope,
            )

        fun models() =
            moleculeFlow(mode = RecompositionMode.Immediate) {
                presenter.present(params = params)
            }.distinctUntilChanged()
    }
}
