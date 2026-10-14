package com.salattracker.data.repository

import com.salattracker.data.local.dao.GoalDao
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.daysOfWeekFromMask
import com.salattracker.domain.usecase.goalPeriodStat
import com.salattracker.domain.usecase.periodStartFor
import com.salattracker.domain.usecase.toDaysMask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Wide enough to cover a full month-long goal period straddling the start of any Stats range. */
private const val LOOKBACK_DAYS = 31L

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
) : GoalRepository {

    override fun observeActiveGoalsWithProgress(): Flow<List<GoalWithProgress>> {
        val since = LocalDate.now().minusDays(LOOKBACK_DAYS)
        return goalDao.observeActiveGoals().combine(goalDao.observeCheckInsSince(since)) { goals, checkIns ->
            val today = LocalDate.now()
            goals.map { entity ->
                val goal = entity.toDomain()
                val periodStart = periodStartFor(goal.frequency, today)
                val currentAmount = checkIns
                    .filter { it.goalId == goal.id && !it.date.isBefore(periodStart) }
                    .sumOf { it.amount }
                GoalWithProgress(goal, currentAmount)
            }
        }
    }

    override suspend fun createGoal(
        name: String,
        category: GoalCategory,
        frequency: GoalFrequency,
        targetAmount: Int,
        unit: String,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        durationDays: Int,
        daysOfWeek: Set<DayOfWeek>,
        endsAfterDuration: Boolean,
    ): Long = goalDao.insertGoal(
        SpiritualGoalEntity(
            name            = name,
            category        = category,
            frequency       = frequency,
            targetAmount    = targetAmount,
            unit            = unit,
            reminderEnabled = reminderEnabled,
            reminderTime    = reminderTime,
            startDate       = LocalDate.now(),
            durationDays    = durationDays,
            daysOfWeek      = daysOfWeek.toDaysMask(),
            endsAfterDuration = endsAfterDuration,
        )
    )

    override suspend fun checkIn(goalId: Long, amount: Int, date: LocalDate) {
        goalDao.insertCheckIn(GoalCheckInEntity(goalId = goalId, date = date, amount = amount))
    }

    override suspend fun setDayLog(goalId: Long, date: LocalDate, amount: Int, skipped: Boolean) {
        val entry = when {
            skipped     -> GoalCheckInEntity(goalId = goalId, date = date, amount = 0, isSkip = true)
            amount > 0  -> GoalCheckInEntity(goalId = goalId, date = date, amount = amount)
            else        -> null
        }
        goalDao.replaceDay(goalId, date, entry)
    }

    override suspend fun setArchived(goalId: Long, archived: Boolean) {
        goalDao.setActive(goalId, !archived)
    }

    override fun observeGoalHistories(): Flow<List<GoalHistory>> =
        goalDao.observeAllGoals().combine(goalDao.observeAllCheckIns()) { goals, checkIns ->
            val byGoal = checkIns.groupBy { it.goalId }
            goals.map { entity ->
                GoalHistory(entity.toDomain(), byGoal[entity.id].orEmpty().map { it.toDomain() })
            }
        }

    override fun observeGoalHistory(goalId: Long): Flow<GoalHistory?> =
        goalDao.observeGoal(goalId).combine(goalDao.observeCheckInsFor(goalId)) { goal, checkIns ->
            goal?.let { GoalHistory(it.toDomain(), checkIns.map { c -> c.toDomain() }) }
        }

    override suspend fun getAllActiveGoals(): List<SpiritualGoal> =
        goalDao.getActiveGoals().map { it.toDomain() }

    override suspend fun getGoal(goalId: Long): SpiritualGoal? =
        goalDao.getGoal(goalId)?.toDomain()

    override suspend fun updateGoal(
        goalId: Long,
        name: String,
        category: GoalCategory,
        frequency: GoalFrequency,
        targetAmount: Int,
        unit: String,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?,
        durationDays: Int,
        daysOfWeek: Set<DayOfWeek>,
        endsAfterDuration: Boolean,
    ) {
        val existing = goalDao.getGoal(goalId) ?: return
        goalDao.updateGoal(
            existing.copy(
                name            = name,
                category        = category,
                frequency       = frequency,
                targetAmount    = targetAmount,
                unit            = unit,
                reminderEnabled = reminderEnabled,
                reminderTime    = reminderTime,
                durationDays    = durationDays,
                daysOfWeek      = daysOfWeek.toDaysMask(),
                endsAfterDuration = endsAfterDuration,
            )
        )
    }

    override suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteCheckInsForGoal(goalId)
        goalDao.deleteGoal(goalId)
    }

    override suspend fun getGoalPeriodStats(from: LocalDate, to: LocalDate): List<GoalPeriodStat> {
        val goals = goalDao.getActiveGoals()
        if (goals.isEmpty()) return emptyList()

        // Fetch from well before [from] so a weekly/monthly goal's period straddling the start
        // of the range is judged on its full total, not just the slice inside [from, to].
        val checkIns = goalDao.getCheckInsBetween(from.minusDays(LOOKBACK_DAYS), to)
        return goals.map { it.toDomain() }
            // Goals that only existed outside the range have nothing to report for it.
            .filter { goal -> !goal.startDate.isAfter(to) && goal.endDate?.isBefore(from) != true }
            .map { goal ->
                val goalCheckIns = checkIns.filter { it.goalId == goal.id }.map { it.toDomain() }
                goalPeriodStat(goal, goalCheckIns, from, to)
            }
    }
}

private fun SpiritualGoalEntity.toDomain() = SpiritualGoal(
    id              = id,
    name            = name,
    category        = category,
    frequency       = frequency,
    targetAmount    = targetAmount,
    unit            = unit,
    reminderEnabled = reminderEnabled,
    reminderTime    = reminderTime,
    startDate       = startDate,
    durationDays    = durationDays,
    isActive        = isActive,
    daysOfWeek      = daysOfWeekFromMask(daysOfWeek),
    endsAfterDuration = endsAfterDuration,
)

private fun GoalCheckInEntity.toDomain() = GoalCheckIn(date = date, amount = amount, isSkip = isSkip)
