package com.salattracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {

    @Test
    fun `month grid always has 42 consecutive dates starting on a Sunday`() {
        val dates = monthGridDates(YearMonth.of(2026, 10))
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.SUNDAY, dates.first().dayOfWeek)
        assertTrue(dates.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `month grid covers every day of the month exactly once`() {
        val month = YearMonth.of(2026, 2) // Feb 2026, 28 days
        val daysInMonth = monthGridDates(month).filter { YearMonth.from(it) == month }
        assertEquals(28, daysInMonth.size)
    }

    @Test
    fun `week dates are 7 consecutive days starting on Sunday`() {
        val week = weekDatesOf(LocalDate.of(2026, 10, 14)) // a Wednesday
        assertEquals(7, week.size)
        assertEquals(DayOfWeek.SUNDAY, week.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 11), week.first())
        assertEquals(LocalDate.of(2026, 10, 17), week.last())
    }

    @Test
    fun `shiftPeriod month clamps day to the shorter target month`() {
        val jan31 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), shiftPeriod(CalendarViewType.MONTH, jan31, 1))
    }

    @Test
    fun `shiftPeriod week moves by exactly 7 days`() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals(date.plusDays(7), shiftPeriod(CalendarViewType.WEEK, date, 1))
        assertEquals(date.minusDays(7), shiftPeriod(CalendarViewType.WEEK, date, -1))
    }

    @Test
    fun `shiftPeriod year clamps Feb 29 to Feb 28 on a non-leap target`() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2025, 2, 28), shiftPeriod(CalendarViewType.YEAR, leapDay, 1))
    }

    @Test
    fun `shiftPeriod never leaves the calendar range`() {
        assertEquals(CALENDAR_MIN_DATE, shiftPeriod(CalendarViewType.YEAR, CALENDAR_MIN_DATE, -1))
        assertEquals(CALENDAR_MAX_DATE, shiftPeriod(CalendarViewType.YEAR, CALENDAR_MAX_DATE, 1))
    }

    @Test
    fun `observedRange for month spans from the previous month's grid to the next month's grid`() {
        val (from, to) = observedRange(CalendarViewType.MONTH, LocalDate.of(2026, 10, 14))
        assertTrue(!from.isAfter(LocalDate.of(2026, 9, 1)))
        assertTrue(!to.isBefore(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun `observedRange for week is exactly that week`() {
        val (from, to) = observedRange(CalendarViewType.WEEK, LocalDate.of(2026, 10, 14))
        assertEquals(LocalDate.of(2026, 10, 11), from)
        assertEquals(LocalDate.of(2026, 10, 17), to)
    }

    @Test
    fun `observedRange for year is Jan 1 to Dec 31`() {
        val (from, to) = observedRange(CalendarViewType.YEAR, LocalDate.of(2026, 6, 1))
        assertEquals(LocalDate.of(2026, 1, 1), from)
        assertEquals(LocalDate.of(2026, 12, 31), to)
    }

    @Test
    fun `isAwayFromToday is false only when the period actually contains today`() {
        val today = LocalDate.of(2026, 10, 14)
        assertEquals(false, isAwayFromToday(CalendarViewType.MONTH, today, today))
        assertEquals(true, isAwayFromToday(CalendarViewType.MONTH, today.minusMonths(1), today))
        assertEquals(false, isAwayFromToday(CalendarViewType.WEEK, today, today))
        assertEquals(true, isAwayFromToday(CalendarViewType.WEEK, today.minusWeeks(1), today))
        assertEquals(false, isAwayFromToday(CalendarViewType.YEAR, today, today))
        assertEquals(true, isAwayFromToday(CalendarViewType.YEAR, today.minusYears(1), today))
    }

    @Test
    fun `month page index round-trips through a date`() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals(date, dateFromMonthPageIndex(monthPageIndexOf(date), date.dayOfMonth))
    }

    @Test
    fun `week page index round-trips through a date`() {
        val date = LocalDate.of(2026, 10, 14)
        val dayIndex = date.dayOfWeek.value % 7
        assertEquals(date, dateFromWeekPageIndex(weekPageIndexOf(date), dayIndex))
    }

    @Test
    fun `year page index round-trips through a date`() {
        val date = LocalDate.of(2026, 10, 14)
        assertEquals(date, dateFromYearPageIndex(yearPageIndexOf(date), date.monthValue, date.dayOfMonth))
    }

    @Test
    fun `month page index increases by exactly 1 for the next month`() {
        val a = monthPageIndexOf(LocalDate.of(2026, 10, 1))
        val b = monthPageIndexOf(LocalDate.of(2026, 11, 1))
        assertEquals(1, b - a)
    }

    @Test
    fun `year page index increases by exactly 1 for the next year`() {
        val a = yearPageIndexOf(LocalDate.of(2026, 10, 14))
        val b = yearPageIndexOf(LocalDate.of(2027, 10, 14))
        assertEquals(1, b - a)
    }
}
