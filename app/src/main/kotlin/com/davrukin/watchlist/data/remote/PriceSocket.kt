package com.davrukin.watchlist.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * One raw socket session per collection of [connect]. Implementations complete the flow when the
 * server closes the connection and emit [PriceSocketEvent.Failed] on transport errors; reconnect
 * policy lives above this interface in [com.davrukin.watchlist.data.stream.ReconnectingPriceStream] so it can be tested with a fake.
 */
// TODO: should these be separate files?
interface PriceSocket {
    fun connect(): Flow<PriceSocketEvent>
}

sealed interface PriceSocketEvent {
    data class Opened(
        val session: PriceSocketSession,
    ) : PriceSocketEvent

    data class MessageReceived(
        val text: String,
    ) : PriceSocketEvent

    data class Failed(
        val cause: Throwable?,
    ) : PriceSocketEvent
}

fun interface PriceSocketSession {
    fun send(text: String): Boolean
}
