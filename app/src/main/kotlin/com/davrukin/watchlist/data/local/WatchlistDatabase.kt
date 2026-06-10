package com.davrukin.watchlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchlistItemEntity::class],
    version = 2,
)
abstract class WatchlistDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite doesn't support changing column constraints (NULL to NOT NULL) directly.
                // We must use the create-copy-drop-rename pattern.
                db.execSQL(
                    """
                    CREATE TABLE watchlist_new (
                        symbol TEXT NOT NULL PRIMARY KEY,
                        displaySymbol TEXT NOT NULL,
                        description TEXT NOT NULL,
                        type TEXT NOT NULL,
                        addedAtEpochMillis INTEGER NOT NULL,
                        lastPrice REAL NOT NULL DEFAULT 'NaN',
                        lastChange REAL NOT NULL DEFAULT 'NaN',
                        lastPercentChange REAL NOT NULL DEFAULT 'NaN',
                        lastUpdatedEpochMillis INTEGER NOT NULL DEFAULT -1
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO watchlist_new (
                        symbol, displaySymbol, description, type, addedAtEpochMillis,
                        lastPrice, lastChange, lastPercentChange, lastUpdatedEpochMillis
                    )
                    SELECT 
                        symbol, displaySymbol, description, type, addedAtEpochMillis,
                        COALESCE(lastPrice, 'NaN'), 
                        COALESCE(lastChange, 'NaN'), 
                        COALESCE(lastPercentChange, 'NaN'), 
                        COALESCE(lastUpdatedEpochMillis, -1)
                    FROM watchlist
                    """.trimIndent(),
                )

                db.execSQL("DROP TABLE watchlist")
                db.execSQL("ALTER TABLE watchlist_new RENAME TO watchlist")
            }
        }
    }
}
