package com.davrukin.watchlist.domain.model

import java.time.Instant

data class PriceTick(
    val symbol: String,
    val price: Double,
    val timestamp: Instant,
)
