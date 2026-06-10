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

                loaded.eventHandler.onEvent(event = WatchlistUiModel.Event.Remove(symbol = "AAPL"))

                val updated = expectMostRecentItemWith { it.items.size == 1 }
                assertEquals("MSFT", updated.items.single().symbol)

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
                itemPresenter = WatchlistItemPresenter(),
                appScope = scope.backgroundScope,
            )

        fun models() =
            moleculeFlow(mode = RecompositionMode.Immediate) {
                presenter.present(params = params)
            }.distinctUntilChanged()
    }
}
