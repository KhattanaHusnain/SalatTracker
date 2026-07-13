package com.salattracker.data.local.dao

import androidx.room.*
import com.salattracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ── Streak DAO ────────────────────────────────────────────────────────────────

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_snapshots WHERE user_id = :userId")
    suspend fun getAllSnapshots(userId: String): List<StreakSnapshotEntity>

    // Room invalidates this automatically on ANY write to streak_snapshots, from
    // any screen/worker — needed so Home's tiles don't go stale when a prayer is
    // marked from Calendar, Qaza, or the auto-Qaza background worker.
    @Query("SELECT * FROM streak_snapshots WHERE user_id = :userId")
    fun observeAllSnapshots(userId: String): Flow<List<StreakSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: StreakSnapshotEntity)
}

// ── Prayer times cache DAO ────────────────────────────────────────────────────

@Dao
interface PrayerTimesDao {
    @Query("""
        SELECT * FROM prayer_times_cache
        WHERE date = :date AND latitude = :lat AND longitude = :lon AND method = :method
        LIMIT 1
    """)
    suspend fun getCached(
        date: LocalDate,
        lat: Double,
        lon: Double,
        method: Int,
    ): PrayerTimeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: PrayerTimeCacheEntity)

    @Query("DELETE FROM prayer_times_cache WHERE cached_at < :before")
    suspend fun evictOlderThan(before: Long)
}

// ── Verse cache DAO ───────────────────────────────────────────────────────────

@Dao
interface VerseDao {
    @Query("SELECT * FROM verse_cache")
    suspend fun getAll(): List<VerseCacheEntity>

    @Query("SELECT verse_key FROM verse_cache")
    suspend fun getCachedKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VerseCacheEntity)
}