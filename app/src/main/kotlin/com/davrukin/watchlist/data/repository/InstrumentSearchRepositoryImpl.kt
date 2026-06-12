package com.davrukin.watchlist.data.repository

import com.davrukin.watchlist.data.source.MarketDataSelector
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.repository.InstrumentSearchRepository

class InstrumentSearchRepositoryImpl(
    private val selector: MarketDataSelector,
) : InstrumentSearchRepository {
    override suspend fun search(query: String): Result<List<Instrument>> {
        return selector.current.search(query = query)
    }
}
