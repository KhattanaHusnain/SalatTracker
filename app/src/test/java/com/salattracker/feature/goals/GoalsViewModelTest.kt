package com.salattracker.feature.goals

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.testutil.FakeGoalRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GoalsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)
    private lateinit var repository: FakeGoalRepository

    private fun goal(
        id: Long,
        target: Int = 1,
        frequency: GoalFrequency = GoalFrequency.DAILY,
        active: Boolean = true,
        endsAfter: Boolean = false,
        duration: Int = 30,
        days: Set<DayOfWeek> = emptySet(),
        start: LocalDate = today.minusDays(30),
    ) = SpiritualGoal(
        id, "Goal $id", GoalCategory.QURAN, frequency, target, "Pages", false, null,
        start, duration, active, days, endsAfter,
    )

    private fun viewModel(vararg histories: GoalHistory): GoalsViewModel {
        repository.seedHistories(histories.toList())
        return GoalsViewModel(repository, clock)
    }

    @Before fun setUp() { repository = FakeGoalRepository() }

    @Test fun `active goals and archived or completed goals are split`() = runTest {
        val vm = viewModel(
            GoalHistory(goal(1), emptyList()),
            GoalHistory(goal(2, active = false), emptyList()),
            GoalHistory(goal(3, endsAfter = true, duration = 5), emptyList()),
        )
        runCurrent()
        assertEquals(listOf(1L), vm.uiState.value.active.map { it.goal.id })
        assertEquals(listOf(3L, 2L), vm.uiState.value.archived.map { it.goal.id })
        assertEquals(GoalStatus.COMPLETED, vm.uiState.value.archived.first().status)
    }

    @Test fun `hero counts only due daily goals`() = runTest {
        val notDueToday = DayOfWeek.entries.toSet() - today.dayOfWeek
        val vm = viewModel(
            GoalHistory(goal(1), listOf(GoalCheckIn(today, 1))),
            GoalHistory(goal(2), emptyList()),
            GoalHistory(goal(3), listOf(GoalCheckIn(today, 0, true))),
            GoalHistory(goal(4, days = notDueToday), emptyList()),
            GoalHistory(goal(5, target = 3, frequency = GoalFrequency.WEEKLY), listOf(GoalCheckIn(today, 3))),
        )
        runCurrent()
        assertEquals(1, vm.uiState.value.today.dailyDone)
        assertEquals(2, vm.uiState.value.today.dailyTotal)
        assertEquals(1, vm.uiState.value.today.periodicDone)
    }

    @Test fun `week strip is Monday first and contains the current calendar week`() = runTest {
        val vm = viewModel(GoalHistory(goal(1), emptyList()))
        runCurrent()
        val strip = vm.uiState.value.active.single().strip
        assertEquals(DayOfWeek.MONDAY, strip.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, strip.last().date.dayOfWeek)
        assertTrue(strip.any { it.date == today && it.isToday })
    }

    @Test fun `quick action adds one unit to an eligible goal`() = runTest {
        val vm = viewModel(GoalHistory(goal(1, target = 3), listOf(GoalCheckIn(today, 1))))
        runCurrent()
        vm.onQuickAdd(1)
        runCurrent()
        assertEquals(listOf(Triple(1L, 1, today)), repository.checkIns)
    }

    @Test fun `quick action ignores complete excused and not due goals`() = runTest {
        val notDueToday = DayOfWeek.entries.toSet() - today.dayOfWeek
        val vm = viewModel(
            GoalHistory(goal(1), listOf(GoalCheckIn(today, 1))),
            GoalHistory(goal(2), listOf(GoalCheckIn(today, 0, true))),
            GoalHistory(goal(3, days = notDueToday), emptyList()),
        )
        runCurrent()
        vm.onQuickAdd(1); vm.onQuickAdd(2); vm.onQuickAdd(3)
        runCurrent()
        assertTrue(repository.checkIns.isEmpty())
    }
}
