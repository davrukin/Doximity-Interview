package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.davrukin.watchlist.domain.model.ConnectionState
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
        val watchlist by launchUseCase(initial = null as List<WatchlistItem>?) { observeWatchlist() }
        val quotes by launchUseCase(initial = emptyMap<String, Quote>()) { observeQuotes() }
        val connectionState by launchUseCase(initial = ConnectionState.CONNECTING) { observeConnectionState() }
        val dataMode by observeMarketDataMode().collectAsState()
        var isRefreshing by remember { mutableStateOf(false) }

        val items =
            (watchlist ?: emptyList()).map { item ->
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
            remember(params) {
                EventHandler<WatchlistUiModel.Event> { event ->
                    when (event) {
                        is WatchlistUiModel.Event.Remove -> {
                            appScope.launch { removeFromWatchlist(symbol = event.symbol) }
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
                        WatchlistUiModel.Event.ToggleDataMode -> toggleMarketDataMode()
                        WatchlistUiModel.Event.OpenSearch -> params.onOpenSearch()
                    }
                }
            }

        return WatchlistUiModel(
            items = items,
            isLoading = watchlist == null,
            isRefreshing = isRefreshing,
            connectionState = connectionState,
            dataMode = dataMode,
            isLiveAvailable = observeMarketDataMode.isLiveAvailable,
            eventHandler = eventHandler,
        )
    }

    companion object {
        private val REFRESH_SPINNER_MINIMUM: Duration = 400.milliseconds
    }
}
