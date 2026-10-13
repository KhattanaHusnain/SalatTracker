package com.salattracker.domain.usecase

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Which of the four calendar views is showing. */
enum class CalendarViewType { MONTH, MONTH_GRID, WEEK, YEAR }

/** The earliest/latest date any calendar view will navigate to — a sanity bound for chevrons,
 * swipes and the go-to-date picker, not a real calendar limit. */
val CALENDAR_MIN_DATE: LocalDate = LocalDate.of(1950, 1, 1)
val CALENDAR_MAX_DATE: LocalDate = LocalDate.of(2100, 12, 31)

fun LocalDate.coerceToCalendarRange(): LocalDate = when {
    isBefore(CALENDAR_MIN_DATE) -> CALENDAR_MIN_DATE
    isAfter(CALENDAR_MAX_DATE)  -> CALENDAR_MAX_DATE
    else                        -> this
}

/** Sunday-start week containing [date] — matches the Sun-Sat grid used throughout Calendar. */
fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

/** The 42 dates (6 full Sun-Sat weeks) covering [month], including the leading/trailing days of
 * the neighbouring months needed to fill the grid out. Always Sunday-first. */
fun monthGridDates(month: YearMonth): List<LocalDate> {
    val gridStart = weekStartOf(month.atDay(1))
    return (0 until 42).map { gridStart.plusDays(it.toLong()) }
}

/** The 7 dates (Sun-Sat) of the week containing [date]. */
fun weekDatesOf(date: LocalDate): List<LocalDate> {
    val start = weekStartOf(date)
    return (0 until 7).map { start.plusDays(it.toLong()) }
}

/** Moves the anchor date of [view] by [direction] periods (+1 forward, -1 back), clamped to the
 * day-of-month/day-of-year that actually exists in the target period, and to the calendar's
 * overall [CALENDAR_MIN_DATE]/[CALENDAR_MAX_DATE] bounds. */
fun shiftPeriod(view: CalendarViewType, date: LocalDate, direction: Int): LocalDate {
    val shifted = when (view) {
        CalendarViewType.MONTH, CalendarViewType.MONTH_GRID -> {
            val target = YearMonth.from(date).plusMonths(direction.toLong())
            target.atDay(date.dayOfMonth.coerceAtMost(target.lengthOfMonth()))
        }
        CalendarViewType.WEEK -> date.plusDays(7L * direction)
        CalendarViewType.YEAR -> {
            val target = YearMonth.of(date.year + direction, date.monthValue)
            target.atDay(date.dayOfMonth.coerceAtMost(target.lengthOfMonth()))
        }
    }
    return shifted.coerceToCalendarRange()
}

/** The date range to fetch prayer entries for, given the current view and anchor date — the
 * visible grid plus one month of slack either side (Month/Month-grid), the visible week (Week),
 * or the whole year (Year) — so paging to an adjacent period rarely needs a fresh fetch. */
fun observedRange(view: CalendarViewType, date: LocalDate): Pair<LocalDate, LocalDate> = when (view) {
    CalendarViewType.MONTH, CalendarViewType.MONTH_GRID -> {
        val month = YearMonth.from(date)
        monthGridDates(month.minusMonths(1)).first() to monthGridDates(month.plusMonths(1)).last()
    }
    CalendarViewType.WEEK -> {
        val week = weekDatesOf(date)
        week.first() to week.last()
    }
    CalendarViewType.YEAR -> LocalDate.of(date.year, 1, 1) to LocalDate.of(date.year, 12, 31)
}

/** Whether the visible period does NOT contain [today] — drives showing a "Today" shortcut. */
fun isAwayFromToday(view: CalendarViewType, date: LocalDate, today: LocalDate): Boolean = when (view) {
    CalendarViewType.MONTH, CalendarViewType.MONTH_GRID -> YearMonth.from(date) != YearMonth.from(today)
    CalendarViewType.WEEK -> weekStartOf(date) != weekStartOf(today)
    CalendarViewType.YEAR -> date.year != today.year
}

// ── HorizontalPager index mapping ──────────────────────────────────────────────
// Each pager is keyed by a stable integer page index spanning [CALENDAR_MIN_DATE, CALENDAR_MAX_DATE].
// Going from a page back to a date needs a day/day-of-week/month-of-year to place within that
// page; callers pass the anchor date's own value, which is clamped to whatever the target page
// actually has (e.g. page-for-31st landing on a 30-day month).

private val MONTH_EPOCH: YearMonth = YearMonth.from(CALENDAR_MIN_DATE)
private val WEEK_EPOCH: LocalDate = weekStartOf(CALENDAR_MIN_DATE)
private val YEAR_EPOCH: Int = CALENDAR_MIN_DATE.year

fun monthPageIndexOf(date: LocalDate): Int =
    ChronoUnit.MONTHS.between(MONTH_EPOCH, YearMonth.from(date)).toInt()

fun dateFromMonthPageIndex(page: Int, dayOfMonth: Int): LocalDate {
    val month = MONTH_EPOCH.plusMonths(page.toLong())
    return month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))
}

fun weekPageIndexOf(date: LocalDate): Int =
    ChronoUnit.WEEKS.between(WEEK_EPOCH, weekStartOf(date)).toInt()

/** [dayOfWeekIndex]: 0 = Sunday .. 6 = Saturday, matching `LocalDate.dayOfWeek.value % 7`. */
fun dateFromWeekPageIndex(page: Int, dayOfWeekIndex: Int): LocalDate =
    WEEK_EPOCH.plusWeeks(page.toLong()).plusDays(dayOfWeekIndex.coerceIn(0, 6).toLong())

fun yearPageIndexOf(date: LocalDate): Int = date.year - YEAR_EPOCH

fun dateFromYearPageIndex(page: Int, monthValue: Int, dayOfMonth: Int): LocalDate {
    val month = YearMonth.of(YEAR_EPOCH + page, monthValue.coerceIn(1, 12))
    return month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))
}
