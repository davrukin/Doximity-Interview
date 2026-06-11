package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow

class ObserveConnectionStateUseCase(
    private val repository: PriceRepository,
) {
    operator fun invoke(): Flow<ConnectionState> {
        return repository.observeConnectionState()
    }
}
