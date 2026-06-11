package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.presentation.core.Presenter
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Presents one watchlist row: prefers the live quote, falls back to the persisted quote marked
 * stale, and marks everything stale while the stream is not connected.
 */
class WatchlistItemPresenter : Presenter<WatchlistRowUiModel, WatchlistItemPresenter.Params> {
    data class Params(
        val item: WatchlistItem,
        val liveQuote: Quote?,
        val connectionState: ConnectionState,
    )

    @Composable
    override fun present(params: Params): WatchlistRowUiModel {
        val instrument: Instrument = params.item.instrument
        val quote: Quote? = params.liveQuote ?: params.item.cachedQuote
        val isStale: Boolean =
            quote != null &&
                (quote.isStale || params.connectionState != ConnectionState.CONNECTED)

        val livePrice: Double = params.liveQuote?.price ?: Double.NaN
        var previousPrice: Double by rememberSaveable { mutableDoubleStateOf(value = Double.NaN) }
        var movement: WatchlistRowUiModel.PriceMovement? by rememberSaveable {
            mutableStateOf(value = null)
        }
        // Use rememberSaveable to preserve sparkline history and state across rotations and screen transitions (Nav3).
        val recentPrices: SnapshotStateList<Double> =
            rememberSaveable(
                saver =
                    Saver(
                        save = { list: SnapshotStateList<Double> ->
                            list.toDoubleArray()
                        },
                        restore = { array: DoubleArray ->
                            mutableStateListOf<Double>().apply {
                                addAll(elements = array.toList())
                            }
                        },
                    ),
            ) {
                mutableStateListOf()
            }
        LaunchedEffect(key1 = livePrice) {
            val previous: Double = previousPrice
            previousPrice = livePrice
            if (!livePrice.isNaN() && !previous.isNaN() && livePrice != previous) {
                movement =
                    if (livePrice > previous) {
                        WatchlistRowUiModel.PriceMovement.UP
                    } else {
                        WatchlistRowUiModel.PriceMovement.DOWN
                    }
            }
            if (!livePrice.isNaN()) {
                recentPrices.add(livePrice)
                if (recentPrices.size > SPARKLINE_CAPACITY) {
                    recentPrices.removeAt(0)
                }
            }
        }

        return WatchlistRowUiModel(
            symbol = instrument.symbol,
            displaySymbol = instrument.displaySymbol,
            description = instrument.description,
            type = instrument.type,
            price =
                quote?.let { quoteValue ->
                    if (quoteValue.price.isNaN()) {
                        null
                    } else {
                        priceFormat.format(quoteValue.price)
                    }
                },
            change =
                quote?.let { quoteValue ->
                    formatChange(quote = quoteValue)
                },
            isGain =
                quote?.let { quoteValue ->
                    if (quoteValue.change.isNaN()) {
                        null
                    } else {
                        quoteValue.change >= 0
                    }
                },
            isStale = isStale,
            isUnsupported = quote?.isUnsupported == true,
            staleAsOf =
                if (isStale) {
                    timeFormat.format(quote.lastUpdated.atZone(zoneId))
                } else {
                    null
                },
            movement = movement,
            sparkline = recentPrices.toList(),
        )
    }

    private fun formatChange(quote: Quote): String? {
        if (quote.change.isNaN()) {
            return null
        }
        val sign: String =
            if (quote.change >= 0) {
                "+"
            } else {
                ""
            }
        val formatted = "$sign${priceFormat.format(quote.change)}"
        if (quote.percentChange.isNaN()) {
            return formatted
        }
        return "$formatted ($sign${percentFormat.format(quote.percentChange)}%)"
    }

    private val priceFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private val percentFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    companion object {
        private const val SPARKLINE_CAPACITY: Int = 40
    }
}
