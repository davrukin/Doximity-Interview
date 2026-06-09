package com.davrukin.watchlist.data

import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.Quote

/**
 * Everything mode-specific behind one contract: the live Finnhub implementation and the demo
 * generator are swapped by the repositories when the market-data mode changes.
 *
 * [quoteSnapshot] returns success(null) when no snapshot exists for the instrument (crypto has no
 * REST quote; the first streamed tick fills it in) — distinct from failure, which is an error.
 */
interface MarketDataSource {
    suspend fun search(query: String): Result<List<Instrument>>

    suspend fun quoteSnapshot(instrument: Instrument): Result<Quote?>

    val priceStream: PriceStreamSource
}
