package com.davrukin.watchlist.data.stream

import app.cash.turbine.test
import com.davrukin.watchlist.data.remote.PriceSocket
import com.davrukin.watchlist.data.remote.PriceSocketEvent
import com.davrukin.watchlist.domain.model.ConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectingPriceStreamTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `connects subscribes and parses ticks`() =
        runTest {
            val socket = FakePriceSocket()
            val stream = ReconnectingPriceStream(socket = socket, json = json, jitterFraction = 0.0)
            val symbols = MutableStateFlow(setOf("AAPL"))

            stream.events(symbols = symbols).test {
                assertEquals(connectionChanged(ConnectionState.CONNECTING), awaitItem())
                socket.open()
                assertEquals(connectionChanged(ConnectionState.CONNECTED), awaitItem())
                runCurrent()
                assertEquals(listOf("""{"type":"subscribe","symbol":"AAPL"}"""), socket.sent)

                socket.message(text = """{"type":"trade","data":[{"s":"AAPL","p":101.5,"t":1700000000000}]}""")
                val ticks = awaitItem() as PriceStreamEvent.Ticks
                assertEquals("AAPL", ticks.ticks.single().symbol)
                assertEquals(101.5, ticks.ticks.single().price, 0.0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ignores non-trade messages`() =
        runTest {
            val socket = FakePriceSocket()
            val stream = ReconnectingPriceStream(socket = socket, json = json, jitterFraction = 0.0)

            stream.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                awaitItem()
                socket.open()
                awaitItem()
                socket.message(text = """{"type":"ping"}""")
                socket.message(text = "not json at all")
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `diffs symbol changes into subscribe and unsubscribe messages`() =
        runTest {
            val socket = FakePriceSocket()
            val stream = ReconnectingPriceStream(socket = socket, json = json, jitterFraction = 0.0)
            val symbols = MutableStateFlow(setOf("AAPL"))

            stream.events(symbols = symbols).test {
                awaitItem()
                socket.open()
                awaitItem()
                runCurrent()

                symbols.value = setOf("MSFT")
                runCurrent()
                assertEquals(
                    listOf(
                        """{"type":"subscribe","symbol":"AAPL"}""",
                        """{"type":"subscribe","symbol":"MSFT"}""",
                        """{"type":"unsubscribe","symbol":"AAPL"}""",
                    ),
                    socket.sent,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reconnects after a drop and resubscribes current symbols`() =
        runTest {
            val socket = FakePriceSocket()
            val stream = ReconnectingPriceStream(socket = socket, json = json, jitterFraction = 0.0)

            stream.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                awaitItem()
                socket.open()
                awaitItem()
                runCurrent()

                socket.fail()
                assertEquals(connectionChanged(ConnectionState.RECONNECTING), awaitItem())

                advanceTimeBy(1.5.seconds)
                runCurrent()
                assertEquals(2, socket.connectCount)

                socket.open()
                assertEquals(connectionChanged(ConnectionState.CONNECTED), awaitItem())
                runCurrent()
                assertEquals(2, socket.sent.count { it == """{"type":"subscribe","symbol":"AAPL"}""" })

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reports offline after repeated failures while still retrying`() =
        runTest {
            val socket = FakePriceSocket()
            val stream =
                ReconnectingPriceStream(
                    socket = socket,
                    json = json,
                    jitterFraction = 0.0,
                    offlineAfterAttempts = 2,
                )

            stream.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                awaitItem()
                socket.open()
                awaitItem()
                runCurrent()

                socket.fail()
                assertEquals(connectionChanged(ConnectionState.RECONNECTING), awaitItem())

                advanceTimeBy(1.5.seconds)
                runCurrent()
                socket.fail()
                assertEquals(connectionChanged(ConnectionState.OFFLINE), awaitItem())

                advanceTimeBy(2.5.seconds)
                runCurrent()
                assertTrue(socket.connectCount >= 3)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `backs off exponentially between retries`() =
        runTest {
            val socket = FakePriceSocket()
            val stream = ReconnectingPriceStream(socket = socket, json = json, jitterFraction = 0.0)

            stream.events(symbols = MutableStateFlow(setOf("AAPL"))).test {
                awaitItem()
                socket.open()
                awaitItem()
                runCurrent()

                socket.fail()
                awaitItem()
                advanceTimeBy(1.1.seconds)
                runCurrent()
                assertEquals(2, socket.connectCount)

                socket.fail()
                awaitItem()
                advanceTimeBy(1.1.seconds)
                runCurrent()
                assertEquals(2, socket.connectCount)

                advanceTimeBy(1.1.seconds)
                runCurrent()
                assertEquals(3, socket.connectCount)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun connectionChanged(state: ConnectionState): PriceStreamEvent =
        PriceStreamEvent.ConnectionChanged(state = state)

    private class FakePriceSocket : PriceSocket {
        val sent = mutableListOf<String>()
        var connectCount = 0
            private set
        private var channel: Channel<PriceSocketEvent>? = null

        override fun connect(): Flow<PriceSocketEvent> {
            connectCount++
            val created = Channel<PriceSocketEvent>(Channel.UNLIMITED)
            channel = created
            return created.consumeAsFlow()
        }

        suspend fun open() {
            requireNotNull(channel).send(
                PriceSocketEvent.Opened(
                    session = { text ->
                        sent += text
                        true
                    },
                ),
            )
        }

        suspend fun message(text: String) {
            requireNotNull(channel).send(PriceSocketEvent.MessageReceived(text = text))
        }

        suspend fun fail() {
            requireNotNull(channel).send(PriceSocketEvent.Failed(cause = null))
        }
    }
}
