package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Immutable
import com.davrukin.watchlist.presentation.core.UiModel

/**
 * One watchlist row, fully formatted for display. [price] and [change] are null when no value is
 * known yet (e.g. crypto awaiting its first tick), which the UI renders as a missing price.
 */
@Immutable
data class WatchlistRowUiModel(
    val symbol: String,
    val displaySymbol: String,
    val description: String,
    val price: String?,
    val change: String?,
    val isGain: Boolean?,
    val isStale: Boolean,
    val staleAsOf: String? = null,
    val movement: PriceMovement? = null,
) : UiModel {
    enum class PriceMovement {
        UP,
        DOWN,
    }
}
