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

internal fun periodEndFor(frequency: GoalFrequency, periodStart: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> periodStart
    GoalFrequency.WEEKLY  -> periodStart.plusDays(6)
    GoalFrequency.MONTHLY -> YearMonth.from(periodStart).atEndOfMonth()
}

internal fun nextPeriodStart(frequency: GoalFrequency, periodStart: LocalDate): LocalDate = when (frequency) {
    GoalFrequency.DAILY   -> periodStart.plusDays(1)
    GoalFrequency.WEEKLY  -> periodStart.plusDays(7)
    GoalFrequency.MONTHLY -> YearMonth.from(periodStart).plusMonths(1).atDay(1)
}

/** Check-ins indexed by day: amounts of normal check-ins, and the set of excused days. */
internal class DayLog(checkIns: List<GoalCheckIn>) {
    private val amountByDate: Map<LocalDate, Int> =
        checkIns.filter { !it.isSkip }.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
    private val skipDates: Set<LocalDate> = checkIns.filter { it.isSkip }.map { it.date }.toSet()

    fun amountOn(date: LocalDate): Int = amountByDate[date] ?: 0
    fun isSkipped(date: LocalDate): Boolean = date in skipDates

    fun amountBetween(from: LocalDate, to: LocalDate): Int {
        var sum = 0
        var d = from
        while (!d.isAfter(to)) { sum += amountOn(d); d = d.plusDays(1) }
        return sum
    }
}

/**
 * How [goal] did over [from]..[to], judged by the goal's own day/week/month periods rather
 * than averaged per day (a weekly goal hit on day 1 of 7 isn't "14% done" — its week just isn't
 * over yet). [checkIns] must already be filtered to this one goal, and should cover back to at
 * least a full period before [from] (see [com.salattracker.domain.repository.GoalRepository.getGoalPeriodStats]),
 * so a week/month straddling the start of the range can still be judged on its full total.
 *
 * Daily goals only count days they're due on ([SpiritualGoal.isDueOn]) and skip excused days;
 * nothing counts past the goal's end date. A period counts toward [GoalPeriodStat.periodsCounted]
 * once it has fully elapsed as of [today], or earlier if its target is already hit — an in-progress,
 * not-yet-hit period (including [today]'s own) isn't counted yet.
 */
fun goalPeriodStat(
    goal: SpiritualGoal,
    checkIns: List<GoalCheckIn>,
    from: LocalDate,
    to: LocalDate,
    today: LocalDate = LocalDate.now(),
): GoalPeriodStat {
    val log = DayLog(checkIns)
    val loggedAmount = log.amountBetween(from, to)

    val effectiveStart = maxOf(from, goal.startDate)
    val goalEnd = goal.endDate
    val effectiveEnd = if (goalEnd != null) minOf(to, goalEnd) else to
    if (effectiveStart.isAfter(effectiveEnd)) {
        return GoalPeriodStat(goal.name, goal.category, goal.frequency, goal.unit, loggedAmount, 0, 0)
    }

    var periodsHit = 0
    var periodsCounted = 0
    var periodStart = periodStartFor(goal.frequency, effectiveStart)
    while (!periodStart.isAfter(effectiveEnd)) {
        val periodEnd = periodEndFor(goal.frequency, periodStart)
        val countable = goal.frequency != GoalFrequency.DAILY ||
            (goal.isDueOn(periodStart) && !log.isSkipped(periodStart))
        if (countable) {
            val hit = log.amountBetween(periodStart, periodEnd) >= goal.targetAmount
            val clippedEnd = if (goalEnd != null) minOf(periodEnd, goalEnd) else periodEnd
            if (hit || clippedEnd.isBefore(today)) {
                periodsCounted++
                if (hit) periodsHit++
            }
        }
        periodStart = nextPeriodStart(goal.frequency, periodStart)
    }

    return GoalPeriodStat(goal.name, goal.category, goal.frequency, goal.unit, loggedAmount, periodsHit, periodsCounted)
}
