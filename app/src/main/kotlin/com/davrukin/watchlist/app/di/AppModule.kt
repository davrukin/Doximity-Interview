package com.davrukin.watchlist.app.di

import com.davrukin.watchlist.common.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import java.time.Clock

val appModule =
    module {
        single {
            AppDispatchers(
                io = Dispatchers.IO,
                default = Dispatchers.Default,
                main = Dispatchers.Main,
            )
        }
        single<Clock> { Clock.systemUTC() }
        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + get<AppDispatchers>().default)
        }
    }
