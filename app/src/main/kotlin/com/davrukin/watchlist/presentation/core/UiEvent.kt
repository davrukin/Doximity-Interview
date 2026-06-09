package com.davrukin.watchlist.presentation.core

/**
 * Marker for events the UI dispatches back to a [Presenter].
 *
 * Events are modeled as a sealed interface nested inside the [UiModel] they belong to, so the
 * full set of user actions for a screen is type-checked in a single `when` expression.
 */
interface UiEvent
