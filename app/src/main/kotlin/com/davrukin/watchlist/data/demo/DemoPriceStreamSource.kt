package com.davrukin.watchlist.data.demo

import com.davrukin.watchlist.data.stream.PriceStreamEvent
import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.PriceTick
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Clock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Random-walk tick generator. Periodically simulates a brief connection blip so the
 * reconnecting state is observable in demo mode.
 */
class DemoPriceStreamSource(
    private val catalog: DemoInstrumentCatalog,
    private val clock: Clock,
    private val random: Random = Random.Default,
    private val tickInterval: Duration = 800.milliseconds,
    private val reconnectAfterTicks: Int = 50,
    private val reconnectPause: Duration = 3.seconds,
) : PriceStreamSource {
    override fun events(symbols: Flow<Set<String>>): Flow<PriceStreamEvent> =
        channelFlow {
            val latestSymbols = MutableStateFlow(emptySet<String>())
            launch {
                symbols.collect { latestSymbols.value = it }
            }
            send(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTING))
            delay(duration = 300.milliseconds)
            send(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED))
            val prices = mutableMapOf<String, Double>()
            var ticksSinceReconnect = 0
            while (true) {
                delay(duration = tickInterval)
                ticksSinceReconnect++
                if (ticksSinceReconnect >= reconnectAfterTicks) {
                    ticksSinceReconnect = 0
                    send(PriceStreamEvent.ConnectionChanged(state = ConnectionState.RECONNECTING))
                    delay(duration = reconnectPause)
                    send(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED))
                }
                val ticks =
                    latestSymbols.value.mapNotNull { symbol ->
                        if (random.nextFloat() > TICK_PROBABILITY) {
                            return@mapNotNull null
                        }
                        nextTick(symbol = symbol, prices = prices)
                    }
                if (ticks.isNotEmpty()) {
                    send(PriceStreamEvent.Ticks(ticks = ticks))
                }
            }
        }

    private fun nextTick(
        symbol: String,
        prices: MutableMap<String, Double>,
    ): PriceTick? {
        val base = catalog.basePrice(symbol = symbol) ?: return null
        val current = prices.getOrPut(symbol) { base }
        val drift = current * (random.nextDouble(from = -STEP_FRACTION, until = STEP_FRACTION))
        val next = (current + drift).coerceAtLeast(minimumValue = 0.01)
        prices[symbol] = next
        return PriceTick(
            symbol = symbol,
            price = next,
            timestamp = clock.instant(),
        )
    }

    companion object {
        private const val TICK_PROBABILITY = 0.7f
        private const val STEP_FRACTION = 0.0015
    }
}
