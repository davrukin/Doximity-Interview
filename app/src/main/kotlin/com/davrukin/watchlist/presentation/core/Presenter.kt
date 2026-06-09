package com.davrukin.watchlist.presentation.core

import androidx.compose.runtime.Composable

/**
 * Produces an immutable [Model] for a screen each time its inputs or internal state change.
 *
 * [present] runs in the Compose runtime but emits no UI: it holds state with `remember` and
 * friends, collects data-layer flows, and reads top to bottom from inputs to the returned model.
 * Presenters compose hierarchically by calling child presenters' [present] functions.
 */
interface Presenter<Model : UiModel, Params> {
    @Composable
    fun present(params: Params): Model
}
