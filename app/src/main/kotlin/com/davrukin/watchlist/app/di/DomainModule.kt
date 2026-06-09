package com.davrukin.watchlist.app.di

import com.davrukin.watchlist.domain.usecase.AddToWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.ObserveConnectionStateUseCase
import com.davrukin.watchlist.domain.usecase.ObserveMarketDataModeUseCase
import com.davrukin.watchlist.domain.usecase.ObserveQuotesUseCase
import com.davrukin.watchlist.domain.usecase.ObserveWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.RemoveFromWatchlistUseCase
import com.davrukin.watchlist.domain.usecase.SearchInstrumentsUseCase
import com.davrukin.watchlist.domain.usecase.ToggleMarketDataModeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule =
    module {
        factoryOf(::SearchInstrumentsUseCase)
        factoryOf(::ObserveWatchlistUseCase)
        factoryOf(::AddToWatchlistUseCase)
        factoryOf(::RemoveFromWatchlistUseCase)
        factoryOf(::ObserveQuotesUseCase)
        factoryOf(::ObserveConnectionStateUseCase)
        factoryOf(::ObserveMarketDataModeUseCase)
        factoryOf(::ToggleMarketDataModeUseCase)
    }
