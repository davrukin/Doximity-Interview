package com.davrukin.watchlist.data.demo

import app.cash.turbine.test
import com.davrukin.watchlist.data.stream.PriceStreamEvent
import com.davrukin.watchlist.domain.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class DemoPriceStreamSourceTest {
    @Test
    fun `connects and emits positive random-walk ticks for watched symbols`() =
        runTest {
            val source = streamSource(reconnectAfterTicks = 1_000)

            source.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                assertEquals(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTING), awaitItem())
                assertEquals(PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED), awaitItem())

                val ticks = awaitFirstTicks()
                assertTrue(ticks.ticks.all { it.symbol == "AAPL" })
                assertTrue(ticks.ticks.all { it.price > 0 })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `periodically simulates a reconnect blip`() =
        runTest {
            val source = streamSource(reconnectAfterTicks = 2)

            source.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                awaitItem()
                awaitItem()

                var sawReconnecting = false
                repeat(times = 10) {
                    val event = awaitItem()
                    if (event == PriceStreamEvent.ConnectionChanged(state = ConnectionState.RECONNECTING)) {
                        sawReconnecting = true
                        assertEquals(
                            PriceStreamEvent.ConnectionChanged(state = ConnectionState.CONNECTED),
                            awaitItem(),
                        )
                        return@repeat
                    }
                }
                assertTrue(sawReconnecting)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun streamSource(reconnectAfterTicks: Int): DemoPriceStreamSource =
        DemoPriceStreamSource(
            catalog = DemoInstrumentCatalog(),
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            random = Random(seed = 42),
            tickInterval = 100.milliseconds,
            reconnectAfterTicks = reconnectAfterTicks,
        )

    private suspend fun app.cash.turbine.ReceiveTurbine<PriceStreamEvent>.awaitFirstTicks(): PriceStreamEvent.Ticks {
        repeat(times = 20) {
            val event = awaitItem()
            if (event is PriceStreamEvent.Ticks) {
                return event
            }
        }
        error("No ticks emitted within 20 events")
    }
}
