package com.davrukin.watchlist.data.demo

import com.davrukin.watchlist.data.MarketDataSource
import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.Quote
import java.time.Clock
import kotlin.random.Random

class DemoMarketDataSource(
    private val catalog: DemoInstrumentCatalog,
    private val clock: Clock,
    private val random: Random = Random.Default,
    override val priceStream: PriceStreamSource,
) : MarketDataSource {
    override suspend fun search(query: String): Result<List<Instrument>> {
        return Result.success(catalog.search(query = query))
    }

    override suspend fun quoteSnapshot(instrument: Instrument): Result<Quote?> {
        val base: Double = catalog.basePrice(symbol = instrument.symbol)
        if (base.isNaN()) {
            return Result.success(value = null)
        }
        val price: Double = base * (1 + random.nextDouble(from = -SNAPSHOT_SPREAD, until = SNAPSHOT_SPREAD))
        val change: Double = price - base
        return Result.success(
            value = Quote(
                price = price,
                change = change,
                percentChange = change / base * 100,
                lastUpdated = clock.instant(),
                isStale = false,
            ),
        )
    }

    companion object {
        private const val SNAPSHOT_SPREAD = 0.01
    }
}
