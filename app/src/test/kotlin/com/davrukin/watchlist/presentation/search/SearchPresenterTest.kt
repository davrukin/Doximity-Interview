package com.davrukin.watchlist.presentation.search

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.davrukin.watchlist.domain.usecase.AddToWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.SearchInstrumentsUseCase
import com.davrukin.watchlist.testing.FakeInstrumentSearchRepository
import com.davrukin.watchlist.testing.FakeWatchlistRepository
import com.davrukin.watchlist.testing.instrument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPresenterTest {
    @Test
    fun `starts idle with a blank query`() {
        runTest {
            val fixture = Fixture(scope = this)

            fixture.models().test {
                val initial = awaitItem()
                assertEquals("", initial.query)
                assertEquals(SearchUiModel.Phase.IDLE, initial.phase)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `debounced query produces results flagged with watchlist membership`() {
        runTest {
            val fixture = Fixture(scope = this)
            fixture.searchRepository.result =
                Result.success(
                    listOf(instrument(symbol = "AAPL"), instrument(symbol = "AAPD")),
                )
            fixture.watchlistRepository.add(instrument = instrument(symbol = "AAPL"))

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = SearchUiModel.Event.QueryChanged(query = "aap"))

                val loading = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.LOADING }
                assertEquals("aap", loading.query)

                val loaded = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.RESULTS }
                assertEquals(listOf("AAPL", "AAPD"), loaded.results.map { it.instrument.symbol })
                assertEquals(listOf(true, false), loaded.results.map { it.isOnWatchlist })
                assertEquals(listOf("aap"), fixture.searchRepository.queries)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `empty results surface the empty phase`() {
        runTest {
            val fixture = Fixture(scope = this)
            fixture.searchRepository.result = Result.success(emptyList())

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = SearchUiModel.Event.QueryChanged(query = "zzz"))

                val empty = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.EMPTY }
                assertTrue(empty.results.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `failure surfaces the error phase and retry re-runs the search`() {
        runTest {
            val fixture = Fixture(scope = this)
            fixture.searchRepository.result = Result.failure(RuntimeException("boom"))

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = SearchUiModel.Event.QueryChanged(query = "aap"))

                val failed = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.ERROR }

                fixture.searchRepository.result = Result.success(listOf(instrument(symbol = "AAPL")))
                failed.eventHandler.onEvent(event = SearchUiModel.Event.Retry)

                val recovered = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.RESULTS }
                assertEquals(
                    "AAPL",
                    recovered.results
                        .single()
                        .instrument.symbol,
                )
                assertEquals(2, fixture.searchRepository.queries.size)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `toggle adds when absent and removes when present`() {
        runTest {
            val fixture = Fixture(scope = this)
            val aapl = instrument(symbol = "AAPL")
            fixture.searchRepository.result = Result.success(listOf(aapl))

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = SearchUiModel.Event.QueryChanged(query = "aap"))

                val loaded = expectMostRecentItemWith { it.phase == SearchUiModel.Phase.RESULTS }
                loaded.eventHandler.onEvent(event = SearchUiModel.Event.ToggleWatchlist(instrument = aapl))

                val added = expectMostRecentItemWith { it.results.single().isOnWatchlist }
                added.eventHandler.onEvent(event = SearchUiModel.Event.ToggleWatchlist(instrument = aapl))

                expectMostRecentItemWith { !it.results.single().isOnWatchlist }
                assertTrue(
                    fixture.watchlistRepository.items.value
                        .isEmpty(),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `back event invokes navigation callback`() {
        runTest {
            var backed = false
            val fixture = Fixture(scope = this, onBack = { backed = true })

            fixture.models().test {
                awaitItem().eventHandler.onEvent(event = SearchUiModel.Event.Back)
                assertTrue(backed)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<SearchUiModel>.expectMostRecentItemWith(
        predicate: (SearchUiModel) -> Boolean,
    ): SearchUiModel {
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
        onBack: () -> Unit = {},
    ) {
        val searchRepository = FakeInstrumentSearchRepository()
        val watchlistRepository = FakeWatchlistRepository()
        private val params = SearchPresenter.Params(onBack = onBack)
        private val presenter =
            SearchPresenter(
                searchInstruments = SearchInstrumentsUseCase(repository = searchRepository),
                observeWatchlist = ObserveWatchlistUseCase(repository = watchlistRepository),
                addToWatchlist = AddToWatchlistUseCase(repository = watchlistRepository),
                removeFromWatchlist = RemoveFromWatchlistUseCase(repository = watchlistRepository),
                appScope = scope.backgroundScope,
            )

        fun models(): Flow<SearchUiModel> {
            return moleculeFlow(mode = RecompositionMode.Immediate) {
                presenter.present(params = params)
            }.distinctUntilChanged()
        }
    }
}
