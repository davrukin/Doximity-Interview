package com.davrukin.watchlist.data.stream

import com.davrukin.watchlist.data.remote.PriceSocket
import com.davrukin.watchlist.data.remote.PriceSocketEvent
import com.davrukin.watchlist.data.remote.PriceSocketSession
import com.davrukin.watchlist.data.remote.SocketMessageDto
import com.davrukin.watchlist.data.remote.SubscriptionMessageDto
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.PriceTick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Maintains one socket connection, resubscribing the current symbol set after every (re)connect
 * and retrying dropped connections with exponential backoff plus jitter: delays double from
 * [baseRetryDelay] up to [maxRetryDelay], each randomized by ±[jitterFraction] so simultaneous
 * clients don't reconnect in lockstep. Reports [ConnectionState.OFFLINE] once
 * [offlineAfterAttempts] consecutive attempts have failed, while continuing to retry.
 */
class ReconnectingPriceStream(
    private val socket: PriceSocket,
    private val json: Json,
    private val baseRetryDelay: Duration = 1.seconds,
    private val maxRetryDelay: Duration = 30.seconds,
    private val jitterFraction: Double = 0.2,
    private val random: Random = Random.Default,
    private val offlineAfterAttempts: Int = 3,
) : PriceStreamSource {
    override fun events(symbols: Flow<Set<String>>): Flow<PriceStreamEvent> =
        channelFlow {
            val latestSymbols = MutableStateFlow(value = emptySet<String>())
            launch {
                symbols.collect {
                    latestSymbols.value = it
                }
            }
            var failedAttempts = 0
            send(element = PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTING))
            while (true) {
                var subscriptionJob: Job? = null
                try {
                    socket.connect().collect { event ->
                        when (event) {
                            is PriceSocketEvent.Opened -> {
                                failedAttempts = 0
                                send(element = PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED))
                                subscriptionJob =
                                    launch {
                                        manageSubscriptions(session = event.session, wantedSymbols = latestSymbols)
                                    }
                            }
                            is PriceSocketEvent.MessageReceived -> {
                                val ticks = parseTicks(event.text)
                                if (ticks.isNotEmpty()) {
                                    send(PriceStreamEvent.Ticks(ticks = ticks))
                                }
                            }
                            is PriceSocketEvent.Failed -> throw PriceSocketDroppedException()
                        }
                    }
                } catch (_: PriceSocketDroppedException) {
                    // Connection dropped; fall through to the retry loop.
                } finally {
                    subscriptionJob?.cancel()
                }
                failedAttempts++
                send(PriceStreamEvent.ConnectionChanged(state = stateForFailedAttempts(failedAttempts)))
                delay(retryDelayFor(failedAttempts = failedAttempts))
            }
        }

    private fun stateForFailedAttempts(failedAttempts: Int): ConnectionState =
        if (failedAttempts < offlineAfterAttempts) {
            ConnectionState.RECONNECTING
        } else {
            ConnectionState.OFFLINE
        }

    private fun retryDelayFor(failedAttempts: Int): Duration {
        val exponential = baseRetryDelay * 2.0.pow(n = failedAttempts - 1)
        val capped = minOf(exponential, maxRetryDelay)
        if (jitterFraction == 0.0) {
            return capped
        }
        return capped * (1 + random.nextDouble(from = -jitterFraction, until = jitterFraction))
    }

    private suspend fun manageSubscriptions(
        session: PriceSocketSession,
        wantedSymbols: StateFlow<Set<String>>,
    ) {
        var subscribed = emptySet<String>()
        wantedSymbols.collect { wanted ->
            (wanted - subscribed).forEach { symbol ->
                session.send(text = subscriptionMessage(type = SubscriptionMessageDto.TYPE_SUBSCRIBE, symbol = symbol))
            }
            (subscribed - wanted).forEach { symbol ->
                session.send(
                    text = subscriptionMessage(type = SubscriptionMessageDto.TYPE_UNSUBSCRIBE, symbol = symbol),
                )
            }
            subscribed = wanted
        }
    }

    private fun subscriptionMessage(
        type: String,
        symbol: String,
    ): String {
        return json.encodeToString(value = SubscriptionMessageDto(type = type, symbol = symbol))
    }

    private fun parseTicks(text: String): List<PriceTick> {
        val message =
            try {
                json.decodeFromString<SocketMessageDto>(text)
            } catch (_: SerializationException) {
                return emptyList()
            }
        if (message.type != SocketMessageDto.TYPE_TRADE) {
            return emptyList()
        }
        return message.data.map { it.toPriceTick() }
    }

    private class PriceSocketDroppedException : Exception()
}
