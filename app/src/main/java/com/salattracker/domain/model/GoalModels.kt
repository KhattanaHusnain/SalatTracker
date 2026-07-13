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

/** A goal's check-in completion rate over an arbitrary date range, for the Stats bar chart. */
data class GoalCompletionStat(
    val goalName: String,
    val category: GoalCategory,
    val completionRate: Float,
)

/** Average across-goals completion rate for a single day, for the Stats "Week" bar chart. */
data class GoalDailyPoint(
    val date: LocalDate,
    val completionRate: Float,
)
