package com.salattracker.domain.usecase

import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.SpiritualGoal
import java.time.LocalDate
import java.time.YearMonth

// weekStartOf(date) — the Sunday-start week containing [date] — already exists in CalendarGrid.kt,
// same package.

/** The start of the goal-frequency period containing [today] — "today" for a daily goal, the
 * Sunday-start week for a weekly goal, the 1st of the month for a monthly goal. */
fun periodStartFor(frequency: GoalFrequency, today: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> today
    GoalFrequency.WEEKLY  -> weekStartOf(today)
    GoalFrequency.MONTHLY -> YearMonth.from(today).atDay(1)
}

private fun periodEndFor(frequency: GoalFrequency, periodStart: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> periodStart
    GoalFrequency.WEEKLY  -> periodStart.plusDays(6)
    GoalFrequency.MONTHLY -> YearMonth.from(periodStart).atEndOfMonth()
}

private fun nextPeriodStart(frequency: GoalFrequency, periodStart: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> periodStart.plusDays(1)
    GoalFrequency.WEEKLY  -> periodStart.plusDays(7)
    GoalFrequency.MONTHLY -> YearMonth.from(periodStart).plusMonths(1).atDay(1)
}

/**
 * How [goal] did over [from]..[to], judged by the goal's own day/week/monthly periods rather
 * than averaged per day (a weekly goal hit on day 1 of 7 isn't "14% done" — its week just isn't
 * over yet). [checkIns] must already be filtered to this one goal, and should cover back to at
 * least a full period before [from] (see [com.salattracker.domain.repository.GoalRepository.getGoalPeriodStats]),
 * so a week/month straddling the start of the range can still be judged on its full total.
 *
 * A period counts toward [GoalPeriodStat.periodsCounted] once it has fully elapsed as of [today],
 * or earlier if its target is already hit — an in-progress, not-yet-hit period (including
 * [today]'s own) isn't counted yet, the same "not decided yet" treatment [perfectDays] gives an
 * in-progress today.
 */
fun goalPeriodStat(
    goal: SpiritualGoal,
    checkIns: List<GoalCheckIn>,
    from: LocalDate,
    to: LocalDate,
    today: LocalDate = LocalDate.now(),
): GoalPeriodStat {
    val loggedAmount = checkIns.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sumOf { it.amount }

    val effectiveStart = maxOf(from, goal.startDate)
    if (effectiveStart.isAfter(to)) {
        return GoalPeriodStat(goal.name, goal.category, goal.frequency, goal.unit, loggedAmount, 0, 0)
    }

    var periodsHit = 0
    var periodsCounted = 0
    var periodStart = periodStartFor(goal.frequency, effectiveStart)
    while (!periodStart.isAfter(to)) {
        val periodEnd = periodEndFor(goal.frequency, periodStart)
        val amountInPeriod = checkIns.filter { !it.date.isBefore(periodStart) && !it.date.isAfter(periodEnd) }.sumOf { it.amount }
        val hit = amountInPeriod >= goal.targetAmount
        val ended = periodEnd.isBefore(today)
        if (hit || ended) {
            periodsCounted++
            if (hit) periodsHit++
        }
        periodStart = nextPeriodStart(goal.frequency, periodStart)
    }

    return GoalPeriodStat(goal.name, goal.category, goal.frequency, goal.unit, loggedAmount, periodsHit, periodsCounted)
}
