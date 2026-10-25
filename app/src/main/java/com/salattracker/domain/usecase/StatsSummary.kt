package com.salattracker.domain.usecase

import com.salattracker.domain.repository.DayStatusCounts
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusCounts
import java.time.LocalDate
import java.time.LocalTime
import com.salattracker.domain.model.PrayerType

// ── Completion % ──────────────────────────────────────────────────────────────

/** Congregation + individual as a share of countable (non-excused) prayers. Null when nothing
 * is countable — the caller shows "—", not a misleading 0%. */
fun completionPercent(counts: StatusCounts): Int? = counts.completionPercent

data class LoggingCoverage(val logged: Int, val expected: Int) {
    val percent: Int? get() = if (expected == 0) null else (logged * 100) / expected
}

/** Coverage starts at the later of the selected range and the first logged date. Today includes
 * only prayers whose calculated time has passed; every stored status, including excused, counts. */
fun loggingCoverage(days: List<DayStatusCounts>, from: LocalDate, today: LocalDate, firstRecorded: LocalDate?, now: LocalTime, todayTimes: Map<PrayerType, LocalTime>?): LoggingCoverage {
    val start = listOfNotNull(from, firstRecorded).maxOrNull() ?: return LoggingCoverage(0, 0)
    if (start.isAfter(today)) return LoggingCoverage(0, 0)
    val completedDays = start.datesUntil(today).count().toInt()
    val dueToday = todayTimes?.values?.count { !it.isAfter(now) } ?: 0
    val expected = completedDays * PrayerType.values().size + dueToday
    val logged = days.filter { !it.date.isBefore(start) }.sumOf { it.counts.congregation + it.counts.individual + it.counts.qazaPending + it.counts.qazaCompleted + it.counts.excused }
    return LoggingCoverage(logged.coerceAtMost(expected), expected)
}

/** Congregation as a share of prayers that were actually prayed, excluding qaza and excused
 * statuses. Null distinguishes an empty period from a true 0% congregation rate. */
fun congregationPercent(counts: StatusCounts): Int? {
    val prayed = counts.congregation + counts.individual
    return if (prayed == 0) null else (counts.congregation * 100) / prayed
}

data class CongregationChallenge(val currentRun: Int, val bestRun: Int)

/** Consecutive completed calendar days with all five prayers recorded in congregation, measured
 * in the latest 40-day window. Missing or partially recorded dates break a run. */
fun congregationChallenge(days: List<DayStatusCounts>, today: LocalDate): CongregationChallenge {
    val byDate = days.associateBy { it.date }
    val lastCompletedDay = today.minusDays(1)
    val dates = (0 until 40).map { lastCompletedDay.minusDays(it.toLong()) }.reversed()
    var current = 0
    var best = 0
    dates.forEach { date ->
        if (byDate[date]?.counts?.congregation == 5) {
            current++
            best = maxOf(best, current)
        } else {
            current = 0
        }
    }
    return CongregationChallenge(currentRun = current, bestRun = best)
}

// ── Perfect days ──────────────────────────────────────────────────────────────

data class PerfectDays(val perfect: Int, val counted: Int)

private const val MIN_INSIGHT_SAMPLE = 10
private const val MEANINGFUL_COMPLETION_GAP = 10

/**
 * How many of [days] were a "perfect day" — all five prayers prayed, none missed, Menstruation
 * days neutral. A day counts only once it's actually decided:
 *  - [today] is skipped while still in progress (fewer than 5 prayers recorded yet and nothing
 *    missed) — it hasn't failed, but it hasn't earned a checkmark yet either.
 *  - A day with no countable prayers (nothing recorded or every prayer excused) isn't counted
 *    as a miss — it's simply excluded from both [PerfectDays.perfect] and [counted].
 *  - A past day is perfect only when nothing was missed and every non-excused slot was prayed
 *    (prayed + excused == 5) with at least one prayer actually prayed — matching the streak
 *    rule where an entirely-unrecorded past day breaks the streak rather than counting as perfect.
 */
fun perfectDays(days: List<DayStatusCounts>, today: LocalDate): PerfectDays {
    var perfect = 0
    var counted = 0
    for ((date, counts) in days) {
        if (counts.countable == 0) continue // nothing recorded or every prayer excused
        if (date == today && counts.missed == 0 && counts.prayed + counts.excused < 5) continue // still in progress

        counted++
        val isPerfect = counts.missed == 0 && counts.prayed > 0 && counts.prayed + counts.excused >= 5
        if (isPerfect) perfect++
    }
    return PerfectDays(perfect, counted)
}

// ── Completion trend ──────────────────────────────────────────────────────────

enum class TrendBucket { DAY, WEEK, MONTH }

/** Weekly aggregation smooths five-prayer daily noise into a useful improvement trend. */
fun trendBucketFor(_rangeDays: Int): TrendBucket = TrendBucket.WEEK

/** One x-axis point of the completion-trend chart: [start] is the bucket's first in-range date,
 * and [completionPct] is null for a bucket with nothing countable. Null buckets deliberately
 * remain in the result so the chart can preserve time gaps instead of joining unrelated days. */
data class CompletionTrendPoint(val start: LocalDate, val completionPct: Int?)

/**
 * Buckets [days] by [bucket] across every date from [from] through [to] and reduces each bucket
 * to one [CompletionTrendPoint]. Empty buckets are retained as null points. Weekly buckets begin
 * at [from], and calendar-month buckets are clipped to the selected range, so no plotted label
 * can fall outside it.
 */
fun completionTrend(
    days: List<DayStatusCounts>,
    bucket: TrendBucket,
    from: LocalDate,
    to: LocalDate,
): List<CompletionTrendPoint> = trendWindows(bucket, from, to).map { (start, end) ->
    val total = days.asSequence()
        .filter { it.date in start..end }
        .fold(StatusCounts()) { acc, day -> acc + day.counts }
    CompletionTrendPoint(start, total.completionPercent)
}

private fun trendWindows(bucket: TrendBucket, from: LocalDate, to: LocalDate): List<Pair<LocalDate, LocalDate>> {
    require(!from.isAfter(to)) { "Trend range start must not be after its end." }
    return when (bucket) {
        TrendBucket.DAY -> generateSequence(from) { date -> date.plusDays(1).takeIf { !it.isAfter(to) } }
            .map { it to it }
            .toList()
        TrendBucket.WEEK -> generateSequence(from) { date -> date.plusDays(7).takeIf { !it.isAfter(to) } }
            .map { start -> start to minOf(start.plusDays(6), to) }
            .toList()
        TrendBucket.MONTH -> generateSequence(java.time.YearMonth.from(from)) { it.plusMonths(1) }
            .map { month -> maxOf(from, month.atDay(1)) to minOf(to, month.atEndOfMonth()) }
            .takeWhile { (start, _) -> !start.isAfter(to) }
            .toList()
    }
}

// ── By prayer ─────────────────────────────────────────────────────────────────

/** The prayer with the lowest completion rate — the one Stats calls out as needing the most
 * attention. Null when fewer than two prayers have data to compare, or every rate with data
 * ties. Prayers with nothing countable are ignored; ties among the lowest break to the
 * earliest prayer in [PrayerType] order. */
fun weakestPrayer(breakdown: List<PrayerBreakdown>): PrayerBreakdown? {
    val withData = breakdown.filter {
        it.completionRate != null && it.counts.countable >= MIN_INSIGHT_SAMPLE
    }
    if (withData.size < 2) return null
    val min = withData.minOf { it.completionRate!! }
    val average = withData.map { it.completionRate!! }.average()
    if (average - min < MEANINGFUL_COMPLETION_GAP / 100f) return null
    return withData.filter { it.completionRate == min }.minByOrNull { it.prayerType.ordinal }
}

/** True once every prayer that has data is at 100% — the "every prayer on track" callout.
 * False (not shown) when nothing has data at all. */
fun allPrayersOnTrack(breakdown: List<PrayerBreakdown>): Boolean {
    val withData = breakdown.filter { it.completionRate != null }
    return withData.isNotEmpty() && withData.all { it.completionRate == 1f }
}
