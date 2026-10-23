package com.salattracker.notification

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerReminderDateTest {

    @Test
    fun `an action retains its scheduled date after midnight`() {
        val afterMidnight = LocalDate.of(2026, 10, 24)

        assertEquals(LocalDate.of(2026, 10, 23), parsePrayerDate("2026-10-23", afterMidnight))
    }

    @Test
    fun `legacy malformed action dates fall back safely`() {
        val fallback = LocalDate.of(2026, 10, 24)

        assertEquals(fallback, parsePrayerDate("not-a-date", fallback))
        assertEquals(fallback, parsePrayerDate(null, fallback))
    }
}
