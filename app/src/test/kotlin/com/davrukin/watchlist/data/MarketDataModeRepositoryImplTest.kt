package com.davrukin.watchlist.data

import com.davrukin.watchlist.domain.model.MarketDataMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataModeRepositoryImplTest {
    @Test
    fun `starts live and toggles when an api key is configured`() {
        val repository = MarketDataModeRepositoryImpl(apiKey = "key")

        assertTrue(repository.isLiveAvailable)
        assertEquals(MarketDataMode.LIVE, repository.mode.value)

        repository.toggle()
        assertEquals(MarketDataMode.DEMO, repository.mode.value)

        repository.toggle()
        assertEquals(MarketDataMode.LIVE, repository.mode.value)
    }

    @Test
    fun `locks to demo mode without an api key`() {
        val repository = MarketDataModeRepositoryImpl(apiKey = "")

        assertFalse(repository.isLiveAvailable)
        assertEquals(MarketDataMode.DEMO, repository.mode.value)

        repository.toggle()
        assertEquals(MarketDataMode.DEMO, repository.mode.value)
    }
}
