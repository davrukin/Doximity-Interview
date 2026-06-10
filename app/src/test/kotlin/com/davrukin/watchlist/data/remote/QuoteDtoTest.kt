package com.davrukin.watchlist.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class QuoteDtoTest {
    @Test
    fun `maps populated payload to quote`() {
        val dto =
            QuoteDto(
                currentPrice = 228.4,
                change = 1.2,
                percentChange = 0.53,
                timestampEpochSeconds = 1_765_000_000,
            )

        val quote = requireNotNull(dto.toQuote())

        assertEquals(228.4, quote.price, 0.0)
        assertEquals(1.2, requireNotNull(quote.change), 0.0)
        assertEquals(0.53, requireNotNull(quote.percentChange), 0.0)
        assertEquals(Instant.ofEpochSecond(1_765_000_000), quote.lastUpdated)
        assertEquals(false, quote.isStale)
    }

    @Test
    fun `maps zeroed payload to null`() {
        val dto = QuoteDto(currentPrice = 0.0, timestampEpochSeconds = 0)

        assertNull(dto.toQuote())
    }
}
