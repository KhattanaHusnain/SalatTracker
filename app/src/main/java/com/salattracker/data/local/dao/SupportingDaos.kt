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