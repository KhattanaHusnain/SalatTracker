package com.salattracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

// ── Goal category / frequency ─────────────────────────────────────────────────

enum class GoalCategory { SALAT, QURAN, FASTING, CHARITY, DHIKR }

enum class GoalFrequency { DAILY, WEEKLY, MONTHLY }

// ── Domain model (no Room imports) ────────────────────────────────────────────

data class SpiritualGoal(
    val id: Long,
    val name: String,
    val category: GoalCategory,
    val frequency: GoalFrequency,
    val targetAmount: Int,
    val unit: String,               // "Time(s)" | "Minutes" | "Pages"
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val startDate: LocalDate,
    val durationDays: Int,
    val isActive: Boolean,
)

/** A goal plus how much progress has been logged in its current period (today/this week/this month). */
data class GoalWithProgress(
    val goal: SpiritualGoal,
    val currentAmount: Int,
) {
    val progressFraction: Float get() = if (goal.targetAmount <= 0) 0f else
        (currentAmount.toFloat() / goal.targetAmount).coerceIn(0f, 1f)
    val isComplete: Boolean get() = currentAmount >= goal.targetAmount
}

/** One check-in event, for Stats to replay against a goal's own day/week/month periods. */
data class GoalCheckIn(
    val date: LocalDate,
    val amount: Int,
)

/**
 * How a goal did over an arbitrary Stats date range, judged by the goal's own frequency rather
 * than averaged per day — see [com.salattracker.domain.usecase.goalPeriodStat]. [periodsCounted]
 * is 0 (and [periodsHit] meaningless) when the goal didn't exist yet anywhere in the range.
 */
data class GoalPeriodStat(
    val goalName: String,
    val category: GoalCategory,
    val frequency: GoalFrequency,
    val unit: String,
    val loggedAmount: Int,
    val periodsHit: Int,
    val periodsCounted: Int,
)
