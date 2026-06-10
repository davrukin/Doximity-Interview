package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Immutable
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.presentation.core.EventHandler
import com.davrukin.watchlist.presentation.core.UiEvent
import com.davrukin.watchlist.presentation.core.UiModel

@Immutable
data class WatchlistUiModel(
    val items: List<WatchlistRowUiModel>,
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val connectionState: ConnectionState,
    val dataMode: MarketDataMode,
    val isLiveAvailable: Boolean,
    val sortOrder: SortOrder = SortOrder.ADDED,
    val eventHandler: EventHandler<Event>,
) : UiModel {
    enum class SortOrder {
        ADDED,
        SYMBOL,
        CHANGE,
        ;

        fun next(): SortOrder = entries[(ordinal + 1) % entries.size]
    }

    sealed interface Event : UiEvent {
        data class Remove(
            val symbol: String,
        ) : Event

        data object Refresh : Event

        data object CycleSortOrder : Event

        data object ToggleDataMode : Event

        data object OpenSearch : Event
    }
}
