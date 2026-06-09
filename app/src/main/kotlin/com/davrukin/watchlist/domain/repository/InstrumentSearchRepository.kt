package com.davrukin.watchlist.domain.repository

import com.davrukin.watchlist.domain.model.Instrument

interface InstrumentSearchRepository {
    /**
     * Searches stocks and crypto pairs matching [query].
     *
     * Failures (network, API errors) are returned as [Result.failure] rather than thrown.
     */
    suspend fun search(query: String): Result<List<Instrument>>
}
