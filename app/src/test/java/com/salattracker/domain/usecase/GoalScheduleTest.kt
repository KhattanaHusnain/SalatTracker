package com.salattracker.domain.usecase

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GoalScheduleTest {

    // 2026-10-14 is a Wednesday.
    private val today = LocalDate.of(2026, 10, 14)
    private val start = LocalDate.of(2026, 10, 1)

    private fun goal(
        frequency: GoalFrequency = GoalFrequency.DAILY,
        target: Int = 1,
        days: Set<DayOfWeek> = emptySet(),
        endsAfter: Boolean = false,
        duration: Int = 30,
        startDate: LocalDate = start,
        reminder: LocalTime? = null,
        active: Boolean = true,
    ) = SpiritualGoal(
        id = 1, name = "G", category = GoalCategory.QURAN, frequency = frequency, targetAmount = target,
        unit = "Pages", reminderEnabled = reminder != null, reminderTime = reminder, startDate = startDate,
        durationDays = duration, isActive = active, daysOfWeek = days, endsAfterDuration = endsAfter,
    )

    private fun ci(day: Int, amount: Int = 1, skip: Boolean = false) =
        GoalCheckIn(LocalDate.of(2026, 10, day), amount, skip)

    // ── due days / end date ───────────────────────────────────────────────────

    @Test
    fun `an empty weekday selection means every day`() {
        assertTrue(goal().isDueOn(today))
    }

    @Test
    fun `a weekday selection restricts daily goals`() {
        val g = goal(days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertFalse(g.isDueOn(today))                       // Wednesday
        assertTrue(g.isDueOn(LocalDate.of(2026, 10, 15)))   // Thursday
    }

    @Test
    fun `weekly goals ignore the weekday selection`() {
        assertTrue(goal(frequency = GoalFrequency.WEEKLY, days = setOf(DayOfWeek.MONDAY)).isDueOn(today))
    }

    @Test
    fun `not due before start or after the end date`() {
        val g = goal(endsAfter = true, duration = 10) // Oct 1 .. Oct 10
        assertFalse(g.isDueOn(LocalDate.of(2026, 9, 30)))
        assertTrue(g.isDueOn(LocalDate.of(2026, 10, 10)))
        assertFalse(g.isDueOn(LocalDate.of(2026, 10, 11)))
        assertEquals(LocalDate.of(2026, 10, 10), g.endDate)
    }

    @Test
    fun `duration is ignored unless the goal ends`() {
        assertNull(goal(endsAfter = false, duration = 10).endDate)
        assertFalse(goal(endsAfter = false, duration = 10).isFinished(today))
    }

    @Test
    fun `isFinished and dayIndex follow the end date`() {
        val g = goal(endsAfter = true, duration = 30) // Oct 1 .. Oct 30
        assertFalse(g.isFinished(today))
        assertEquals(14, g.dayIndex(today))
        assertTrue(g.isFinished(LocalDate.of(2026, 10, 31)))
        assertEquals(30, g.dayIndex(LocalDate.of(2026, 12, 1)))
        assertNull(goal().dayIndex(today))
    }

    @Test
    fun `weekday mask round trips`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        assertEquals(days, daysOfWeekFromMask(days.toDaysMask()))
        assertEquals(0, emptySet<DayOfWeek>().toDaysMask())
    }

    // ── day state ─────────────────────────────────────────────────────────────

    @Test
    fun `daily day states`() {
        val g = goal(target = 5, days = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY))
        val yesterday = today.minusDays(1) // Tuesday, due
        val monday = today.minusDays(2)    // not due
        assertEquals(GoalDayState.DONE, goalDayState(g, yesterday, 5, false, today))
        assertEquals(GoalDayState.PARTIAL, goalDayState(g, yesterday, 2, false, today))
        assertEquals(GoalDayState.MISSED, goalDayState(g, yesterday, 0, false, today))
        assertEquals(GoalDayState.SKIPPED, goalDayState(g, yesterday, 0, true, today))
        assertEquals(GoalDayState.NOT_DUE, goalDayState(g, monday, 0, false, today))
        assertEquals(GoalDayState.OPEN, goalDayState(g, today, 0, false, today))
        assertEquals(GoalDayState.LOCKED, goalDayState(g, today.plusDays(1), 0, false, today))
        assertEquals(GoalDayState.LOCKED, goalDayState(g, start.minusDays(1), 0, false, today))
    }

    @Test
    fun `periodic goals show logged or none`() {
        val g = goal(frequency = GoalFrequency.WEEKLY, target = 3)
        assertEquals(GoalDayState.LOGGED, goalDayState(g, today.minusDays(1), 1, false, today))
        assertEquals(GoalDayState.NONE, goalDayState(g, today.minusDays(1), 0, false, today))
        assertEquals(GoalDayState.OPEN, goalDayState(g, today, 0, false, today))
    }

    @Test
    fun `current period amount ignores excused days and older periods`() {
        val g = goal(frequency = GoalFrequency.WEEKLY, target = 5)
        // Week of Wed Oct 14 starts Sun Oct 11.
        val amount = currentPeriodAmount(g, listOf(ci(9, 4), ci(11, 2), ci(13, 1), ci(12, 9, skip = true)), today)
        assertEquals(3, amount)
    }

    // ── streak ────────────────────────────────────────────────────────────────

    @Test
    fun `daily streak counts consecutive hits and an unfinished today is neutral`() {
        // Hit Oct 11, 12, 13; today (14) not logged yet.
        val s = goalStreak(goal(startDate = LocalDate.of(2026, 10, 11)), listOf(ci(11), ci(12), ci(13)), today)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `a missed past day resets the current streak but keeps the best`() {
        // Hit Oct 1-3, missed Oct 4, hit Oct 5-6, today Oct 7 (not logged yet).
        val s = goalStreak(goal(), listOf(ci(1), ci(2), ci(3), ci(5), ci(6)), LocalDate.of(2026, 10, 7))
        assertEquals(2, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `not due and excused days neither extend nor break a streak`() {
        // Due Mon+Wed only. Oct 12 Mon hit, Oct 13 Tue not due, Oct 14 Wed (today) hit.
        val g = goal(days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), startDate = LocalDate.of(2026, 10, 12))
        assertEquals(2, goalStreak(g, listOf(ci(12), ci(14)), today).current)

        // Daily goal: Oct 12 hit, Oct 13 excused, Oct 14 hit => streak 2, not broken.
        val d = goal(startDate = LocalDate.of(2026, 10, 12))
        assertEquals(2, goalStreak(d, listOf(ci(12), ci(13, 0, skip = true), ci(14)), today).current)
    }

    @Test
    fun `weekly streak counts hit weeks and the in-progress week is neutral`() {
        // Weeks (Sun-start): Oct 4-10 and Oct 11-17 (current). Target 2. Start Oct 4.
        val g = goal(frequency = GoalFrequency.WEEKLY, target = 2, startDate = LocalDate.of(2026, 10, 4))
        val s = goalStreak(g, listOf(ci(5), ci(6)), today) // last week hit, this week empty so far
        assertEquals(1, s.current)
        assertEquals(1, s.best)
    }

    @Test
    fun `streak stops at the end date`() {
        val g = goal(endsAfter = true, duration = 3) // Oct 1-3
        val s = goalStreak(g, listOf(ci(1), ci(2), ci(3)), today)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
    }

    // ── reminders ─────────────────────────────────────────────────────────────

    @Test
    fun `next reminder is later today when the time is still ahead`() {
        val g = goal(reminder = LocalTime.of(20, 0))
        assertEquals(LocalDateTime.of(2026, 10, 14, 20, 0), nextReminderAt(g, LocalDateTime.of(2026, 10, 14, 9, 0)))
    }

    @Test
    fun `next reminder skips days the goal is not due`() {
        val g = goal(reminder = LocalTime.of(8, 0), days = setOf(DayOfWeek.FRIDAY))
        assertEquals(LocalDateTime.of(2026, 10, 16, 8, 0), nextReminderAt(g, LocalDateTime.of(2026, 10, 14, 9, 0)))
    }

    @Test
    fun `no reminder when off, archived, or after the end date`() {
        val now = LocalDateTime.of(2026, 10, 14, 9, 0)
        assertNull(nextReminderAt(goal(reminder = null), now))
        assertNull(nextReminderAt(goal(reminder = LocalTime.of(8, 0), active = false), now))
        assertNull(nextReminderAt(goal(reminder = LocalTime.of(8, 0), endsAfter = true, duration = 5), now))
    }
}
