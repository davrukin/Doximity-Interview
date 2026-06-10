package com.davrukin.watchlist.domain.usecase

import com.davrukin.watchlist.domain.repository.PriceRepository

class RefreshQuotesUseCase(
    private val repository: PriceRepository,
) {
    suspend operator fun invoke(): Unit = repository.refreshQuotes()
}
