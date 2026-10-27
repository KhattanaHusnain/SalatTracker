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

/** v8 -> v9: the Quran feature's per-device user-data tables — bookmarks, read history (whose
 * most recent row also serves as "continue reading"), search history, and the registry of which
 * downloadable content packs (translations/tafsirs/word-by-word/recitations) are on this device.
 * Purely additive. The Quran text itself and the content those packs unlock live in their own
 * separate database(s), not here — see data/local/quran/, added alongside the reading UI. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quran_bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`chapter_number` INTEGER NOT NULL, `from_verse` INTEGER NOT NULL, `to_verse` INTEGER NOT NULL, " +
                "`note` TEXT, `created_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quran_read_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`chapter_number` INTEGER NOT NULL, `verse_number` INTEGER NOT NULL, `read_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quran_search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`query` TEXT NOT NULL, `searched_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quran_content_packs` (`pack_id` TEXT NOT NULL PRIMARY KEY, " +
                "`type` TEXT NOT NULL, `language` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT, " +
                "`size_bytes` INTEGER NOT NULL, `source_url` TEXT NOT NULL, `version` INTEGER NOT NULL, " +
                "`is_downloaded` INTEGER NOT NULL DEFAULT 0, `local_path` TEXT, `downloaded_at` INTEGER)"
        )
    }
}

/** v9 -> v10: drops `verse_cache` — the verse-of-the-day feature now reads straight from the
 * locally-seeded Quran data (QuranCoreDatabase + the default translation pack) instead of
 * fetching from the Quran.com API and caching the result here. It only ever held re-fetchable
 * derived data (nothing the user entered), so there's nothing to migrate out of it. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `verse_cache`")
    }
}

/** v10 -> v11: a read can now represent a bounded Juz Mushaf page, not only a Surah ayah. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quran_read_history ADD COLUMN juz_number INTEGER")
        db.execSQL("ALTER TABLE quran_read_history ADD COLUMN page_number INTEGER")
    }
}

/** v11 -> v12: persistent Tasbih rounds and an explicit goal opt-in. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spiritual_goals ADD COLUMN uses_tasbih_counter INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS tasbih_sessions (" +
                "session_id TEXT NOT NULL PRIMARY KEY, goal_id INTEGER, phrase_id TEXT NOT NULL, " +
                "target INTEGER NOT NULL, count INTEGER NOT NULL, is_completed INTEGER NOT NULL DEFAULT 0, " +
                "completed_on TEXT)"
        )
    }
}

/** v12 -> v13: records Calendar day-sheet edits as late so the label survives relaunch/backup. */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE prayer_entries ADD COLUMN logged_late INTEGER NOT NULL DEFAULT 0")
    }
}

/** v13 -> v14: private Qaza reasons plus the append-only activity ledger used by Qaza v10. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE prayer_entries ADD COLUMN qaza_reason TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS qaza_activity (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "user_id TEXT NOT NULL, prayer_date TEXT NOT NULL, prayer_type TEXT NOT NULL, " +
                "previous_status TEXT, new_status TEXT NOT NULL, reason_tag TEXT, source TEXT NOT NULL, " +
                "recorded_at INTEGER NOT NULL, batch_id TEXT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_qaza_activity_user_id_recorded_at ON qaza_activity (user_id, recorded_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_qaza_activity_user_id_prayer_date_prayer_type ON qaza_activity (user_id, prayer_date, prayer_type)")
    }
}
