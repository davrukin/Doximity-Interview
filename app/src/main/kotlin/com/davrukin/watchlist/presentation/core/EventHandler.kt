package com.davrukin.watchlist.presentation.core

import androidx.compose.runtime.Immutable

/**
 * Single dispatch point for a screen's [UiEvent]s, carried on the [UiModel] in place of a list of
 * callback lambdas.
 *
 * Presenters must `remember` the handler they expose so the same instance is reused across
 * recompositions; otherwise every produced model compares unequal and recomposition skipping is
 * defeated.
 */
@Immutable
fun interface EventHandler<E : UiEvent> {
    fun onEvent(event: E)
}
