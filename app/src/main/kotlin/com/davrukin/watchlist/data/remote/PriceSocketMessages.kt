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
        const val TYPE_SUBSCRIBE: String = "subscribe"
        const val TYPE_UNSUBSCRIBE: String = "unsubscribe"
    }
}

@Serializable
data class SocketMessageDto(
    val type: String = "",
    val data: List<TradeDto> = emptyList(),
) {
    companion object {
        const val TYPE_TRADE: String = "trade"
    }
}

@Serializable
data class TradeDto(
    @SerialName(value = "s") val symbol: String,
    @SerialName(value = "p") val price: Double,
    @SerialName(value = "t") val timestampEpochMillis: Long,
    @SerialName(value = "v") val volume: Double? = null,
) {
    fun toPriceTick(): PriceTick =
        PriceTick(
            symbol = symbol,
            price = price,
            timestamp = Instant.ofEpochMilli(timestampEpochMillis),
        )
}
