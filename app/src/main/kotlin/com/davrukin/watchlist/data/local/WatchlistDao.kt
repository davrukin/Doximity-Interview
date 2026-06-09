package com.davrukin.watchlist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAtEpochMillis ASC")
    fun observeAll(): Flow<List<WatchlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistItemEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query(
        """
        UPDATE watchlist
        SET lastPrice = :price,
            lastChange = :change,
            lastPercentChange = :percentChange,
            lastUpdatedEpochMillis = :updatedAtEpochMillis
        WHERE symbol = :symbol
        """,
    )
    suspend fun updateQuote(
        symbol: String,
        price: Double,
        change: Double?,
        percentChange: Double?,
        updatedAtEpochMillis: Long,
    )
}
