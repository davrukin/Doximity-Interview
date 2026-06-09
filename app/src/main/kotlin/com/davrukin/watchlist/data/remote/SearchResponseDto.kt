package com.davrukin.watchlist.data.remote

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val count: Int = 0,
    val result: List<SearchResultDto> = emptyList(),
)

@Serializable
data class SearchResultDto(
    val symbol: String,
    val displaySymbol: String = symbol,
    val description: String = "",
    val type: String = "",
) {
    fun toInstrument(): Instrument =
        Instrument(
            symbol = symbol,
            displaySymbol = displaySymbol,
            description = description,
            type = InstrumentType.STOCK,
        )
}
