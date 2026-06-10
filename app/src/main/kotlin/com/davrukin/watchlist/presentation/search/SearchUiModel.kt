package com.davrukin.watchlist.presentation.search

import androidx.compose.runtime.Immutable
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.presentation.core.EventHandler
import com.davrukin.watchlist.presentation.core.UiEvent
import com.davrukin.watchlist.presentation.core.UiModel

@Immutable
data class SearchUiModel(
    val query: String,
    val results: List<Result>,
    val phase: Phase,
    val eventHandler: EventHandler<Event>,
) : UiModel {
    // TODO: is inside here the best place or should they be in their own files?
    enum class Phase {
        IDLE,
        LOADING,
        RESULTS,
        EMPTY,
        ERROR,
    }

    @Immutable
    data class Result(
        val instrument: Instrument,
        val isOnWatchlist: Boolean,
    )

    sealed interface Event : UiEvent {
        data class QueryChanged(
            val query: String,
        ) : Event

        data class ToggleWatchlist(
            val instrument: Instrument,
        ) : Event

        data object Retry : Event

        data object Back : Event
    }
}
