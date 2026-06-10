package com.davrukin.watchlist.app.di

import com.davrukin.watchlist.presentation.search.SearchPresenter
import com.davrukin.watchlist.presentation.watchlist.WatchlistItemPresenter
import com.davrukin.watchlist.presentation.watchlist.WatchlistPresenter
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val presentationModule: Module =
    module {
        factoryOf(::WatchlistItemPresenter)
        factoryOf(::WatchlistPresenter)
        factoryOf(::SearchPresenter)
    }
