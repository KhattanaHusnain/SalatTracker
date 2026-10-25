package com.salattracker.domain.usecase

import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// ── Schedule ─────────────────────────────────────────────────────────────────

/**
 * Whether [date] is a day this goal expects activity: inside [startDate, endDate], and — for
 * DAILY goals — on one of the chosen weekdays (an empty selection means every day). Weekly and
 * monthly goals are "due" throughout their window, since any day can contribute to the period.
 */
fun SpiritualGoal.isDueOn(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    val end = endDate
    if (end != null && date.isAfter(end)) return false
    if (frequency != GoalFrequency.DAILY) return true
    return daysOfWeek.isEmpty() || date.dayOfWeek in daysOfWeek
}

/** True once [today] is past the goal's end date. Ongoing goals never finish. */
fun SpiritualGoal.isFinished(today: LocalDate): Boolean = endDate?.let { today.isAfter(it) } ?: false

/** 1-based day number of [today] within the goal's span (clamped to the span), or null when ongoing. */
fun SpiritualGoal.dayIndex(today: LocalDate): Int? {
    val end = endDate ?: return null
    val last = if (today.isAfter(end)) end else today
    return (ChronoUnit.DAYS.between(startDate, last) + 1).toInt().coerceAtLeast(1)
}

/** Weekday selection <-> storage bitmask (bit = DayOfWeek.value - 1; 0 = every day). */
fun Set<DayOfWeek>.toDaysMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun daysOfWeekFromMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.values().filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

// ── Per-day state (what a cell in the strip / calendar shows) ────────────────

enum class GoalDayState { DONE, PARTIAL, MISSED, SKIPPED, OPEN, NOT_DUE, LOGGED, NONE, LOCKED }

fun goalDayState(
    goal: SpiritualGoal,
    date: LocalDate,
    dayAmount: Int,
    daySkipped: Boolean,
    today: LocalDate,
): GoalDayState {
    val end = goal.endDate
    if (date.isAfter(today) || date.isBefore(goal.startDate) || (end != null && date.isAfter(end))) {
        return GoalDayState.LOCKED
    }
    if (goal.frequency != GoalFrequency.DAILY) {
        return when {
            dayAmount > 0 -> GoalDayState.LOGGED
            date == today -> GoalDayState.OPEN
            else          -> GoalDayState.NONE
        }
    }
    return when {
        daySkipped                          -> GoalDayState.SKIPPED
        dayAmount >= goal.targetAmount      -> GoalDayState.DONE
        dayAmount > 0                       -> GoalDayState.PARTIAL
        !goal.isDueOn(date)                 -> GoalDayState.NOT_DUE
        date == today                       -> GoalDayState.OPEN
        else                                -> GoalDayState.MISSED
    }
}

/** Progress in the goal's current period (today / this week / this month), excused days excluded. */
fun currentPeriodAmount(goal: SpiritualGoal, checkIns: List<GoalCheckIn>, today: LocalDate): Int =
    DayLog(checkIns).amountBetween(periodStartFor(goal.frequency, today), today)

// ── Streak ───────────────────────────────────────────────────────────────────

/** Consecutive hit periods (days for DAILY, weeks for WEEKLY, months for MONTHLY). */
data class GoalStreak(val current: Int, val best: Int)

/**
 * Walks the goal's own periods from its start: a hit period extends the streak; a missed past
 * period resets it. Not-due days, excused days and a period still in progress (today / this
 * week / this month, not yet hit) are neutral — they neither extend nor break it.
 */
fun goalStreak(goal: SpiritualGoal, checkIns: List<GoalCheckIn>, today: LocalDate): GoalStreak {
    val log = DayLog(checkIns)
    val goalEnd = goal.endDate
    val last = if (goalEnd != null && goalEnd.isBefore(today)) goalEnd else today
    if (goal.startDate.isAfter(last)) return GoalStreak(0, 0)

    var run = 0
    var best = 0
    var periodStart = periodStartFor(goal.frequency, goal.startDate)
    while (!periodStart.isAfter(last)) {
        val periodEnd = periodEndFor(goal.frequency, periodStart)
        val clippedEnd = if (goalEnd != null) minOf(periodEnd, goalEnd) else periodEnd
        val countable = goal.frequency != GoalFrequency.DAILY ||
            (goal.isDueOn(periodStart) && !log.isSkipped(periodStart))
        if (countable) {
            val hit = log.amountBetween(periodStart, periodEnd) >= goal.targetAmount
            when {
                hit -> { run++; if (run > best) best = run }
                clippedEnd.isBefore(today) -> run = 0   // ended without reaching the target
                else -> Unit                            // still in progress: neutral
            }
        }
        periodStart = nextPeriodStart(goal.frequency, periodStart)
    }
    return GoalStreak(current = run, best = best)
}

// ── Reminders ────────────────────────────────────────────────────────────────

/**
 * The next time this goal's reminder should fire after [now]: the first day within the coming
 * week that the goal is due on and whose reminder time is still ahead. Null when reminders are
 * off, the goal is archived, or it has no further due day (e.g. it has ended).
 */
fun nextReminderAt(
    goal: SpiritualGoal,
    now: LocalDateTime,
    checkIns: List<GoalCheckIn> = emptyList(),
): LocalDateTime? {
    val time = goal.reminderTime ?: return null
    if (!goal.reminderEnabled || !goal.isActive) return null
    for (offset in 0..7) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val at = LocalDateTime.of(date, time)
        if (at.isAfter(now) && goal.isDueOn(date) && currentPeriodAmount(goal, checkIns, date) < goal.targetAmount) return at
    }
    return null
}

// ── Status & totals (details screen / archived section) ──────────────────────

enum class GoalStatus { ACTIVE, ARCHIVED, COMPLETED }

/** COMPLETED wins over ARCHIVED: a goal that ran its course reads as completed even if later archived. */
fun SpiritualGoal.statusOn(today: LocalDate): GoalStatus = when {
    isFinished(today) -> GoalStatus.COMPLETED
    !isActive         -> GoalStatus.ARCHIVED
    else              -> GoalStatus.ACTIVE
}

/** Whole-life totals of a goal, from its start up to today (or its end date). */
data class GoalTotals(
    val loggedAmount: Int,
    val periodsHit: Int,
    val periodsCounted: Int,
    val excusedDays: Int,
)

fun goalTotals(goal: SpiritualGoal, checkIns: List<GoalCheckIn>, today: LocalDate): GoalTotals {
    val end = goal.endDate?.let { if (it.isBefore(today)) it else today } ?: today
    val stat = goalPeriodStat(goal, checkIns, goal.startDate, end, today)
    return GoalTotals(
        loggedAmount   = stat.loggedAmount,
        periodsHit     = stat.periodsHit,
        periodsCounted = stat.periodsCounted,
        excusedDays    = checkIns.count { it.isSkip },
    )
}
