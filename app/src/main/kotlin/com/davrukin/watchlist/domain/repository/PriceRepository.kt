package com.davrukin.watchlist.domain.repository

import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    /**
     * Live quotes for [instruments], keyed by symbol: REST snapshots merged with streamed ticks.
     *
     * Collecting starts the underlying stream subscription; the connection is shared across
     * collectors and torn down shortly after the last one stops.
     */
    fun observeQuotes(instruments: List<Instrument>): Flow<Map<String, Quote>>

    fun observeConnectionState(): Flow<ConnectionState>

    /** Re-fetches REST snapshots for the instruments currently being observed. */
    suspend fun refreshQuotes()
}
