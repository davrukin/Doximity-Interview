package com.davrukin.watchlist.presentation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.Flow

/**
 * Collects the flow returned by [useCase] into [State], starting once per composition.
 *
 * The collection is keyed to the composition (not the flow instance), so recompositions do not
 * re-invoke [useCase] or restart the collection. That once-only contract is what the
 * lambda-param-in-effect lint rule warns about; it is disabled for this file in .editorconfig
 * because here the behavior is the point.
 */
@Composable
fun <T> launchUseCase(
    initial: T,
    useCase: () -> Flow<T>,
): State<T> =
    produceState(initialValue = initial) {
        useCase()
            .collect {
                value = it
            }
    }
