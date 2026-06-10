package com.davrukin.watchlist.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.usecase.AddToWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.SearchInstrumentsUseCase
import com.davrukin.watchlist.presentation.core.EventHandler
import com.davrukin.watchlist.presentation.core.Presenter
import com.davrukin.watchlist.presentation.core.launchUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class SearchPresenter(
    private val searchInstruments: SearchInstrumentsUseCase,
    private val observeWatchlist: ObserveWatchlistUseCase,
    private val addToWatchlist: AddToWatchlistUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
    private val appScope: CoroutineScope,
) : Presenter<SearchUiModel, SearchPresenter.Params> {
    data class Params(
        val onBack: () -> Unit,
    )

    private sealed interface SearchState {
        data object Idle : SearchState

        data object Loading : SearchState

        data class Loaded(
            val instruments: List<Instrument>,
        ) : SearchState

        data object Failed : SearchState
    }

    @Composable
    override fun present(params: Params): SearchUiModel {
        var query by rememberSaveable { mutableStateOf("") }
        var retryToken by remember { mutableIntStateOf(0) }
        var searchState by remember { mutableStateOf<SearchState>(value = SearchState.Idle) }
        val watchlist by launchUseCase(initial = emptyList()) { observeWatchlist() }

        LaunchedEffect(query, retryToken) {
            if (query.isBlank()) {
                searchState = SearchState.Idle
                return@LaunchedEffect
            }
            searchState = SearchState.Loading
            delay(duration = DEBOUNCE)
            searchState = searchInstruments(query = query.trim()).fold(
                onSuccess = { instruments ->
                    SearchState.Loaded(instruments = instruments)
                },
                onFailure = {
                    SearchState.Failed
                },
            )
        }

        val watchlistSymbols = watchlist.map { it.instrument.symbol }.toSet() // TODO: lint marks as redundant
        val results =
            when (val state = searchState) {
                is SearchState.Loaded ->
                    state.instruments.map { instrument ->
                        SearchUiModel.Result(
                            instrument = instrument,
                            isOnWatchlist = instrument.symbol in watchlistSymbols,
                        )
                    }

                else -> emptyList()
            }

        val eventHandler =
            remember(params) {
                EventHandler<SearchUiModel.Event> { event ->
                    when (event) {
                        is SearchUiModel.Event.QueryChanged -> query = event.query
                        is SearchUiModel.Event.ToggleWatchlist -> {
                            val onWatchlist = watchlist.any { it.instrument.symbol == event.instrument.symbol }
                            appScope.launch {
                                if (onWatchlist) {
                                    removeFromWatchlist(symbol = event.instrument.symbol)
                                } else {
                                    addToWatchlist(instrument = event.instrument)
                                }
                            }
                        }

                        SearchUiModel.Event.Retry -> retryToken++
                        SearchUiModel.Event.Back -> params.onBack()
                    }
                }
            }

        return SearchUiModel(
            query = query,
            results = results,
            phase =
                when (searchState) {
                    SearchState.Idle -> SearchUiModel.Phase.IDLE
                    SearchState.Loading -> SearchUiModel.Phase.LOADING
                    is SearchState.Loaded -> {
                        if (results.isEmpty()) {
                            SearchUiModel.Phase.EMPTY
                        } else {
                            SearchUiModel.Phase.RESULTS
                        }
                    }

                    SearchState.Failed -> SearchUiModel.Phase.ERROR
                },
            eventHandler = eventHandler,
        )
    }

    companion object {
        private val DEBOUNCE: Duration = 300.milliseconds
    }
}
