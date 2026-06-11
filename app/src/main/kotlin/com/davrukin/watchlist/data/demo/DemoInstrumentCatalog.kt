package com.davrukin.watchlist.data.demo

import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType

/**
 * Canned instruments with base prices that anchor the demo random walk.
 */
class DemoInstrumentCatalog {
    data class Entry(
        val instrument: Instrument,
        val basePrice: Double,
    )

    val entries: List<Entry> =
        listOf(
            stock(symbol = "AAPL", description = "APPLE INC", basePrice = 228.40),
            stock(symbol = "MSFT", description = "MICROSOFT CORP", basePrice = 512.70),
            stock(symbol = "GOOGL", description = "ALPHABET INC-CL A", basePrice = 196.25),
            stock(symbol = "AMZN", description = "AMAZON.COM INC", basePrice = 231.10),
            stock(symbol = "NVDA", description = "NVIDIA CORP", basePrice = 172.85),
            stock(symbol = "TSLA", description = "TESLA INC", basePrice = 318.50),
            stock(symbol = "META", description = "META PLATFORMS INC-CLASS A", basePrice = 742.30),
            stock(symbol = "JPM", description = "JPMORGAN CHASE & CO", basePrice = 296.60),
            crypto(pair = "BTCUSDT", display = "BTC/USDT", description = "Binance BTCUSDT", basePrice = 104250.0),
            crypto(pair = "ETHUSDT", display = "ETH/USDT", description = "Binance ETHUSDT", basePrice = 3920.0),
            crypto(pair = "SOLUSDT", display = "SOL/USDT", description = "Binance SOLUSDT", basePrice = 218.40),
            crypto(pair = "XRPUSDT", display = "XRP/USDT", description = "Binance XRPUSDT", basePrice = 2.86),
            crypto(pair = "ADAUSDT", display = "ADA/USDT", description = "Binance ADAUSDT", basePrice = 1.14),
            crypto(pair = "DOGEUSDT", display = "DOGE/USDT", description = "Binance DOGEUSDT", basePrice = 0.31),
        )

    fun search(query: String): List<Instrument> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }
        return entries
            .map { entry ->
                entry.instrument
            }.filter { instrument ->
                instrument.displaySymbol.contains(other = trimmed, ignoreCase = true) ||
                    instrument.description.contains(other = trimmed, ignoreCase = true)
            }
    }

    fun basePrice(symbol: String): Double =
        entries
            .firstOrNull { entry ->
                entry.instrument.symbol == symbol
            }?.basePrice ?: Double.NaN

    private fun stock(
        symbol: String,
        description: String,
        basePrice: Double,
    ): Entry =
        Entry(
            instrument =
                Instrument(
                    symbol = symbol,
                    displaySymbol = symbol,
                    description = description,
                    type = InstrumentType.STOCK,
                ),
            basePrice = basePrice,
        )

    private fun crypto(
        pair: String,
        display: String,
        description: String,
        basePrice: Double,
    ): Entry =
        Entry(
            instrument =
                Instrument(
                    symbol = "BINANCE:$pair",
                    displaySymbol = display,
                    description = description,
                    type = InstrumentType.CRYPTO,
                ),
            basePrice = basePrice,
        )
}
