package com.davrukin.watchlist.data

import android.util.Log
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
        Log.d("WatchlistRepository", "Adding ${instrument.symbol} to database")
        val entity = WatchlistItemEntity.fromInstrument(
            instrument = instrument,
            addedAt = clock.instant(),
        )

        try {
            val rowId = dao.insert(entity = entity)
            if (rowId == -1L) {
                if (!dao.exists(symbol = instrument.symbol)) {
                    val message = "Failed to insert ${instrument.symbol}: INSERT OR IGNORE dropped row"
                    Log.e("WatchlistRepository", message)
                    throw IllegalStateException(message)
                } else {
                    Log.d("WatchlistRepository", "${instrument.symbol} already exists in the database")
                }
            } else {
                Log.d("WatchlistRepository", "Successfully added ${instrument.symbol}")
            }
        } catch (e: Exception) {
            Log.e("WatchlistRepository", "Failed to add ${instrument.symbol}", e)
            throw e
        }
    }

    override suspend fun remove(symbol: String) {
        Log.d("WatchlistRepository", "Removing $symbol from database")
        try {
            dao.delete(symbol = symbol)
            Log.d("WatchlistRepository", "Successfully removed $symbol")
        } catch (e: Exception) {
            Log.e("WatchlistRepository", "Failed to remove $symbol", e)
            throw e
        }
    }
}

