package com.salattracker.feature.goals

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalDayState
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.testutil.FakeGoalReminderScheduler
import com.salattracker.testutil.FakeGoalRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class GoalDetailsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)
    private lateinit var repository: FakeGoalRepository
    private lateinit var scheduler: FakeGoalReminderScheduler

    private fun goal(active: Boolean = true, startDate: LocalDate = today.minusDays(40)) = SpiritualGoal(
        7, "Quran", GoalCategory.QURAN, GoalFrequency.DAILY, 5, "Pages", false, null,
        startDate, 30, active,
    )

    @Before fun setUp() { repository = FakeGoalRepository(); scheduler = FakeGoalReminderScheduler() }

    private fun viewModel(history: GoalHistory = GoalHistory(goal(), emptyList())): GoalDetailsViewModel {
        repository.seedHistories(listOf(history))
        return GoalDetailsViewModel(repository, scheduler, SavedStateHandle(mapOf("goalId" to 7L)), clock)
    }

    @Test fun `loads goal statistics and Monday first month grid`() = runTest {
        val checkIns = (1..3).map { GoalCheckIn(today.minusDays(it.toLong()), 5) }
        val vm = viewModel(GoalHistory(goal(), checkIns))
        runCurrent()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Quran", state.goal?.name)
        assertEquals(3, state.streak.current)
        assertEquals(42, state.cells.size)
        assertEquals(DayOfWeek.MONDAY, state.cells.first().date.dayOfWeek)
        assertEquals(GoalStatus.ACTIVE, state.status)
        assertEquals(GoalDayState.OPEN, state.cells.first { it.date == today }.state)
    }

    @Test fun `month navigation remains bounded`() = runTest {
        val vm = viewModel(); runCurrent()
        assertFalse(vm.uiState.value.canGoNextMonth)
        assertTrue(vm.uiState.value.canGoPrevMonth)
        vm.onPrevMonth(); runCurrent()
        assertEquals(YearMonth.now(clock).minusMonths(1), vm.uiState.value.visibleMonth)
        vm.onNextMonth(); runCurrent()
        assertEquals(YearMonth.now(clock), vm.uiState.value.visibleMonth)
    }

    @Test fun `archiving cancels and restoring reschedules`() = runTest {
        val vm = viewModel(); runCurrent()
        vm.onArchiveToggle(); runCurrent()
        assertEquals(listOf(7L to true), repository.archiveCalls)
        assertEquals(listOf(7L), scheduler.cancelled)
        repository.seedHistories(listOf(GoalHistory(goal(active = false), emptyList()))); runCurrent()
        vm.onArchiveToggle(); runCurrent()
        assertEquals(7L to false, repository.archiveCalls.last())
        assertEquals(1, scheduler.rescheduleCount)
    }

    @Test fun `delete removes goal and cancels reminder`() = runTest {
        val vm = viewModel(); runCurrent()
        vm.onDeleteClick()
        assertTrue(vm.uiState.value.showDeleteConfirm)
        vm.onConfirmDelete(); runCurrent()
        assertEquals(listOf(7L), repository.deletedGoalIds)
        assertEquals(listOf(7L), scheduler.cancelled)
        assertTrue(vm.uiState.value.deleted)
    }
}
