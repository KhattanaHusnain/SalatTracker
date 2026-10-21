package com.salattracker.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6: goal schedules (specific weekdays), optional end-after-duration, and excused
 * ("skipped") check-in days. Purely additive, so existing goals become every-day, ongoing goals
 * and existing check-ins stay normal - no data is lost.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spiritual_goals ADD COLUMN days_of_week INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE spiritual_goals ADD COLUMN ends_after_duration INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goal_check_ins ADD COLUMN is_skip INTEGER NOT NULL DEFAULT 0")
    }
}

/** v6 -> v7: the Mosque Mode table. Purely additive — no existing data is touched. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mosques` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, " +
                "`radius_meters` INTEGER NOT NULL)"
        )
    }
}

/** v7 -> v8: prayer times are now calculated on-device (adhan-kotlin) instead of fetched and
 * cached from the AlAdhan API - the cache table is no longer read or written anywhere, so it's
 * dropped. It only ever held derived data (nothing the user entered), so there's nothing to
 * migrate into the new calculation; the next read simply recomputes. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `prayer_times_cache`")
    }
}
