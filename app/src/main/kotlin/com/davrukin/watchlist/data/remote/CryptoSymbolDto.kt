package com.davrukin.watchlist.data.remote

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import kotlinx.serialization.Serializable

@Serializable
data class CryptoSymbolDto(
    val symbol: String,
    val displaySymbol: String = symbol,
    val description: String = "",
) {
    fun toInstrument(): Instrument =
        Instrument(
            symbol = symbol,
            displaySymbol = displaySymbol,
            description = description,
            type = InstrumentType.CRYPTO,
        )
}
