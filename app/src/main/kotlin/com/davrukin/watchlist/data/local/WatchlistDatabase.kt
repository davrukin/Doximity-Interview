package com.davrukin.watchlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistItemEntity::class],
    version = 1,
)
abstract class WatchlistDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
