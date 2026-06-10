package com.davrukin.watchlist.domain.model

import androidx.compose.runtime.Immutable

/**
 * A watchlist entry: the instrument plus its last persisted quote, if any.
 */
@Immutable
data class WatchlistItem(
    val instrument: Instrument,
    val cachedQuote: Quote?,
)
