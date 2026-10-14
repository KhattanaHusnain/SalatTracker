package com.salattracker.domain.model

import java.time.DayOfWeek
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
    /** false = archived by the user (history kept, hidden from the active list). */
    val isActive: Boolean,
    /** Due weekdays for DAILY goals; empty means every day. Ignored for weekly/monthly. */
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    /** When false the goal is ongoing and [durationDays] is ignored. */
    val endsAfterDuration: Boolean = false,
) {
    /** Last day of the goal, or null when ongoing. */
    val endDate: LocalDate? get() =
        if (endsAfterDuration && durationDays > 0) startDate.plusDays(durationDays - 1L) else null
}

/** One check-in event, for Stats to replay against a goal's own day/week/month periods. */
data class GoalCheckIn(
    val date: LocalDate,
    val amount: Int,
    /** An excused day: neutral — not hit, not missed, never breaks a streak. */
    val isSkip: Boolean = false,
)

/** A goal with its full check-in history (archived goals included). */
data class GoalHistory(
    val goal: SpiritualGoal,
    val checkIns: List<GoalCheckIn>,
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
