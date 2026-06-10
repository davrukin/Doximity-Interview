package com.davrukin.watchlist.data.remote

import com.davrukin.watchlist.common.resultOf
import com.davrukin.watchlist.data.MarketDataSource
import com.davrukin.watchlist.data.stream.PriceStreamSource
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.domain.model.Quote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LiveMarketDataSource(
    private val api: FinnhubApi,
    override val priceStream: PriceStreamSource,
) : MarketDataSource {
    private val cryptoCatalogMutex = Mutex()
    private var cryptoCatalog: List<Instrument>? = null

    override suspend fun search(query: String): Result<List<Instrument>> =
        resultOf {
            coroutineScope {
                val stocks =
                    async {
                        api.search(query = query).result.map { result ->
                            result.toInstrument()
                        }
                    }
                val crypto = async {
                    searchCrypto(query = query)
                }
                stocks.await().take(n = MAX_STOCK_RESULTS) + crypto.await().take(n = MAX_CRYPTO_RESULTS)
            }
        }

    override suspend fun quoteSnapshot(instrument: Instrument): Result<Quote?> =
        when (instrument.type) {
            InstrumentType.STOCK -> resultOf {
                api.quote(symbol = instrument.symbol).toQuote()
            }
            // Finnhub has no REST quote for crypto; the first streamed tick fills the price in.
            InstrumentType.CRYPTO -> Result.success(value = null)
        }

    private suspend fun searchCrypto(query: String): List<Instrument> {
        val catalog = cryptoCatalogMutex.withLock {
            cryptoCatalog ?: api
                .cryptoSymbols(exchange = BINANCE_EXCHANGE)
                .map { symbol ->
                    symbol.toInstrument()
                }
                .filter { instrument ->
                    instrument.displaySymbol.endsWith(suffix = USDT_SUFFIX, ignoreCase = true)
                }
                .also { instruments ->
                    cryptoCatalog = instruments
                }
        }
        return catalog.filter { instrument ->
            instrument.displaySymbol.contains(other = query, ignoreCase = true) ||
                instrument.description.contains(other = query, ignoreCase = true)
        }
    }

    companion object {
        private const val BINANCE_EXCHANGE = "BINANCE"
        private const val USDT_SUFFIX = "USDT"
        private const val MAX_STOCK_RESULTS = 15
        private const val MAX_CRYPTO_RESULTS = 10
    }
}
