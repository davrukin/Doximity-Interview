package com.davrukin.watchlist.domain.model

/**
 * A watchlist entry: the instrument plus its last persisted quote, if any.
 */
data class WatchlistItem(
    val instrument: Instrument,
    val cachedQuote: Quote?,
)
