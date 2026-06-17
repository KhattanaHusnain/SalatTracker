package com.salattracker.data.local.dao

import androidx.room.*
import com.salattracker.data.local.entity.*
import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ── Streak DAO ────────────────────────────────────────────────────────────────

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_snapshots WHERE user_id = :userId")
    suspend fun getSnapshot(userId: String): StreakSnapshotEntity?

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

// ── Sync queue DAO ────────────────────────────────────────────────────────────

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC LIMIT :limit")
    suspend fun fetchBatch(limit: Int = 50): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteProcessed(ids: List<Long>)

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id IN (:ids)")
    suspend fun incrementRetry(ids: List<Long>)

    @Query("DELETE FROM sync_queue WHERE retry_count >= :maxRetries")
    suspend fun purgeExhausted(maxRetries: Int = 5)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observeQueueSize(): Flow<Int>
}