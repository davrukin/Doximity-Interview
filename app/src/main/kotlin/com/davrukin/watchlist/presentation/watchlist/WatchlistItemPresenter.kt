package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Composable
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.Quote
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.presentation.core.Presenter
import java.text.NumberFormat
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
        val instrument = params.item.instrument
        val quote = params.liveQuote ?: params.item.cachedQuote
        val isStale =
            quote != null &&
                (quote.isStale || params.connectionState != ConnectionState.CONNECTED)
        return WatchlistRowUiModel(
            symbol = instrument.symbol,
            displaySymbol = instrument.displaySymbol,
            description = instrument.description,
            price = quote?.let { priceFormat.format(it.price) },
            change = quote?.let { formatChange(quote = it) },
            isGain = quote?.change?.let { it >= 0 },
            isStale = isStale,
        )
    }

    private fun formatChange(quote: Quote): String? {
        val change = quote.change ?: return null
        val sign = if (change >= 0) "+" else ""
        val formatted = "$sign${priceFormat.format(change)}"
        val percent = quote.percentChange ?: return formatted
        return "$formatted ($sign${percentFormat.format(percent)}%)"
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
}
