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
    val lastPrice: Double = Double.NaN,
    val lastChange: Double = Double.NaN,
    val lastPercentChange: Double = Double.NaN,
    val lastUpdatedEpochMillis: Long = -1L,
) {
    fun toWatchlistItem(): WatchlistItem {
        val quote: Quote? = if (!lastPrice.isNaN() && lastUpdatedEpochMillis != -1L) {
            Quote(
                price = lastPrice,
                change = lastChange,
                percentChange = lastPercentChange,
                lastUpdated = Instant.ofEpochMilli(lastUpdatedEpochMillis),
                isStale = true,
            )
        } else {
            null
        }

        return WatchlistItem(
            instrument = Instrument(
                symbol = symbol,
                displaySymbol = displaySymbol,
                description = description,
                type = type,
            ),
            cachedQuote = quote,
        )
    }

    companion object {
        fun fromInstrument(
            instrument: Instrument,
            addedAt: Instant,
        ): WatchlistItemEntity {
            return WatchlistItemEntity(
                symbol = instrument.symbol,
                displaySymbol = instrument.displaySymbol,
                description = instrument.description,
                type = instrument.type,
                addedAtEpochMillis = addedAt.toEpochMilli(),
            )
        }
    }
}
