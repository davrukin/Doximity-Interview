package com.davrukin.watchlist.app.di

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import com.davrukin.watchlist.BuildConfig
import com.davrukin.watchlist.data.demo.DemoInstrumentCatalog
import com.davrukin.watchlist.data.demo.DemoMarketDataSource
import com.davrukin.watchlist.data.demo.DemoPriceStreamSource
import com.davrukin.watchlist.data.local.WatchlistDatabase
import com.davrukin.watchlist.data.remote.FinnhubApi
import com.davrukin.watchlist.data.remote.FinnhubAuthInterceptor
import com.davrukin.watchlist.data.remote.LiveMarketDataSource
import com.davrukin.watchlist.data.remote.OkHttpPriceSocket
import com.davrukin.watchlist.data.repository.InstrumentSearchRepositoryImpl
import com.davrukin.watchlist.data.repository.MarketDataModeRepositoryImpl
import com.davrukin.watchlist.data.repository.PriceRepositoryImpl
import com.davrukin.watchlist.data.repository.WatchlistRepositoryImpl
import com.davrukin.watchlist.data.source.MarketDataSelector
import com.davrukin.watchlist.data.source.MarketDataSource
import com.davrukin.watchlist.data.stream.ReconnectingPriceStream
import com.davrukin.watchlist.domain.repository.InstrumentSearchRepository
import com.davrukin.watchlist.domain.repository.MarketDataModeRepository
import com.davrukin.watchlist.domain.repository.PriceRepository
import com.davrukin.watchlist.domain.repository.WatchlistRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private object DataConfig {
    const val LIVE_SOURCE: String = "live"
    const val DEMO_SOURCE: String = "demo"
    const val FINNHUB_SOCKET_URL: String = "wss://ws.finnhub.io"
}

val dataModule: Module =
    module {
        single {
            Json {
                ignoreUnknownKeys = true
            }
        }
        single {
            OkHttpClient
                .Builder()
                .addInterceptor(interceptor = FinnhubAuthInterceptor(apiKey = BuildConfig.FINNHUB_API_KEY))
                .build()
        }
        single<FinnhubApi> {
            Retrofit
                .Builder()
                .baseUrl(FinnhubApi.BASE_URL)
                .client(get<OkHttpClient>())
                .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(FinnhubApi::class.java)
        }

        single {
            Room
                .databaseBuilder(
                    context = androidContext(),
                    klass = WatchlistDatabase::class.java,
                    name = "watchlist.db",
                ).build()
        }
        single {
            get<WatchlistDatabase>().watchlistDao()
        }

        single<MarketDataSource>(qualifier = named(name = DataConfig.LIVE_SOURCE)) {
            LiveMarketDataSource(
                api = get(),
                priceStream =
                    ReconnectingPriceStream(
                        socket =
                            OkHttpPriceSocket(
                                client = get(),
                                url = "${DataConfig.FINNHUB_SOCKET_URL}?token=${BuildConfig.FINNHUB_API_KEY}",
                            ),
                        json = get(),
                    ),
            )
        }
        single<MarketDataSource>(qualifier = named(name = DataConfig.DEMO_SOURCE)) {
            val catalog = DemoInstrumentCatalog()
            DemoMarketDataSource(
                catalog = catalog,
                clock = get(),
                priceStream =
                    DemoPriceStreamSource(
                        catalog = catalog,
                        clock = get(),
                    ),
            )
        }

        single<MarketDataModeRepository> {
            MarketDataModeRepositoryImpl(apiKey = BuildConfig.FINNHUB_API_KEY)
        }
        single {
            MarketDataSelector(
                modeRepository = get(),
                live = get(qualifier = named(name = DataConfig.LIVE_SOURCE)),
                demo = get(qualifier = named(name = DataConfig.DEMO_SOURCE)),
            )
        }
        single<InstrumentSearchRepository> {
            InstrumentSearchRepositoryImpl(selector = get())
        }
        single<WatchlistRepository> {
            WatchlistRepositoryImpl(dao = get(), clock = get())
        }
        single<PriceRepository> {
            PriceRepositoryImpl(
                selector = get(),
                modeRepository = get(),
                dao = get(),
                appLifecycleState = ProcessLifecycleOwner.get().lifecycle.currentStateFlow,
                appScope = get(),
            )
        }
    }
