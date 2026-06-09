package com.davrukin.watchlist.app

import android.app.Application
import com.davrukin.watchlist.app.di.appModule
import com.davrukin.watchlist.app.di.dataModule
import com.davrukin.watchlist.app.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WatchlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WatchlistApplication)
            modules(
                appModule,
                dataModule,
                domainModule,
            )
        }
    }
}
