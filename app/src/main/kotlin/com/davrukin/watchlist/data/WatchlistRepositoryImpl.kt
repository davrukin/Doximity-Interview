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
            .map { entities ->
                entities.map { entity ->
                    entity.toWatchlistItem()
                }
            }

    override suspend fun add(instrument: Instrument) {
        val entity: WatchlistItemEntity =
            WatchlistItemEntity.fromInstrument(
                instrument = instrument,
                addedAt = clock.instant(),
            )
        val rowId: Long = dao.insert(entity = entity)
        // INSERT OR IGNORE reports -1 both for benign conflicts (row already present) and for
        // silently dropped rows. Only the second is a bug, and it must fail loudly.
        if (rowId == -1L && !dao.exists(symbol = instrument.symbol)) {
            error("Insert for ${instrument.symbol} was ignored but no row exists")
        }
    }

    override suspend fun remove(symbol: String) {
        dao.delete(symbol = symbol)
    }
}
