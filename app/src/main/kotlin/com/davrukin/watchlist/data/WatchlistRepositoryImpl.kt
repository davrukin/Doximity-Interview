package com.davrukin.watchlist.data

import com.davrukin.watchlist.data.local.WatchlistDao
import com.davrukin.watchlist.data.local.WatchlistItemEntity
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class WatchlistRepositoryImpl(
    private val dao: WatchlistDao,
    private val clock: Clock,
) : WatchlistRepository {
    override fun observeWatchlist(): Flow<List<WatchlistItem>> =
        dao
            .observeAll()
            .map { entities -> entities.map { it.toWatchlistItem() } }

    override suspend fun add(instrument: Instrument) {
        dao.insert(
            entity =
                WatchlistItemEntity.fromInstrument(
                    instrument = instrument,
                    addedAt = clock.instant(),
                ),
        )
    }

    override suspend fun remove(symbol: String) {
        dao.delete(symbol = symbol)
    }
}
