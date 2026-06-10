package com.davrukin.watchlist.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.presentation.core.EventHandler
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingUpdatesQueryThroughEvents() {
        composeRule.setContent {
            var model by remember { mutableStateOf(model(phase = SearchUiModel.Phase.IDLE)) }
            SearchScreen(
                model =
                    model.copy(
                        eventHandler =
                            EventHandler { event ->
                                if (event is SearchUiModel.Event.QueryChanged) {
                                    model = model.copy(query = event.query)
                                }
                            },
                    ),
            )
        }

        composeRule.onNodeWithText(text = "Symbol or name, e.g. AAPL or BTC").performTextInput(text = "btc")

        composeRule.onNodeWithText(text = "btc").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryAndDispatchesEvent() {
        val events = mutableListOf<SearchUiModel.Event>()
        composeRule.setContent {
            SearchScreen(
                model =
                    model(
                        phase = SearchUiModel.Phase.ERROR,
                        eventHandler = EventHandler { events += it },
                    ),
            )
        }

        composeRule.onNodeWithText(text = "Search failed").assertIsDisplayed()
        composeRule.onNodeWithText(text = "Retry").performClick()

        assertEquals(listOf<SearchUiModel.Event>(SearchUiModel.Event.Retry), events)
    }

    @Test
    fun resultRowDispatchesToggleEvent() {
        val events = mutableListOf<SearchUiModel.Event>()
        composeRule.setContent {
            SearchScreen(
                model =
                    model(
                        phase = SearchUiModel.Phase.RESULTS,
                        results = listOf(SearchUiModel.Result(instrument = aapl, isOnWatchlist = false)),
                        eventHandler = EventHandler { events += it },
                    ),
            )
        }

        composeRule.onNodeWithContentDescription(label = "Add AAPL to watchlist").performClick()

        assertEquals(listOf<SearchUiModel.Event>(SearchUiModel.Event.ToggleWatchlist(instrument = aapl)), events)
    }

    @Test
    fun emptyPhaseShowsNoMatches() {
        composeRule.setContent {
            SearchScreen(model = model(query = "zzz", phase = SearchUiModel.Phase.EMPTY))
        }

        composeRule.onNodeWithText(text = "No matches for \"zzz\"").assertIsDisplayed()
    }

    private val aapl =
        Instrument(
            symbol = "AAPL",
            displaySymbol = "AAPL",
            description = "APPLE INC",
            type = InstrumentType.STOCK,
        )

    private fun model(
        query: String = "",
        results: List<SearchUiModel.Result> = emptyList(),
        phase: SearchUiModel.Phase,
        eventHandler: EventHandler<SearchUiModel.Event> = EventHandler {},
    ): SearchUiModel =
        SearchUiModel(
            query = query,
            results = results,
            phase = phase,
            eventHandler = eventHandler,
        )
}
