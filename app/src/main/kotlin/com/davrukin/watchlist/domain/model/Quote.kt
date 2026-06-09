package com.davrukin.watchlist.domain.model

import java.time.Instant

/**
 * Latest known price for an instrument.
 *
 * [isStale] is true when the value comes from the persisted cache and has not been refreshed
 * this session, or when the live connection is down. A connected-but-quiet market is not stale.
 */
data class Quote(
    val price: Double,
    val change: Double?,
    val percentChange: Double?,
    val lastUpdated: Instant,
    val isStale: Boolean,
)
