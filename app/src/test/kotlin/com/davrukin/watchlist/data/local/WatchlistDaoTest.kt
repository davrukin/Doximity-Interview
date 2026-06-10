package com.davrukin.watchlist.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.davrukin.watchlist.domain.model.InstrumentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WatchlistDaoTest {
    private lateinit var database: WatchlistDatabase
    private lateinit var dao: WatchlistDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext<Context>(),
                    WatchlistDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inserts observes and deletes in added order`() =
        runTest {
            dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))
            dao.insert(entity = entity(symbol = "MSFT", addedAt = 2))

            assertEquals(listOf("AAPL", "MSFT"), dao.observeAll().first().map { it.symbol })

            dao.delete(symbol = "AAPL")
            assertEquals(listOf("MSFT"), dao.observeAll().first().map { it.symbol })
        }

    @Test
    fun `updates the cached quote in place`() =
        runTest {
            dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))

            dao.updateQuote(
                symbol = "AAPL",
                price = 230.5,
                change = 2.1,
                percentChange = 0.9,
                updatedAtEpochMillis = 1_700_000_000_000,
            )

            val updated = dao.observeAll().first().single()
            assertEquals(230.5, requireNotNull(updated.lastPrice), 0.0)
            assertEquals(1_700_000_000_000, requireNotNull(updated.lastUpdatedEpochMillis))
        }

    @Test
    fun `re-inserting an existing symbol keeps the cached quote`() =
        runTest {
            dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))
            dao.updateQuote(
                symbol = "AAPL",
                price = 230.5,
                change = null,
                percentChange = null,
                updatedAtEpochMillis = 1,
            )

            dao.insert(entity = entity(symbol = "AAPL", addedAt = 99))

            val kept = dao.observeAll().first().single()
            assertEquals(230.5, requireNotNull(kept.lastPrice), 0.0)
            assertEquals(1, kept.addedAtEpochMillis)
        }

    @Test
    fun `starts empty`() =
        runTest {
            assertTrue(dao.observeAll().first().isEmpty())
        }

    private fun entity(
        symbol: String,
        addedAt: Long,
    ): WatchlistItemEntity =
        WatchlistItemEntity(
            symbol = symbol,
            displaySymbol = symbol,
            description = "$symbol description",
            type = InstrumentType.STOCK,
            addedAtEpochMillis = addedAt,
        )
}
