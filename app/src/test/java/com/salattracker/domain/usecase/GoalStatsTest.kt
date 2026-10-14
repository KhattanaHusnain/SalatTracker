package com.salattracker.domain.usecase

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GoalStatsTest {

    private fun dailyGoal(startDate: LocalDate, target: Int = 1) = SpiritualGoal(
        id = 1, name = "Quran", category = GoalCategory.QURAN, frequency = GoalFrequency.DAILY,
        targetAmount = target, unit = "Pages", reminderEnabled = false, reminderTime = null,
        startDate = startDate, durationDays = 30, isActive = true,
    )

    private fun weeklyGoal(startDate: LocalDate, target: Int = 7) = dailyGoal(startDate, target).copy(frequency = GoalFrequency.WEEKLY)

    @Test
    fun `a goal started mid-range counts only from its start date`() {
        // Range is 10 days (Oct 1-10), but the goal only started on Oct 5 — only 6 daily periods
        // (Oct 5-10) should be judged, not all 10.
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 5))
        val checkIns = (1..10).map { GoalCheckIn(LocalDate.of(2026, 10, it), 1) }
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(6, stat.periodsCounted)
        assertEquals(6, stat.periodsHit)
    }

    @Test
    fun `a weekly goal is judged per Sunday-start week, not per day`() {
        // Sunday Oct 4 - Saturday Oct 10 is one week. One check-in of the full target anywhere in
        // the week should hit that week once, not read as "1 of 7 days done".
        val goal = weeklyGoal(startDate = LocalDate.of(2026, 10, 4), target = 7)
        val checkIns = listOf(GoalCheckIn(LocalDate.of(2026, 10, 6), 7))
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 4), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(1, stat.periodsCounted)
        assertEquals(1, stat.periodsHit)
    }

    @Test
    fun `the current in-progress period is excluded until it is hit`() {
        // today is Oct 10 itself — not yet ended, and not yet hit — so it isn't counted.
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 1))
        val checkIns = (1..9).map { GoalCheckIn(LocalDate.of(2026, 10, it), 1) }
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 10),
        )
        assertEquals(9, stat.periodsCounted) // Oct 1-9, all ended and hit
        assertEquals(9, stat.periodsHit)
    }

    @Test
    fun `an in-progress period already hit counts early`() {
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 1))
        val checkIns = listOf(GoalCheckIn(LocalDate.of(2026, 10, 10), 1))
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 10), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 10),
        )
        assertEquals(1, stat.periodsCounted)
        assertEquals(1, stat.periodsHit)
    }

    @Test
    fun `loggedAmount only counts check-ins inside the range`() {
        val goal = dailyGoal(startDate = LocalDate.of(2026, 9, 1))
        val checkIns = listOf(
            GoalCheckIn(LocalDate.of(2026, 9, 28), 5),  // before range
            GoalCheckIn(LocalDate.of(2026, 10, 5), 3),  // inside range
            GoalCheckIn(LocalDate.of(2026, 10, 12), 9), // after range
        )
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(3, stat.loggedAmount)
    }

    @Test
    fun `a goal that starts after the range has nothing counted`() {
        val goal = dailyGoal(startDate = LocalDate.of(2026, 11, 1))
        val stat = goalPeriodStat(
            goal, emptyList(),
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(0, stat.periodsCounted)
        assertEquals(0, stat.periodsHit)
        assertEquals(0, stat.loggedAmount)
    }

    @Test
    fun `a daily goal only counts the weekdays it is due on`() {
        // Oct 1-10 2026: Thu Oct 1 .. Sat Oct 10. Due Mondays only -> just Oct 5.
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 1)).copy(daysOfWeek = setOf(DayOfWeek.MONDAY))
        val stat = goalPeriodStat(
            goal, listOf(GoalCheckIn(LocalDate.of(2026, 10, 5), 1)),
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(1, stat.periodsCounted)
        assertEquals(1, stat.periodsHit)
    }

    @Test
    fun `excused days are not counted as hit or missed`() {
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 1))
        val checkIns = listOf(
            GoalCheckIn(LocalDate.of(2026, 10, 1), 1),
            GoalCheckIn(LocalDate.of(2026, 10, 2), 0, isSkip = true),
        )
        val stat = goalPeriodStat(
            goal, checkIns,
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 3),
            today = LocalDate.of(2026, 10, 4),
        )
        // Oct 1 hit, Oct 2 excused (ignored), Oct 3 missed.
        assertEquals(2, stat.periodsCounted)
        assertEquals(1, stat.periodsHit)
    }

    @Test
    fun `nothing counts past the end date`() {
        val goal = dailyGoal(startDate = LocalDate.of(2026, 10, 1)).copy(endsAfterDuration = true, durationDays = 3)
        val stat = goalPeriodStat(
            goal, emptyList(),
            from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10),
            today = LocalDate.of(2026, 10, 11),
        )
        assertEquals(3, stat.periodsCounted)
    }
}
