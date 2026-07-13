package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GoalDao {

    @Insert
    suspend fun insertGoal(goal: SpiritualGoalEntity): Long

    @Query("SELECT * FROM spiritual_goals WHERE is_active = 1")
    fun observeActiveGoals(): Flow<List<SpiritualGoalEntity>>

    @Query("SELECT * FROM spiritual_goals WHERE is_active = 1")
    suspend fun getActiveGoals(): List<SpiritualGoalEntity>

    @Query("SELECT * FROM spiritual_goals WHERE id = :goalId")
    suspend fun getGoal(goalId: Long): SpiritualGoalEntity?

    @Update
    suspend fun updateGoal(goal: SpiritualGoalEntity)

    @Query("DELETE FROM spiritual_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)

    @Query("DELETE FROM goal_check_ins WHERE goal_id = :goalId")
    suspend fun deleteCheckInsForGoal(goalId: Long)

    @Insert
    suspend fun insertCheckIn(checkIn: GoalCheckInEntity)

    // Room invalidates this automatically on any check-in insert, from any screen/worker.
    @Query("SELECT * FROM goal_check_ins WHERE date >= :since")
    fun observeCheckInsSince(since: LocalDate): Flow<List<GoalCheckInEntity>>

    @Query("SELECT * FROM goal_check_ins WHERE date BETWEEN :from AND :to")
    suspend fun getCheckInsBetween(from: LocalDate, to: LocalDate): List<GoalCheckInEntity>
}
