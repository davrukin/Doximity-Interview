package com.davrukin.watchlist.domain.model

import androidx.compose.runtime.Immutable

/**
 * A tradable instrument.
 *
 * [symbol] is the Finnhub subscription identifier (e.g. `AAPL`, `BINANCE:BTCUSDT`) and the
 * app-wide identity; [displaySymbol] is what the user sees (e.g. `AAPL`, `BTC/USDT`).
 */
@Immutable
data class Instrument(
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val type: InstrumentType,
)
