package com.davrukin.watchlist.data.remote

import com.davrukin.watchlist.domain.model.PriceTick
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SubscriptionMessageDto(
    val type: String,
    val symbol: String,
) {
    companion object {
        const val TYPE_SUBSCRIBE = "subscribe"
        const val TYPE_UNSUBSCRIBE = "unsubscribe"
    }
}

@Serializable
data class SocketMessageDto(
    val type: String = "",
    val data: List<TradeDto> = emptyList(),
) {
    companion object {
        const val TYPE_TRADE = "trade"
    }
}

@Serializable
data class TradeDto(
    @SerialName("s") val symbol: String,
    @SerialName("p") val price: Double,
    @SerialName("t") val timestampEpochMillis: Long,
    @SerialName("v") val volume: Double? = null,
) {
    fun toPriceTick(): PriceTick =
        PriceTick(
            symbol = symbol,
            price = price,
            timestamp = Instant.ofEpochMilli(timestampEpochMillis),
        )
}
