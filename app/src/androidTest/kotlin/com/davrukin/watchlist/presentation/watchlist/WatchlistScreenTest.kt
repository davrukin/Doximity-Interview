package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.presentation.core.EventHandler
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchlistScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyWatchlistShowsEmptyState() {
        composeRule.setContent {
            WatchlistScreen(model = model(items = emptyList()))
        }

        composeRule.onNodeWithText(text = "Your watchlist is empty").assertIsDisplayed()
    }

    @Test
    fun rowsRenderPriceChangeAndStaleLabel() {
        composeRule.setContent {
            WatchlistScreen(model = model(items = listOf(freshRow, staleRow)))
        }

        composeRule.onNodeWithText(text = "AAPL").assertIsDisplayed()
        composeRule.onNodeWithText(text = "228.40").assertIsDisplayed()
        composeRule.onNodeWithText(text = "+1.20 (+0.53%)").assertIsDisplayed()
        composeRule.onNodeWithText(text = "MSFT").assertIsDisplayed()
        composeRule.onNodeWithText(text = "stale").assertIsDisplayed()
    }

    @Test
    fun missingPriceRendersDash() {
        composeRule.setContent {
            WatchlistScreen(model = model(items = listOf(missingPriceRow)))
        }

        composeRule.onNodeWithText(text = "—").assertIsDisplayed()
    }

    @Test
    fun reconnectingBannerIsShownWhenNotConnected() {
        composeRule.setContent {
            WatchlistScreen(
                model = model(items = listOf(freshRow), connectionState = ConnectionState.RECONNECTING),
            )
        }

        composeRule.onNodeWithText(text = "Connection lost — reconnecting…").assertIsDisplayed()
    }

    @Test
    fun removeButtonDispatchesRemoveEvent() {
        val events = mutableListOf<WatchlistUiModel.Event>()
        composeRule.setContent {
            WatchlistScreen(
                model = model(items = listOf(freshRow), eventHandler = EventHandler { events += it }),
            )
        }

        composeRule.onNodeWithContentDescription(label = "Remove AAPL").performClick()

        assertEquals(listOf<WatchlistUiModel.Event>(WatchlistUiModel.Event.Remove(symbol = "AAPL")), events)
    }

    @Test
    fun fabDispatchesOpenSearchEvent() {
        val events = mutableListOf<WatchlistUiModel.Event>()
        composeRule.setContent {
            WatchlistScreen(
                model = model(items = emptyList(), eventHandler = EventHandler { events += it }),
            )
        }

        composeRule.onNodeWithContentDescription(label = "Search instruments").performClick()

        assertEquals(listOf<WatchlistUiModel.Event>(WatchlistUiModel.Event.OpenSearch), events)
    }

    private val freshRow =
        WatchlistRowUiModel(
            symbol = "AAPL",
            displaySymbol = "AAPL",
            description = "APPLE INC",
            price = "228.40",
            change = "+1.20 (+0.53%)",
            isGain = true,
            isStale = false,
        )

    private val staleRow =
        WatchlistRowUiModel(
            symbol = "MSFT",
            displaySymbol = "MSFT",
            description = "MICROSOFT CORP",
            price = "512.70",
            change = null,
            isGain = null,
            isStale = true,
        )

    private val missingPriceRow =
        WatchlistRowUiModel(
            symbol = "BINANCE:BTCUSDT",
            displaySymbol = "BTC/USDT",
            description = "Binance BTCUSDT",
            price = null,
            change = null,
            isGain = null,
            isStale = false,
        )

    private fun model(
        items: List<WatchlistRowUiModel>,
        connectionState: ConnectionState = ConnectionState.CONNECTED,
        eventHandler: EventHandler<WatchlistUiModel.Event> = EventHandler {},
    ): WatchlistUiModel =
        WatchlistUiModel(
            items = items,
            isLoading = false,
            isRefreshing = false,
            connectionState = connectionState,
            dataMode = MarketDataMode.DEMO,
            isLiveAvailable = false,
            eventHandler = eventHandler,
        )
}
