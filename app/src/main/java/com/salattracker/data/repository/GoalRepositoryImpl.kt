package com.salattracker.data.repository

import com.salattracker.data.local.dao.GoalDao
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/** Sunday-start week containing [date], matching the Sun–Sat convention used elsewhere in the app. */
private fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

private fun periodStartFor(frequency: GoalFrequency, today: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> today
    GoalFrequency.WEEKLY  -> weekStartOf(today)
    GoalFrequency.MONTHLY -> YearMonth.from(today).atDay(1)
}

/** Wide enough to cover a full month's worth of check-ins for any goal's current period. */
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
        )
    )

    override suspend fun checkIn(goalId: Long, amount: Int) {
        goalDao.insertCheckIn(GoalCheckInEntity(goalId = goalId, date = LocalDate.now(), amount = amount))
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
            )
        )
    }

    override suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteCheckInsForGoal(goalId)
        goalDao.deleteGoal(goalId)
    }

    override suspend fun getGoalCompletionBreakdown(from: LocalDate, to: LocalDate): List<GoalCompletionStat> {
        val goals = goalDao.getActiveGoals()
        if (goals.isEmpty()) return emptyList()

        val checkIns = goalDao.getCheckInsBetween(from, to)
        val daysInRange = ChronoUnit.DAYS.between(from, to) + 1

        return goals.map { entity ->
            val goal = entity.toDomain()
            val periods = when (goal.frequency) {
                GoalFrequency.DAILY   -> daysInRange
                GoalFrequency.WEEKLY  -> ceil(daysInRange / 7.0).toLong()
                GoalFrequency.MONTHLY -> ceil(daysInRange / 30.0).toLong()
            }.coerceAtLeast(1)
            val expectedAmount = periods * goal.targetAmount
            val actualAmount = checkIns.filter { it.goalId == goal.id }.sumOf { it.amount }
            val rate = if (expectedAmount > 0) (actualAmount.toFloat() / expectedAmount).coerceIn(0f, 1f) else 0f
            GoalCompletionStat(goal.name, goal.category, rate)
        }
    }

    override suspend fun getDailyGoalCompletionPoints(from: LocalDate, to: LocalDate): List<GoalDailyPoint> {
        val goals = goalDao.getActiveGoals()
        if (goals.isEmpty()) return emptyList()

        val checkIns = goalDao.getCheckInsBetween(from, to)
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()

        return days.map { day ->
            val rate = goals.map { entity ->
                val amount = checkIns.filter { it.goalId == entity.id && it.date == day }.sumOf { it.amount }
                (amount.toFloat() / entity.targetAmount).coerceIn(0f, 1f)
            }.average().toFloat()
            GoalDailyPoint(day, rate)
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
)
