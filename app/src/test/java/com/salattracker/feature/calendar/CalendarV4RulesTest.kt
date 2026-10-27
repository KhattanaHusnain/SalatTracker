package com.salattracker.feature.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarV4RulesTest {
    @Test
    fun `month grid is Monday first and contains six complete weeks`() {
        val dates = calendarMonthGridDates(YearMonth.of(2026, 10))
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertTrue(dates.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
        assertEquals(31, dates.count { YearMonth.from(it) == YearMonth.of(2026, 10) })
    }

    @Test
    fun `month beginning on Monday starts with its first day`() {
        val month = YearMonth.of(2027, 2)
        assertEquals(LocalDate.of(2027, 2, 1), calendarMonthGridDates(month).first())
    }
}
