package com.davrukin.watchlist.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

/**
 * Latest known price for an instrument.
 *
 * [isStale] is true when the value comes from the persisted cache and has not been refreshed
 * this session, or when the live connection is down. A connected-but-quiet market is not stale.
 */
@Immutable
data class Quote(
    val price: Double,
    val change: Double = Double.NaN,
    val percentChange: Double = Double.NaN,
    val lastUpdated: Instant,
    val isStale: Boolean,
    val isUnsupported: Boolean = false,
)
