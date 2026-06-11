package com.davrukin.watchlist.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.davrukin.watchlist.data.local.WatchlistDatabase
import com.davrukin.watchlist.domain.model.WatchlistItem
import com.davrukin.watchlist.testing.instrument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Regression net for the silent-persistence failure class: SQLite stores NaN as NULL, and with
 * INSERT OR IGNORE a NOT NULL violation drops the row without any exception. These tests run the
 * real Room database through the repository's public API, so any schema or sentinel-mapping
 * change that silently loses data fails here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WatchlistRepositoryRoundTripTest {
    private lateinit var database: WatchlistDatabase
    private lateinit var repository: WatchlistRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    WatchlistDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository =
            WatchlistRepositoryImpl(
                dao = database.watchlistDao(),
                clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `added instrument is observable through the repository`() {
        runTest {
            repository.add(instrument = instrument(symbol = "AAPL"))

            val items: List<WatchlistItem> = repository.observeWatchlist().first()
            assertEquals(listOf("AAPL"), items.map { it.instrument.symbol })
            assertNull(items.single().cachedQuote)
        }
    }

    @Test
    fun `quote with NaN change survives the persistence round trip`() {
        runTest {
            repository.add(instrument = instrument(symbol = "BINANCE:BTCUSDT"))

            database.watchlistDao().updateQuote(
                symbol = "BINANCE:BTCUSDT",
                price = 104_250.0,
                change = null,
                percentChange = null,
                updatedAtEpochMillis = 1_700_000_000_000,
            )

            val cached =
                repository
                    .observeWatchlist()
                    .first()
                    .single()
                    .cachedQuote
            requireNotNull(cached)
            assertEquals(104_250.0, cached.price, 0.0)
            assertTrue(cached.change.isNaN())
            assertTrue(cached.percentChange.isNaN())
            assertTrue(cached.isStale)
        }
    }

    @Test
    fun `remove deletes the row and its cached quote`() {
        runTest {
            repository.add(instrument = instrument(symbol = "AAPL"))
            repository.remove(symbol = "AAPL")

            assertTrue(repository.observeWatchlist().first().isEmpty())
        }
    }
}
