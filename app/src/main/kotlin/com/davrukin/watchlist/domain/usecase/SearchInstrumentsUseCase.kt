package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.repository.InstrumentSearchRepository

class SearchInstrumentsUseCase(
    private val repository: InstrumentSearchRepository,
) {
    suspend operator fun invoke(query: String): Result<List<Instrument>> {
        return repository.search(query = query)
    }
}
