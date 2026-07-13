package com.salattracker.domain.repository

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.model.SpiritualGoal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

interface GoalRepository {

    /** Active goals with their current period's progress — live, re-emits on any check-in or edit. */
    fun observeActiveGoalsWithProgress(): Flow<List<GoalWithProgress>>

    suspend fun createGoal(
        name: String,
        category: GoalCategory,
        frequency: GoalFrequency,
        targetAmount: Int,
        unit: String,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        durationDays: Int,
    ): Long

    /** Records one check-in of [amount] (default 1) against [goalId] for the current period. */
    suspend fun checkIn(goalId: Long, amount: Int = 1)

    suspend fun getAllActiveGoals(): List<SpiritualGoal>

    suspend fun getGoal(goalId: Long): SpiritualGoal?

    suspend fun updateGoal(
        goalId: Long,
        name: String,
        category: GoalCategory,
        frequency: GoalFrequency,
        targetAmount: Int,
        unit: String,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        durationDays: Int,
    )

    /** Removes the goal and all of its check-in history. */
    suspend fun deleteGoal(goalId: Long)

    /** Per-goal check-in completion rate over [from]..[to], for the Stats bar chart. */
    suspend fun getGoalCompletionBreakdown(from: LocalDate, to: LocalDate): List<GoalCompletionStat>

    /** Average across-goals completion rate for each day in [from]..[to], for the Stats "Week" bar chart. */
    suspend fun getDailyGoalCompletionPoints(from: LocalDate, to: LocalDate): List<GoalDailyPoint>
}
