package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.PrayerEntryEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity

/**
 * Select-all / clear-all / bulk-insert operations used only by backup and restore.
 * Deliberately separate from PrayerDao/GoalDao, whose queries are shaped around the
 * UI's needs (single day, date range, active-only) rather than "everything".
 *
 * Adding this DAO does not change any @Entity or the @Database version — Room's
 * schema identity hash is derived from entities/views only, not DAOs.
 */
@Dao
interface BackupDao {

    // ── Select all (deterministic order so two reads of the same data serialize identically) ──

    @Query("SELECT * FROM prayer_entries WHERE user_id = :userId ORDER BY date ASC, prayer_type ASC")
    suspend fun getAllPrayerEntries(userId: String): List<PrayerEntryEntity>

    @Query("SELECT * FROM spiritual_goals ORDER BY id ASC")
    suspend fun getAllGoals(): List<SpiritualGoalEntity>

    @Query("SELECT id FROM spiritual_goals ORDER BY id ASC")
    suspend fun getAllGoalIds(): List<Long>

    @Query("SELECT * FROM goal_check_ins ORDER BY goal_id ASC, date ASC, id ASC")
    suspend fun getAllCheckIns(): List<GoalCheckInEntity>

    // ── Counts for the shrink guard (cheaper than reading every row) ──

    @Query("SELECT COUNT(*) FROM prayer_entries WHERE user_id = :userId")
    suspend fun countPrayerEntries(userId: String): Int

    @Query("SELECT MIN(date) FROM prayer_entries WHERE user_id = :userId")
    suspend fun oldestPrayerDate(userId: String): String?

    // ── Clear (restore replaces everything; never used outside a withTransaction block) ──

    @Query("DELETE FROM prayer_entries WHERE user_id = :userId")
    suspend fun clearPrayerEntries(userId: String)

    @Query("DELETE FROM spiritual_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM goal_check_ins")
    suspend fun clearCheckIns()

    @Query("DELETE FROM streak_snapshots WHERE user_id = :userId")
    suspend fun clearStreaks(userId: String)

    // ── Bulk insert for restore. Goals and check-ins keep their original ids (nullif(?, 0)
    // in Room's generated SQL preserves a non-zero id instead of autogenerating one). ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerEntries(entries: List<PrayerEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SpiritualGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkIns: List<GoalCheckInEntity>)
}
