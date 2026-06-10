package com.davrukin.watchlist.data.remote

import com.davrukin.watchlist.domain.model.Quote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Finnhub `/quote` response. Field names follow Finnhub's abbreviated contract:
 * c = current price, d = change, dp = percent change, t = quote timestamp (epoch seconds).
 * A zeroed payload (c = 0, t = 0) is how Finnhub reports an unknown symbol or missing data.
 */
@Serializable
data class QuoteDto(
    @SerialName(value = "c") val currentPrice: Double = 0.0,
    @SerialName(value = "d") val change: Double? = null,
    @SerialName(value = "dp") val percentChange: Double? = null,
    @SerialName(value = "t") val timestampEpochSeconds: Long = 0,
) {
    fun toQuote(): Quote? {
        if (timestampEpochSeconds == 0L && currentPrice == 0.0) {
            return null
        }
        return Quote(
            price = currentPrice,
            change = change ?: Double.NaN,
            percentChange = percentChange ?: Double.NaN,
            lastUpdated = Instant.ofEpochSecond(timestampEpochSeconds),
            isStale = false,
        )
    }
}
