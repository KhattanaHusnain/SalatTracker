package com.salattracker.domain.repository

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.SpiritualGoal
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

interface GoalRepository {

    suspend fun createGoal(
        name: String,
        category: GoalCategory,
        frequency: GoalFrequency,
        targetAmount: Int,
        unit: String,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        durationDays: Int,
        daysOfWeek: Set<DayOfWeek> = emptySet(),
        endsAfterDuration: Boolean = false,
    ): Long

    /** Adds a check-in of [amount] (default 1) to [goalId] on [date] (default today). */
    suspend fun checkIn(goalId: Long, amount: Int = 1, date: LocalDate = LocalDate.now())

    /** Replaces everything logged for [date]: an excused day when [skipped], cleared when [amount] <= 0. */
    suspend fun setDayLog(goalId: Long, date: LocalDate, amount: Int, skipped: Boolean)

    /** Archived goals keep their history but leave the active list and stop reminding. */
    suspend fun setArchived(goalId: Long, archived: Boolean)

    /** Every goal (archived and finished included) with its full check-in history — live. */
    fun observeGoalHistories(): Flow<List<GoalHistory>>

    fun observeGoalHistory(goalId: Long): Flow<GoalHistory?>

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
        daysOfWeek: Set<DayOfWeek> = emptySet(),
        endsAfterDuration: Boolean = false,
    )

    /** Removes the goal and all of its check-in history. */
    suspend fun deleteGoal(goalId: Long)

    /**
     * Per-goal Stats over [from]..[to] — see [com.salattracker.domain.usecase.goalPeriodStat] for
     * how a goal's own day/week/month periods are judged.
     */
    suspend fun getGoalPeriodStats(from: LocalDate, to: LocalDate): List<GoalPeriodStat>
}
