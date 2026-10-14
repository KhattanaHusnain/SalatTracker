package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsSummaryTest {

    // ── completionPercent ───────────────────────────────────────────────────

    @Test
    fun `completionPercent excludes excused days from the denominator`() {
        // 10 total, 2 excused -> 8 countable; 6 prayed (4 congregation + 2 individual) = 75%.
        assertEquals(75, completionPercent(congregation = 4, individual = 2, excused = 2, total = 10))
    }

    @Test
    fun `completionPercent is 0 when there is nothing countable`() {
        assertEquals(0, completionPercent(congregation = 0, individual = 0, excused = 0, total = 0))
        assertEquals(0, completionPercent(congregation = 0, individual = 0, excused = 5, total = 5))
    }

    @Test
    fun `completionPercent is 100 when every countable prayer was prayed`() {
        assertEquals(100, completionPercent(congregation = 5, individual = 0, excused = 0, total = 5))
    }

    // ── weakestPrayer ───────────────────────────────────────────────────────

    @Test
    fun `weakestPrayer is null for an empty list`() {
        assertNull(weakestPrayer(emptyList()))
    }

    @Test
    fun `weakestPrayer is null when every prayer is at 100 percent`() {
        val breakdown = PrayerType.values().map { PrayerBreakdown(it, completionRate = 1f) }
        assertNull(weakestPrayer(breakdown))
    }

    @Test
    fun `weakestPrayer returns the lowest completion rate`() {
        val breakdown = listOf(
            PrayerBreakdown(PrayerType.FAJR, 0.4f),
            PrayerBreakdown(PrayerType.DHUHR, 0.9f),
            PrayerBreakdown(PrayerType.ASR, 0.6f),
            PrayerBreakdown(PrayerType.MAGHRIB, 1f),
            PrayerBreakdown(PrayerType.ISHA, 0.8f),
        )
        assertEquals(PrayerType.FAJR, weakestPrayer(breakdown)?.prayerType)
    }
}
