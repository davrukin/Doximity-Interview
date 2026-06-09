package com.davrukin.watchlist.presentation.core

import androidx.compose.runtime.Immutable

/**
 * Marker for screen state produced by a [Presenter].
 *
 * Implementations are immutable data classes: all publicly accessible properties are fixed after
 * construction, so Compose can safely skip recomposition when an instance is unchanged.
 */
@Immutable
interface UiModel
