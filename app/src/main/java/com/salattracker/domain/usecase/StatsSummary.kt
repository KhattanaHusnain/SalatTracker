package com.salattracker.domain.usecase

import com.salattracker.domain.repository.DayStatusCounts
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusCounts
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── Completion % ──────────────────────────────────────────────────────────────

/** Congregation + individual as a share of countable (non-excused) prayers. Null when nothing
 * is countable — the caller shows "—", not a misleading 0%. */
fun completionPercent(counts: StatusCounts): Int? = counts.completionPercent

// ── Perfect days ──────────────────────────────────────────────────────────────

data class PerfectDays(val perfect: Int, val counted: Int)

/**
 * How many of [days] were a "perfect day" — all five prayers prayed, none missed, Menstruation
 * days neutral. A day counts only once it's actually decided:
 *  - [today] is skipped while still in progress (fewer than 5 prayers recorded yet and nothing
 *    missed) — it hasn't failed, but it hasn't earned a checkmark yet either.
 *  - A day with zero rows at all (nothing recorded, including [today]'s own gap above) isn't
 *    counted as a miss — it's simply excluded from both [PerfectDays.perfect] and [counted].
 *  - A past day is perfect only when nothing was missed and every non-excused slot was prayed
 *    (prayed + excused == 5) with at least one prayer actually prayed — matching the streak
 *    rule where an entirely-unrecorded past day breaks the streak rather than counting as perfect.
 */
fun perfectDays(days: List<DayStatusCounts>, today: LocalDate): PerfectDays {
    var perfect = 0
    var counted = 0
    for ((date, counts) in days) {
        if (counts.countable == 0 && counts.excused == 0) continue // nothing recorded at all
        if (date == today && counts.missed == 0 && counts.prayed + counts.excused < 5) continue // still in progress

        counted++
        val isPerfect = counts.missed == 0 && counts.prayed > 0 && counts.prayed + counts.excused >= 5
        if (isPerfect) perfect++
    }
    return PerfectDays(perfect, counted)
}

// ── By weekday ────────────────────────────────────────────────────────────────

/** Completion % per day of the week, summed across every matching date in range. A weekday with
 * no countable prayers at all maps to null rather than 0. */
fun weekdayCompletion(days: List<DayStatusCounts>): Map<DayOfWeek, Int?> {
    val byWeekday = days.groupBy { it.date.dayOfWeek }
    return DayOfWeek.values().associateWith { weekday ->
        byWeekday[weekday]?.fold(StatusCounts()) { acc, d -> acc + d.counts }?.completionPercent
    }
}

/** The weekday with the lowest completion %, or null when every weekday with data ties (or
 * there's at most one data point to compare). */
fun weakestWeekday(byWeekday: Map<DayOfWeek, Int?>): DayOfWeek? {
    val withData = byWeekday.filterValues { it != null }
    if (withData.size < 2) return null
    val min = withData.values.minOf { it!! }
    val max = withData.values.maxOf { it!! }
    if (min == max) return null
    return withData.entries.first { it.value == min }.key
}

// ── Completion trend ──────────────────────────────────────────────────────────

enum class TrendBucket { DAY, WEEK, MONTH }

/** Daily ≤ 40 days, weekly ≤ 120, monthly beyond — same thresholds the Stats period chips use. */
fun trendBucketFor(rangeDays: Int): TrendBucket = when {
    rangeDays <= 40  -> TrendBucket.DAY
    rangeDays <= 120 -> TrendBucket.WEEK
    else             -> TrendBucket.MONTH
}

/** One x-axis point of the completion-trend chart: [start] is the bucket's first date, and
 * [completionPct] is null for a bucket with nothing countable (skipped by the chart, not
 * plotted as 0). */
data class CompletionTrendPoint(val start: LocalDate, val completionPct: Int?)

/**
 * Buckets [days] by [bucket] and reduces each bucket to one [CompletionTrendPoint]. Weekly
 * buckets are counted backward from [to] (so the most recent bucket always ends exactly on
 * [to], and the oldest may be a partial week) — matching the Stats period chips' own convention.
 */
fun completionTrend(days: List<DayStatusCounts>, bucket: TrendBucket, to: LocalDate): List<CompletionTrendPoint> {
    val grouped: Map<LocalDate, List<DayStatusCounts>> = when (bucket) {
        TrendBucket.DAY -> days.groupBy { it.date }
        TrendBucket.WEEK -> days.groupBy { d ->
            val weeksBack = ChronoUnit.DAYS.between(d.date, to) / 7
            to.minusDays(weeksBack * 7 + 6)
        }
        TrendBucket.MONTH -> days.groupBy { it.date.withDayOfMonth(1) }
    }
    return grouped.toSortedMap().map { (start, bucketDays) ->
        val total = bucketDays.fold(StatusCounts()) { acc, d -> acc + d.counts }
        CompletionTrendPoint(start, total.completionPercent)
    }
}

// ── By prayer ─────────────────────────────────────────────────────────────────

/** The prayer with the lowest completion rate — the one Stats calls out as needing the most
 * attention. Null when fewer than two prayers have data to compare, or every rate with data
 * ties. Prayers with nothing countable are ignored; ties among the lowest break to the
 * earliest prayer in [PrayerType] order. */
fun weakestPrayer(breakdown: List<PrayerBreakdown>): PrayerBreakdown? {
    val withData = breakdown.filter { it.completionRate != null }
    if (withData.size < 2) return null
    val min = withData.minOf { it.completionRate!! }
    val max = withData.maxOf { it.completionRate!! }
    if (min >= max) return null
    return withData.filter { it.completionRate == min }.minByOrNull { it.prayerType.ordinal }
}

/** True once every prayer that has data is at 100% — the "every prayer on track" callout.
 * False (not shown) when nothing has data at all. */
fun allPrayersOnTrack(breakdown: List<PrayerBreakdown>): Boolean {
    val withData = breakdown.filter { it.completionRate != null }
    return withData.isNotEmpty() && withData.all { it.completionRate == 1f }
}
