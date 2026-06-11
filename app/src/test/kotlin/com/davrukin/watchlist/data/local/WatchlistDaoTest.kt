package com.davrukin.watchlist.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.davrukin.watchlist.domain.model.InstrumentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
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
                    ApplicationProvider.getApplicationContext(),
                    WatchlistDatabase::class.java,
                ).setQueryExecutor { it.run() }
                .setTransactionExecutor { it.run() }
                .allowMainThreadQueries()
                .build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inserts observes and deletes in added order`() {
        runTest {
            dao.observeAll().test {
                assertEquals(emptyList<WatchlistItemEntity>(), awaitItem())

                dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))
                assertEquals(listOf("AAPL"), awaitItem().map { it.symbol })

                dao.insert(entity = entity(symbol = "MSFT", addedAt = 2))
                assertEquals(listOf("AAPL", "MSFT"), awaitItem().map { it.symbol })

                dao.delete(symbol = "AAPL")
                assertEquals(listOf("MSFT"), awaitItem().map { it.symbol })

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `updates the cached quote in place`() {
        runTest {
            dao.observeAll().test {
                assertEquals(emptyList<WatchlistItemEntity>(), awaitItem())

                dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))
                assertEquals(1, awaitItem().size)

                dao.updateQuote(
                    symbol = "AAPL",
                    price = 230.5,
                    change = 2.1,
                    percentChange = 0.9,
                    updatedAtEpochMillis = 1_700_000_000_000,
                )

                val updated: WatchlistItemEntity = awaitItem().single()
                assertEquals(230.5, requireNotNull(value = updated.lastPrice), 0.0)
                assertEquals(1_700_000_000_000, requireNotNull(value = updated.lastUpdatedEpochMillis))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `re-inserting an existing symbol keeps the cached quote`() {
        runTest {
            dao.insert(entity = entity(symbol = "AAPL", addedAt = 1))
            dao.updateQuote(
                symbol = "AAPL",
                price = 230.5,
                change = null,
                percentChange = null,
                updatedAtEpochMillis = 1,
            )

            // The conflicting insert is IGNOREd: no write happens, so there is no flow
            // re-emission to await — the outcome is asserted with a fresh query.
            dao.insert(entity = entity(symbol = "AAPL", addedAt = 99))

            val kept: WatchlistItemEntity = dao.observeAll().first().single()
            assertEquals(230.5, requireNotNull(value = kept.lastPrice), 0.0)
            assertEquals(1, kept.addedAtEpochMillis)
        }
    }

    @Test
    fun `starts empty`() {
        runTest {
            dao.observeAll().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
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
