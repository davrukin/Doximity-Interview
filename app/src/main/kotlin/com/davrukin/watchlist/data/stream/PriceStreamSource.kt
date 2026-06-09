package com.davrukin.watchlist.data.stream

import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.PriceTick
import kotlinx.coroutines.flow.Flow

/**
 * A stream of trade ticks and connection-state changes for a changing set of symbols.
 *
 * Implemented by the live Finnhub socket ([ReconnectingPriceStream]) and the demo generator, so
 * everything above this interface is mode-agnostic.
 */
interface PriceStreamSource {
    fun events(symbols: Flow<Set<String>>): Flow<PriceStreamEvent>
}

sealed interface PriceStreamEvent {
    data class Ticks(
        val ticks: List<PriceTick>,
    ) : PriceStreamEvent

    data class ConnectionChanged(
        val state: ConnectionState,
    ) : PriceStreamEvent
}
