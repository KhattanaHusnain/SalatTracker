package com.salattracker.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6: goal schedules (specific weekdays), optional end-after-duration, and excused
 * ("skipped") check-in days. Purely additive, so existing goals become every-day, ongoing goals
 * and existing check-ins stay normal — no data is lost.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spiritual_goals ADD COLUMN days_of_week INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE spiritual_goals ADD COLUMN ends_after_duration INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goal_check_ins ADD COLUMN is_skip INTEGER NOT NULL DEFAULT 0")
    }
}
