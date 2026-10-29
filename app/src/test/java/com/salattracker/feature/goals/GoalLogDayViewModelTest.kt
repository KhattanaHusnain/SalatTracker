package com.salattracker.feature.goals

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
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
import java.time.LocalDate

class GoalLogDayViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val date = LocalDate.of(2026, 10, 18)
    private lateinit var repository: FakeGoalRepository

    private fun goal(target: Int = 5) = SpiritualGoal(
        7, "Read Quran", GoalCategory.QURAN, GoalFrequency.DAILY, target, "Pages", false, null,
        date.minusDays(20), 30, true,
    )

    @Before fun setUp() { repository = FakeGoalRepository() }

    private fun viewModel(target: Int = 5, amount: Int = 0): GoalLogDayViewModel {
        repository.seedHistories(listOf(GoalHistory(goal(target), if (amount > 0) listOf(GoalCheckIn(date, amount)) else emptyList())))
        return GoalLogDayViewModel(repository, SavedStateHandle(mapOf("goalId" to 7L, "date" to date.toString())))
    }

    @Test fun `loads the selected day and saves each increment immediately`() = runTest {
        val vm = viewModel(amount = 2); runCurrent()
        assertEquals(2, vm.uiState.value.amount)
        vm.increment(); runCurrent()
        assertEquals(3, vm.uiState.value.amount)
        assertEquals(FakeGoalRepository.DayLogCall(7, date, 3, false), repository.dayLogs.single())
    }

    @Test fun `skip and clear preserve exact day semantics`() = runTest {
        val vm = viewModel(amount = 2); runCurrent()
        vm.setSkipped(true); runCurrent()
        assertTrue(repository.dayLogs.last().skipped)
        assertEquals(0, repository.dayLogs.last().amount)
        vm.clear(); runCurrent()
        assertFalse(repository.dayLogs.last().skipped)
        assertEquals(0, repository.dayLogs.last().amount)
    }

    @Test fun `target one uses mark done`() = runTest {
        val vm = viewModel(target = 1); runCurrent()
        vm.markDone(); runCurrent()
        assertEquals(1, vm.uiState.value.amount)
        assertEquals(1, repository.dayLogs.single().amount)
    }
}
