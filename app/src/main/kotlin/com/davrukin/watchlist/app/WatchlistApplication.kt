package com.davrukin.watchlist.app

import android.app.Application
import com.davrukin.watchlist.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WatchlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WatchlistApplication)
            modules(appModule)
        }
    }
}
