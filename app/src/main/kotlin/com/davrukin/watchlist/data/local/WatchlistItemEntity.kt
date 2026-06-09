package com.davrukin.watchlist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import java.time.Instant

/**
 * A watchlist row: instrument identity plus the last persisted quote, so the UI can show a
 * last-known price (marked stale) immediately on launch.
 */
@Entity(tableName = "watchlist")
data class WatchlistItemEntity(
    @PrimaryKey val symbol: String,
    val displaySymbol: String,
    val description: String,
    val type: InstrumentType,
    val addedAtEpochMillis: Long,
    val lastPrice: Double? = null,
    val lastChange: Double? = null,
    val lastPercentChange: Double? = null,
    val lastUpdatedEpochMillis: Long? = null,
) {
    fun toWatchlistItem(): WatchlistItem =
        WatchlistItem(
            instrument =
                Instrument(
                    symbol = symbol,
                    displaySymbol = displaySymbol,
                    description = description,
                    type = type,
                ),
            cachedQuote =
                if (lastPrice != null && lastUpdatedEpochMillis != null) {
                    Quote(
                        price = lastPrice,
                        change = lastChange,
                        percentChange = lastPercentChange,
                        lastUpdated = Instant.ofEpochMilli(lastUpdatedEpochMillis),
                        isStale = true,
                    )
                } else {
                    null
                },
        )

    companion object {
        fun fromInstrument(
            instrument: Instrument,
            addedAt: Instant,
        ): WatchlistItemEntity =
            WatchlistItemEntity(
                symbol = instrument.symbol,
                displaySymbol = instrument.displaySymbol,
                description = instrument.description,
                type = instrument.type,
                addedAtEpochMillis = addedAt.toEpochMilli(),
            )
    }
}
