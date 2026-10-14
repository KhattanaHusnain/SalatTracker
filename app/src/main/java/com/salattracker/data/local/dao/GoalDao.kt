package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class GoalDao {

    @Insert
    abstract suspend fun insertGoal(goal: SpiritualGoalEntity): Long

    @Query("SELECT * FROM spiritual_goals WHERE is_active = 1")
    abstract suspend fun getActiveGoals(): List<SpiritualGoalEntity>

    @Query("SELECT * FROM spiritual_goals WHERE id = :goalId")
    abstract suspend fun getGoal(goalId: Long): SpiritualGoalEntity?

    @Update
    abstract suspend fun updateGoal(goal: SpiritualGoalEntity)

    @Query("DELETE FROM spiritual_goals WHERE id = :goalId")
    abstract suspend fun deleteGoal(goalId: Long)

    @Query("DELETE FROM goal_check_ins WHERE goal_id = :goalId")
    abstract suspend fun deleteCheckInsForGoal(goalId: Long)

    @Insert
    abstract suspend fun insertCheckIn(checkIn: GoalCheckInEntity)

    @Query("SELECT * FROM goal_check_ins WHERE date BETWEEN :from AND :to")
    abstract suspend fun getCheckInsBetween(from: LocalDate, to: LocalDate): List<GoalCheckInEntity>

    // ── History / archive (Goals list + details screen) ──────────────────────────────

    @Query("SELECT * FROM spiritual_goals ORDER BY id ASC")
    abstract fun observeAllGoals(): Flow<List<SpiritualGoalEntity>>

    @Query("SELECT * FROM spiritual_goals WHERE id = :goalId")
    abstract fun observeGoal(goalId: Long): Flow<SpiritualGoalEntity?>

    @Query("SELECT * FROM goal_check_ins ORDER BY date ASC, id ASC")
    abstract fun observeAllCheckIns(): Flow<List<GoalCheckInEntity>>

    @Query("SELECT * FROM goal_check_ins WHERE goal_id = :goalId ORDER BY date ASC, id ASC")
    abstract fun observeCheckInsFor(goalId: Long): Flow<List<GoalCheckInEntity>>

    @Query("DELETE FROM goal_check_ins WHERE goal_id = :goalId AND date = :date")
    abstract suspend fun deleteCheckInsForDay(goalId: Long, date: LocalDate)

    @Query("UPDATE spiritual_goals SET is_active = :active WHERE id = :goalId")
    abstract suspend fun setActive(goalId: Long, active: Boolean)

    /** Atomically replaces everything logged for one day with [entry] (or clears the day when null). */
    @Transaction
    open suspend fun replaceDay(goalId: Long, date: LocalDate, entry: GoalCheckInEntity?) {
        deleteCheckInsForDay(goalId, date)
        if (entry != null) insertCheckIn(entry)
    }
}
