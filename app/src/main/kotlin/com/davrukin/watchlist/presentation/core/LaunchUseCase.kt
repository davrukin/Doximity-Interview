package com.davrukin.watchlist.presentation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.Flow

/**
 * Collects the flow returned by [useCase] into [State], starting once per composition.
 *
 * The collection is keyed to the composition (not the flow instance), so recompositions do not
 * re-invoke [useCase] or restart the collection.
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
