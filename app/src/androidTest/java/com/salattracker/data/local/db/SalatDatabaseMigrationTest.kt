package com.salattracker.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guards the upgrade path used by Play Store installs that started on database v7. */
@RunWith(AndroidJUnit4::class)
class SalatDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SalatDatabase::class.java,
    )

    @Test
    fun migrateFrom7To13_preservesPrayerAndGoalData() {
        helper.createDatabase(TEST_DB_NAME, 7).apply {
            execSQL(
                "INSERT INTO prayer_entries " +
                    "(user_id, date, prayer_type, status, updated_at) VALUES " +
                    "('migration-user', '2026-10-24', 'FAJR', 'INDIVIDUAL', 123456789)",
            )
            execSQL(
                "INSERT INTO spiritual_goals " +
                    "(name, category, frequency, target_amount, unit, reminder_enabled, reminder_time, " +
                    "start_date, duration_days, is_active, days_of_week, ends_after_duration) VALUES " +
                    "('Read Quran', 'QURAN', 'DAILY', 1, 'page', 0, NULL, '2026-10-24', 0, 1, 0, 0)",
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            13,
            true,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
        )

        database.query("SELECT status, logged_late FROM prayer_entries WHERE user_id = 'migration-user'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("INDIVIDUAL", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        database.query("SELECT name, uses_tasbih_counter FROM spiritual_goals WHERE name = 'Read Quran'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Read Quran", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        database.close()
    }

    private companion object {
        const val TEST_DB_NAME = "salat-database-migration-test"
    }
}
