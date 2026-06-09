package com.davrukin.watchlist.domain.repository

import com.davrukin.watchlist.domain.model.MarketDataMode
import kotlinx.coroutines.flow.StateFlow

interface MarketDataModeRepository {
    val mode: StateFlow<MarketDataMode>

    /** False when no API key is configured, in which case the app is locked to demo mode. */
    val isLiveAvailable: Boolean

    fun toggle()
}
