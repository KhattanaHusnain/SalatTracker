package com.salattracker.feature.goals

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.testutil.FakeGoalRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
        id = id, name = "Goal $id", category = GoalCategory.QURAN, frequency = frequency, targetAmount = target,
        unit = "Pages", reminderEnabled = false, reminderTime = null, startDate = start, durationDays = duration,
        isActive = active, daysOfWeek = days, endsAfterDuration = endsAfter,
    )

    private fun viewModel(vararg histories: GoalHistory): GoalsViewModel {
        repository.seedHistories(histories.toList())
        return GoalsViewModel(repository, clock)
    }

    @Before
    fun setUp() {
        repository = FakeGoalRepository()
    }

    @Test
    fun `active goals and archived or completed goals are split`() = runTest {
        val vm = viewModel(
            GoalHistory(goal(1), emptyList()),
            GoalHistory(goal(2, active = false), emptyList()),
            GoalHistory(goal(3, endsAfter = true, duration = 5), emptyList()),
        )
        val state = vm.uiState.value
        assertEquals(listOf(1L), state.active.map { it.goal.id })
        assertEquals(listOf(3L, 2L), state.archived.map { it.goal.id }) // completed first
        assertEquals(GoalStatus.COMPLETED, state.archived.first().status)
        assertTrue(state.hasAnyGoals)
    }

    @Test
    fun `today summary counts due daily goals and excludes excused and not-due ones`() = runTest {
        val notDueToday = (DayOfWeek.values().toSet() - today.dayOfWeek)
        val vm = viewModel(
            GoalHistory(goal(1), listOf(GoalCheckIn(today, 1))),                    // done
            GoalHistory(goal(2), emptyList()),                                      // open
            GoalHistory(goal(3), listOf(GoalCheckIn(today, 0, isSkip = true))),     // excused -> left out
            GoalHistory(goal(4, days = notDueToday), emptyList()),                  // not due -> left out
            GoalHistory(goal(5, target = 3, frequency = GoalFrequency.WEEKLY), listOf(GoalCheckIn(today, 3))),
        )
        val summary = vm.uiState.value.today
        assertEquals(1, summary.dailyDone)
        assertEquals(2, summary.dailyTotal)
        assertEquals(1, summary.periodicDone)
        assertEquals(1, summary.periodicTotal)
    }

    @Test
    fun `the strip covers the last seven days ending today`() = runTest {
        val vm = viewModel(GoalHistory(goal(1), emptyList()))
        val strip = vm.uiState.value.active.single().strip
        assertEquals(7, strip.size)
        assertEquals(today, strip.last().date)
        assertEquals(today.minusDays(6), strip.first().date)
    }

    @Test
    fun `tapping an open single-step day logs it, tapping it again clears it`() = runTest {
        val vm = viewModel(GoalHistory(goal(1), emptyList()))
        vm.onDayTap(1, today)
        assertEquals(listOf(Triple(1L, 1, today)), repository.checkIns)

        repository.seedHistories(listOf(GoalHistory(goal(1), listOf(GoalCheckIn(today, 1)))))
        vm.onDayTap(1, today)
        assertEquals(FakeGoalRepository.DayLogCall(1, today, 0, false), repository.dayLogs.single())
    }

    @Test
    fun `tapping a multi-step day adds one and a complete one opens the editor`() = runTest {
        val vm = viewModel(GoalHistory(goal(1, target = 5), listOf(GoalCheckIn(today, 2))))
        vm.onDayTap(1, today)
        assertEquals(listOf(Triple(1L, 1, today)), repository.checkIns)

        repository.seedHistories(listOf(GoalHistory(goal(1, target = 5), listOf(GoalCheckIn(today, 5)))))
        vm.onDayTap(1, today)
        assertNotNull(vm.uiState.value.logSheet)
        assertEquals(5, vm.uiState.value.logSheet?.amount)
    }

    @Test
    fun `tapping a locked or not-due day does nothing`() = runTest {
        val vm = viewModel(GoalHistory(goal(1, start = today), emptyList())) // earlier strip days are before the start
        vm.onDayTap(1, today.minusDays(3))
        assertTrue(repository.checkIns.isEmpty())
        assertTrue(repository.dayLogs.isEmpty())
        assertNull(vm.uiState.value.logSheet)
    }

    @Test
    fun `long press opens the sheet and saving writes the day`() = runTest {
        val vm = viewModel(GoalHistory(goal(1, target = 5), emptyList()))
        val day = today.minusDays(2)
        vm.onDayLongPress(1, day)
        assertEquals(day, vm.uiState.value.logSheet?.date)

        vm.onLogAmountChanged(4)
        vm.onLogSave()
        assertEquals(FakeGoalRepository.DayLogCall(1, day, 4, false), repository.dayLogs.single())
        assertNull(vm.uiState.value.logSheet)
    }
}
