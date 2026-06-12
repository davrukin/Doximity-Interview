package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.usecase.ObserveConnectionStateUseCase
import com.davrukin.watchlist.domain.usecase.ObserveMarketDataModeUseCase
import com.davrukin.watchlist.domain.usecase.ObserveQuotesUseCase
import com.davrukin.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.RefreshQuotesUseCase
import com.davrukin.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.ToggleMarketDataModeUseCase
import com.davrukin.watchlist.presentation.core.EventHandler
import com.davrukin.watchlist.presentation.core.Presenter
import com.davrukin.watchlist.presentation.core.launchUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class WatchlistPresenter(
    private val observeWatchlist: ObserveWatchlistUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeConnectionState: ObserveConnectionStateUseCase,
    private val observeMarketDataMode: ObserveMarketDataModeUseCase,
    private val toggleMarketDataMode: ToggleMarketDataModeUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
    private val refreshQuotes: RefreshQuotesUseCase,
    private val itemPresenter: WatchlistItemPresenter,
    private val appScope: CoroutineScope,
) : Presenter<WatchlistUiModel, WatchlistPresenter.Params> {
    data class Params(
        val onOpenSearch: () -> Unit,
    )

    @Composable
    override fun present(params: Params): WatchlistUiModel {
        val watchlist by launchUseCase(initial = null) {
            observeWatchlist()
        }
        val quotes by launchUseCase(initial = emptyMap()) {
            observeQuotes()
        }
        val connectionState by launchUseCase(initial = ConnectionState.CONNECTING) {
            observeConnectionState()
        }
        // Match the repository's initial state (LIVE) to avoid test mismatch
        val dataMode by observeMarketDataMode().collectAsState(initial = MarketDataMode.LIVE)
        var isRefreshing by remember { mutableStateOf(value = false) }
        var sortOrder by rememberSaveable {
            mutableStateOf(value = WatchlistUiModel.SortOrder.ADDED)
        }
        var pendingRemovalSymbol by rememberSaveable {
            mutableStateOf<String?>(value = null)
        }
        var detailSymbol by rememberSaveable {
            mutableStateOf<String?>(value = null)
        }

        val items =
            sortItems(
                items = watchlist ?: emptyList(),
                quotes = quotes,
                sortOrder = sortOrder,
            ).map { item ->
                key(item.instrument.symbol) {
                    itemPresenter.present(
                        params =
                            WatchlistItemPresenter.Params(
                                item = item,
                                liveQuote = quotes[item.instrument.symbol],
                                connectionState = connectionState,
                            ),
                    )
                }
            }

        val eventHandler =
            remember(key1 = params) {
                EventHandler<WatchlistUiModel.Event> { event ->
                    when (event) {
                        is WatchlistUiModel.Event.RequestRemove -> {
                            pendingRemovalSymbol = event.symbol
                        }

                        WatchlistUiModel.Event.ConfirmRemoval -> {
                            val symbol = pendingRemovalSymbol
                            pendingRemovalSymbol = null
                            if (symbol != null) {
                                appScope.launch {
                                    removeFromWatchlist(symbol = symbol)
                                }
                            }
                        }

                        WatchlistUiModel.Event.DismissRemoval -> {
                            pendingRemovalSymbol = null
                        }

                        is WatchlistUiModel.Event.RowClicked -> {
                            detailSymbol = event.symbol
                        }

                        WatchlistUiModel.Event.DismissDetail -> {
                            detailSymbol = null
                        }

                        WatchlistUiModel.Event.Refresh -> {
                            appScope.launch {
                                isRefreshing = true
                                try {
                                    refreshQuotes()
                                    delay(duration = REFRESH_SPINNER_MINIMUM)
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        }

                        WatchlistUiModel.Event.CycleSortOrder -> {
                            sortOrder = sortOrder.next()
                        }

                        WatchlistUiModel.Event.ToggleDataMode -> {
                            toggleMarketDataMode()
                        }

                        WatchlistUiModel.Event.OpenSearch -> {
                            params.onOpenSearch()
                        }
                    }
                }
            }

        val pendingRemoval =
            pendingRemovalSymbol?.let { symbol ->
                items
                    .firstOrNull { it.symbol == symbol }
                    ?.let { row ->
                        WatchlistUiModel.PendingRemoval(
                            symbol = row.symbol,
                            displaySymbol = row.displaySymbol,
                        )
                    }
            }

        val detail =
            detailSymbol?.let { symbol ->
                items.firstOrNull { it.symbol == symbol }
            }

        return WatchlistUiModel(
            items = items,
            isLoading = watchlist == null,
            isRefreshing = isRefreshing,
            sortOrder = sortOrder,
            pendingRemoval = pendingRemoval,
            detail = detail,
            connectionState = connectionState,
            dataMode = dataMode,
            isLiveAvailable = observeMarketDataMode.isLiveAvailable,
            eventHandler = eventHandler,
        )
    }

    private fun sortItems(
        items: List<WatchlistItem>,
        quotes: Map<String, Quote>,
        sortOrder: WatchlistUiModel.SortOrder,
    ): List<WatchlistItem> {
        return when (sortOrder) {
            WatchlistUiModel.SortOrder.ADDED -> {
                items
            }

            WatchlistUiModel.SortOrder.SYMBOL -> {
                items.sortedBy { item ->
                    item.instrument.displaySymbol
                }
            }

            WatchlistUiModel.SortOrder.CHANGE -> {
                items.sortedByDescending { item ->
                    val quote = quotes[item.instrument.symbol] ?: item.cachedQuote
                    val percent = quote?.percentChange ?: Double.NaN
                    // NaN poisons comparators; unknown changes sort to the bottom.
                    if (percent.isNaN()) {
                        Double.NEGATIVE_INFINITY
                    } else {
                        percent
                    }
                }
            }
        }
    }

    companion object {
        private val REFRESH_SPINNER_MINIMUM: Duration = 400.milliseconds
    }
}
