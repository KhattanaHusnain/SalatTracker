package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

private val TODAY_TIMES = mapOf(
    PrayerType.FAJR to LocalTime.of(5, 30),
    PrayerType.DHUHR to LocalTime.of(12, 15),
    PrayerType.ASR to LocalTime.of(15, 45),
    PrayerType.MAGHRIB to LocalTime.of(18, 20),
    PrayerType.ISHA to LocalTime.of(19, 45),
)

class HomeDashboardTest {

    // ── nextPrayerOf ────────────────────────────────────────────────────────

    @Test
    fun `before Fajr, next prayer is Fajr`() {
        val next = nextPrayerOf(TODAY_TIMES, tomorrowFajr = LocalTime.of(5, 31), now = LocalTime.of(4, 0))
        assertEquals(NextPrayer(PrayerType.FAJR, LocalTime.of(5, 30), isTomorrow = false), next)
    }

    @Test
    fun `between two prayers, next prayer is the next one ahead`() {
        val next = nextPrayerOf(TODAY_TIMES, tomorrowFajr = null, now = LocalTime.of(13, 0))
        assertEquals(NextPrayer(PrayerType.ASR, LocalTime.of(15, 45), isTomorrow = false), next)
    }

    @Test
    fun `after Isha with tomorrow's Fajr cached, next prayer is tomorrow's Fajr`() {
        val next = nextPrayerOf(TODAY_TIMES, tomorrowFajr = LocalTime.of(5, 31), now = LocalTime.of(20, 30))
        assertEquals(NextPrayer(PrayerType.FAJR, LocalTime.of(5, 31), isTomorrow = true), next)
    }

    @Test
    fun `after Isha with no cached tomorrow time, next prayer is null`() {
        assertNull(nextPrayerOf(TODAY_TIMES, tomorrowFajr = null, now = LocalTime.of(20, 30)))
    }

    @Test
    fun `unknown prayer times with no cached tomorrow time, next prayer is null`() {
        assertNull(nextPrayerOf(todayTimes = null, tomorrowFajr = null, now = LocalTime.of(10, 0)))
    }

    @Test
    fun `unknown today's times still falls back to tomorrow's Fajr`() {
        val next = nextPrayerOf(todayTimes = null, tomorrowFajr = LocalTime.of(5, 31), now = LocalTime.of(10, 0))
        assertEquals(NextPrayer(PrayerType.FAJR, LocalTime.of(5, 31), isTomorrow = true), next)
    }

    // ── minutesUntil ────────────────────────────────────────────────────────

    @Test
    fun `minutesUntil same day is a plain difference`() {
        val next = NextPrayer(PrayerType.ASR, LocalTime.of(15, 45), isTomorrow = false)
        assertEquals(45L, minutesUntil(next, now = LocalTime.of(15, 0)))
    }

    @Test
    fun `minutesUntil tomorrow wraps across midnight`() {
        val next = NextPrayer(PrayerType.FAJR, LocalTime.of(5, 30), isTomorrow = true)
        // 23:45 -> 00:00 is 15 minutes, plus 5:30 into the new day = 345.
        assertEquals(345L, minutesUntil(next, now = LocalTime.of(23, 45)))
    }

    // ── currentPrayerOf ─────────────────────────────────────────────────────

    @Test
    fun `currentPrayerOf is null before Fajr`() {
        assertNull(currentPrayerOf(TODAY_TIMES, now = LocalTime.of(4, 0)))
    }

    @Test
    fun `currentPrayerOf is the last prayer whose time has passed`() {
        assertEquals(PrayerType.DHUHR, currentPrayerOf(TODAY_TIMES, now = LocalTime.of(13, 0)))
    }

    @Test
    fun `currentPrayerOf is null when times are unknown`() {
        assertNull(currentPrayerOf(todayTimes = null, now = LocalTime.of(13, 0)))
    }

    // ── perfectDayStreak ────────────────────────────────────────────────────

    @Test
    fun `perfectDayStreak of an empty map is 0`() {
        assertEquals(0, perfectDayStreak(emptyMap()))
    }

    @Test
    fun `perfectDayStreak is the minimum across all five prayers, missing ones count as 0`() {
        val streaks = mapOf(
            PrayerType.FAJR to StreakInfo(currentStreak = 12, highestStreak = 40),
            PrayerType.DHUHR to StreakInfo(currentStreak = 30, highestStreak = 30),
            PrayerType.ASR to StreakInfo(currentStreak = 5, highestStreak = 15),
            // MAGHRIB missing entirely
            PrayerType.ISHA to StreakInfo(currentStreak = 22, highestStreak = 22),
        )
        assertEquals(0, perfectDayStreak(streaks))
    }

    @Test
    fun `perfectDayStreak when every prayer has a streak`() {
        val streaks = PrayerType.values().associateWith { StreakInfo(currentStreak = 12, highestStreak = 40) } +
            mapOf(PrayerType.ASR to StreakInfo(currentStreak = 5, highestStreak = 15))
        assertEquals(5, perfectDayStreak(streaks))
    }
}
