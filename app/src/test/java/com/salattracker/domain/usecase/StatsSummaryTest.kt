package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.DayStatusCounts
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsSummaryTest {

    // ── completionPercent ───────────────────────────────────────────────────

    @Test
    fun `completionPercent excludes excused days from the denominator`() {
        // 8 countable (10 total, 2 excused); 6 prayed (4 congregation + 2 individual) = 75%.
        val counts = StatusCounts(congregation = 4, individual = 2, qazaPending = 2, excused = 2)
        assertEquals(75, completionPercent(counts))
    }

    @Test
    fun `completionPercent is null when there is nothing countable`() {
        assertNull(completionPercent(StatusCounts()))
        assertNull(completionPercent(StatusCounts(excused = 5)))
    }

    @Test
    fun `completionPercent is 100 when every countable prayer was prayed`() {
        assertEquals(100, completionPercent(StatusCounts(congregation = 5)))
    }

    @Test
    fun `congregationPercent counts congregation among prayed prayers only`() {
        assertEquals(60, congregationPercent(StatusCounts(congregation = 3, individual = 2, qazaPending = 4)))
        assertNull(congregationPercent(StatusCounts(qazaPending = 5)))
    }

    @Test
    fun `loggingCoverage starts at first recorded day and excludes not-yet-due prayers today`() {
        val start = today.minusDays(2)
        val coverage = loggingCoverage(
            days = listOf(DayStatusCounts(start, StatusCounts(congregation = 3, excused = 2))),
            from = today.minusDays(10), today = today, firstRecorded = start,
            now = java.time.LocalTime.of(12, 0),
            todayTimes = mapOf(PrayerType.FAJR to java.time.LocalTime.of(5, 0), PrayerType.DHUHR to java.time.LocalTime.of(12, 30)),
        )
        assertEquals(LoggingCoverage(logged = 5, expected = 11), coverage)
    }

    @Test
    fun `congregationChallenge tracks current and best all-congregation runs`() {
        val days = listOf(
            DayStatusCounts(today.minusDays(4), StatusCounts(congregation = 5)),
            DayStatusCounts(today.minusDays(3), StatusCounts(congregation = 5)),
            DayStatusCounts(today.minusDays(2), StatusCounts(congregation = 5)),
            DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 5)),
        )
        assertEquals(CongregationChallenge(currentRun = 4, bestRun = 4), congregationChallenge(days, today))
    }

    // ── perfectDays ───────────────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `a past day with all five prayed is perfect`() {
        val days = listOf(DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 3, individual = 2)))
        val result = perfectDays(days, today)
        assertEquals(1, result.counted)
        assertEquals(1, result.perfect)
    }

    @Test
    fun `an excused day is neutral, not a miss`() {
        val days = listOf(DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 4, excused = 1)))
        val result = perfectDays(days, today)
        assertEquals(1, result.counted)
        assertEquals(1, result.perfect)
    }

    @Test
    fun `fully excused past and current days are excluded`() {
        val days = listOf(
            DayStatusCounts(today.minusDays(1), StatusCounts(excused = 5)),
            DayStatusCounts(today, StatusCounts(excused = 5)),
            DayStatusCounts(today.minusDays(2), StatusCounts(congregation = 5)),
        )

        assertEquals(PerfectDays(perfect = 1, counted = 1), perfectDays(days, today))
    }

    @Test
    fun `a past day missing a prayer is not perfect, matching the streak-break rule`() {
        // Only 3 of 5 slots accounted for, nothing missed outright — still not perfect.
        val days = listOf(DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 3)))
        val result = perfectDays(days, today)
        assertEquals(1, result.counted)
        assertEquals(0, result.perfect)
    }

    @Test
    fun `a past day with any qaza is not perfect`() {
        val days = listOf(DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 4, qazaPending = 1)))
        val result = perfectDays(days, today)
        assertEquals(1, result.counted)
        assertEquals(0, result.perfect)
    }

    @Test
    fun `today in progress with nothing missed yet is skipped`() {
        val days = listOf(DayStatusCounts(today, StatusCounts(congregation = 2)))
        val result = perfectDays(days, today)
        assertEquals(0, result.counted)
        assertEquals(0, result.perfect)
    }

    @Test
    fun `today already has a miss is counted as not perfect`() {
        val days = listOf(DayStatusCounts(today, StatusCounts(congregation = 1, qazaPending = 1)))
        val result = perfectDays(days, today)
        assertEquals(1, result.counted)
        assertEquals(0, result.perfect)
    }

    @Test
    fun `a date with nothing recorded is skipped entirely`() {
        val result = perfectDays(emptyList(), today)
        assertEquals(0, result.counted)
        assertEquals(0, result.perfect)
    }

    // ── trendBucketFor / completionTrend ──────────────────────────────────────

    @Test
    fun `trendBucketFor always uses weekly aggregation`() {
        assertEquals(TrendBucket.WEEK, trendBucketFor(7))
        assertEquals(TrendBucket.WEEK, trendBucketFor(40))
        assertEquals(TrendBucket.WEEK, trendBucketFor(121))
    }

    @Test
    fun `completionTrend buckets daily data into one point per date`() {
        val days = listOf(
            DayStatusCounts(today.minusDays(1), StatusCounts(congregation = 1)),
            DayStatusCounts(today, StatusCounts(congregation = 1, qazaPending = 1)),
        )
        val from = today.minusDays(2)
        val points = completionTrend(days, TrendBucket.DAY, from, today)
        assertEquals(3, points.size)
        assertNull(points[0].completionPct)
        assertEquals(100, points[1].completionPct)
        assertEquals(50, points[2].completionPct)
    }

    @Test
    fun `completionTrend groups weekly buckets from the selected range start`() {
        val weekAgo = today.minusDays(7)
        val days = listOf(
            DayStatusCounts(weekAgo, StatusCounts(congregation = 1)),
            DayStatusCounts(today, StatusCounts(congregation = 1, qazaPending = 1)),
        )
        val points = completionTrend(days, TrendBucket.WEEK, weekAgo, today)
        assertEquals(2, points.size)
        assertEquals(weekAgo, points[0].start)
        assertTrue(points[0].start.isBefore(points[1].start))
    }

    @Test
    fun `completionTrend keeps empty weekly buckets within the selected range`() {
        val from = today.minusDays(20)
        val points = completionTrend(
            listOf(DayStatusCounts(today, StatusCounts(congregation = 1))),
            TrendBucket.WEEK,
            from,
            today,
        )

        assertEquals(listOf(from, from.plusDays(7), from.plusDays(14)), points.map { it.start })
        assertNull(points[0].completionPct)
        assertNull(points[1].completionPct)
        assertEquals(100, points[2].completionPct)
    }

    @Test
    fun `completionTrend groups monthly buckets by calendar month`() {
        val days = listOf(
            DayStatusCounts(LocalDate.of(2026, 9, 5), StatusCounts(congregation = 1)),
            DayStatusCounts(LocalDate.of(2026, 9, 20), StatusCounts(congregation = 1)),
            DayStatusCounts(LocalDate.of(2026, 10, 5), StatusCounts(qazaPending = 1)),
        )
        val from = LocalDate.of(2026, 9, 5)
        val points = completionTrend(days, TrendBucket.MONTH, from, today)
        assertEquals(2, points.size)
        assertEquals(from, points[0].start)
        assertEquals(100, points[0].completionPct)
        assertEquals(0, points[1].completionPct)
    }

    // ── weakestPrayer / allPrayersOnTrack ─────────────────────────────────────

    @Test
    fun `weakestPrayer is null for an empty list`() {
        assertNull(weakestPrayer(emptyList()))
    }

    @Test
    fun `weakestPrayer ignores prayers with no data`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.FAJR, StatusCounts()), // no data — ignored, not treated as 0%
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 2, qazaPending = 2)), // 50%
            PrayerBreakdown(PrayerType.ASR, StatusCounts(congregation = 1)), // 100%
        )
        assertNull(weakestPrayer(breakdown))
    }

    @Test
    fun `weakestPrayer is null when every rate with data is at 100 percent`() {
        val breakdown = PrayerType.values().map { PrayerBreakdown(it, StatusCounts(congregation = 1)) }
        assertNull(weakestPrayer(breakdown))
    }

    @Test
    fun `weakestPrayer breaks a tie at the minimum to the earliest prayer`() {
        // Dhuhr and Fajr both tie for lowest (50%); Asr is higher (100%), so there IS a weakest
        // tier — just a tied one — and it should resolve to Fajr, the earlier of the two.
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 5, qazaPending = 5)),
            PrayerBreakdown(PrayerType.FAJR, StatusCounts(congregation = 5, qazaPending = 5)),
            PrayerBreakdown(PrayerType.ASR, StatusCounts(congregation = 10)),
        )
        assertEquals(PrayerType.FAJR, weakestPrayer(breakdown)?.prayerType)
    }

    @Test
    fun `weakestPrayer is null when the only two prayers with data are equal`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 1, qazaPending = 1)),
            PrayerBreakdown(PrayerType.FAJR, StatusCounts(congregation = 1, qazaPending = 1)),
        )
        assertEquals(null, weakestPrayer(breakdown))
    }

    @Test
    fun `weakestPrayer returns the lowest completion rate`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.FAJR, StatusCounts(congregation = 2, qazaPending = 3)),
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 9, qazaPending = 1)),
            PrayerBreakdown(PrayerType.ASR, StatusCounts(congregation = 6, qazaPending = 4)),
            PrayerBreakdown(PrayerType.MAGHRIB, StatusCounts(congregation = 5)),
            PrayerBreakdown(PrayerType.ISHA, StatusCounts(congregation = 8, qazaPending = 2)),
        )
        assertEquals(PrayerType.FAJR, weakestPrayer(breakdown)?.prayerType)
    }

    @Test
    fun `weakestPrayer ignores trivial differences despite a large sample`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.FAJR, StatusCounts(congregation = 99, qazaPending = 1)),
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 100)),
        )

        assertNull(weakestPrayer(breakdown))
    }

    @Test
    fun `allPrayersOnTrack is false when nothing has data`() {
        assertTrue(!allPrayersOnTrack(emptyList()))
    }

    @Test
    fun `allPrayersOnTrack is true once every prayer with data is at 100 percent`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.FAJR, StatusCounts()), // no data — ignored
            PrayerBreakdown(PrayerType.DHUHR, StatusCounts(congregation = 1)),
        )
        assertTrue(allPrayersOnTrack(breakdown))
    }
}
