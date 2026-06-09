package com.davrukin.watchlist.data

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.repository.InstrumentSearchRepository

class InstrumentSearchRepositoryImpl(
    private val selector: MarketDataSelector,
) : InstrumentSearchRepository {
    override suspend fun search(query: String): Result<List<Instrument>> = selector.current.search(query = query)
}
