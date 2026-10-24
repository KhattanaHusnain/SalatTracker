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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class GoalDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)
    private lateinit var repository: FakeGoalRepository
    private lateinit var scheduler: FakeGoalReminderScheduler

    private fun goal(active: Boolean = true, startDate: LocalDate = today.minusDays(40)) = SpiritualGoal(
        id = 7, name = "Quran", category = GoalCategory.QURAN, frequency = GoalFrequency.DAILY,
        targetAmount = 5, unit = "Pages", reminderEnabled = false, reminderTime = null,
        startDate = startDate, durationDays = 30, isActive = active,
    )

    @Before
    fun setUp() {
        repository = FakeGoalRepository()
        scheduler = FakeGoalReminderScheduler()
    }

    private fun createViewModel(history: GoalHistory? = GoalHistory(goal(), emptyList())): GoalDetailsViewModel {
        history?.let { repository.seedHistories(listOf(it)) }
        return GoalDetailsViewModel(repository, scheduler, SavedStateHandle(mapOf("goalId" to 7L)), clock)
    }

    @Test
    fun `loads the goal with its streak and a month grid`() = runTest {
        val checkIns = (1..3).map { GoalCheckIn(today.minusDays(it.toLong()), 5) }
        val vm = createViewModel(GoalHistory(goal(), checkIns))

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Quran", state.goal?.name)
        assertEquals(3, state.streak.current)
        assertEquals(42, state.cells.size)
        assertEquals(GoalStatus.ACTIVE, state.status)
        assertEquals(GoalDayState.OPEN, state.cells.first { it.date == today }.state)
    }

    @Test
    fun `month navigation is bounded by the goal start and the current month`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.canGoNextMonth)
        assertTrue(vm.uiState.value.canGoPrevMonth)

        vm.onPrevMonth()
        assertEquals(YearMonth.now(clock).minusMonths(1), vm.uiState.value.visibleMonth)
        assertTrue(vm.uiState.value.canGoNextMonth)

        vm.onNextMonth()
        assertEquals(YearMonth.now(clock), vm.uiState.value.visibleMonth)
    }

    @Test
    fun `tapping a day opens the sheet pre-filled, future days do not`() = runTest {
        val yesterday = today.minusDays(1)
        val vm = createViewModel(GoalHistory(goal(), listOf(GoalCheckIn(yesterday, 3))))

        vm.onDayClick(yesterday)
        assertEquals(3, vm.uiState.value.logSheet?.amount)
        assertEquals(5, vm.uiState.value.logSheet?.target)

        vm.onLogDismiss()
        vm.onDayClick(today.plusDays(1))
        assertNull(vm.uiState.value.logSheet)
    }

    @Test
    fun `saving the sheet writes the day and closes it`() = runTest {
        val vm = createViewModel()
        vm.onDayClick(today)
        vm.onLogAmountChanged(4)
        vm.onLogSave()

        assertEquals(listOf(FakeGoalRepository.DayLogCall(7, today, 4, false)), repository.dayLogs)
        assertNull(vm.uiState.value.logSheet)
    }

    @Test
    fun `mark done fills the target and excusing a day is saved as skipped`() = runTest {
        val vm = createViewModel()
        vm.onDayClick(today)
        vm.onLogMarkDone()
        assertEquals(5, vm.uiState.value.logSheet?.amount)

        vm.onLogSkippedChanged(true)
        vm.onLogSave()
        assertEquals(true, repository.dayLogs.single().skipped)
    }

    @Test
    fun `clearing a day logs zero`() = runTest {
        val vm = createViewModel()
        vm.onDayClick(today)
        vm.onLogClear()
        assertEquals(FakeGoalRepository.DayLogCall(7, today, 0, false), repository.dayLogs.single())
    }

    @Test
    fun `archiving cancels the goal's reminder and restoring reschedules`() = runTest {
        val vm = createViewModel()
        vm.onArchiveToggle()
        assertEquals(listOf(7L to true), repository.archiveCalls)
        assertEquals(listOf(7L), scheduler.cancelled)

        repository.seedHistories(listOf(GoalHistory(goal(active = false), emptyList())))
        vm.onArchiveToggle()
        assertEquals(7L to false, repository.archiveCalls.last())
        assertEquals(1, scheduler.rescheduleCount)
    }

    @Test
    fun `confirming delete removes the goal, cancels its alarm and flags deleted`() = runTest {
        val vm = createViewModel()
        vm.onDeleteClick()
        assertTrue(vm.uiState.value.showDeleteConfirm)

        vm.onConfirmDelete()
        assertEquals(listOf(7L), repository.deletedGoalIds)
        assertEquals(listOf(7L), scheduler.cancelled)
        assertTrue(vm.uiState.value.deleted)
        assertNotNull(vm.uiState.value)
    }
}
