package com.salattracker.feature.stats

import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.repository.PeriodStats
import com.salattracker.testutil.FakeGoalRepository
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var prayerRepository: FakePrayerRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        prayerRepository = FakePrayerRepository()
        goalRepository = FakeGoalRepository()
    }

    private fun createViewModel() = StatsViewModel(prayerRepository, goalRepository)

    @Test
    fun `initial state loads every section for the week period`() = runTest {
        prayerRepository.periodStats = PeriodStats(congregationPct = 70, individualPct = 20, missedPct = 10, completionPct = 90)
        goalRepository.dailyPoints = listOf(GoalDailyPoint(LocalDate.now(), 0.5f))
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.WEEK, state.period)
        assertEquals(false, state.summary.isLoading)
        assertEquals(90, state.summary.completionPct)
        assertEquals(false, state.statusMix.isLoading)
        assertEquals(false, state.prayerBreakdown.isLoading)
        assertEquals(false, state.goals.isLoading)
        assertEquals(1, state.goals.dailyPoints.size)
    }

    @Test
    fun `onPeriodChanged queries every section with the new period's date range`() = runTest {
        viewModel = createViewModel()
        prayerRepository.statsForPeriodCalls.clear()
        prayerRepository.statusTrendCalls.clear()
        prayerRepository.prayerBreakdownCalls.clear()

        viewModel.onPeriodChanged(StatsPeriod.NINETY_DAYS)

        assertEquals(StatsPeriod.NINETY_DAYS, viewModel.uiState.value.period)

        val today = LocalDate.now()
        val expectedFrom = today.minusDays(89)
        // getStatsForPeriod is called twice: current period, then the previous period for the delta.
        assertEquals(expectedFrom to today, prayerRepository.statsForPeriodCalls[0])
        assertEquals(expectedFrom.minusDays(90) to expectedFrom.minusDays(1), prayerRepository.statsForPeriodCalls[1])

        assertEquals(Triple(expectedFrom, today, 90), prayerRepository.statusTrendCalls.single())
        assertEquals(expectedFrom to today, prayerRepository.prayerBreakdownCalls.single())
    }

    @Test
    fun `onPeriodChanged beyond 40 days switches goals to the per-goal breakdown`() = runTest {
        goalRepository.completionBreakdown = listOf(GoalCompletionStat("Quran", GoalCategory.QURAN, 0.6f))
        viewModel = createViewModel()

        viewModel.onPeriodChanged(StatsPeriod.NINETY_DAYS)

        val goals = viewModel.uiState.value.goals
        assertTrue(goals.dailyPoints.isEmpty())
        assertEquals(1, goals.items.size)
        assertEquals("Quran", goals.items.first().goalName)
    }

    @Test
    fun `onPeriodChanged at 40 days or under keeps the daily breakdown`() = runTest {
        goalRepository.dailyPoints = listOf(GoalDailyPoint(LocalDate.now(), 1f))
        viewModel = createViewModel()

        viewModel.onPeriodChanged(StatsPeriod.FORTY_DAYS)

        val goals = viewModel.uiState.value.goals
        assertTrue(goals.items.isEmpty())
        assertEquals(1, goals.dailyPoints.size)
    }

    @Test
    fun `the delta is computed against the previous period of the same length`() = runTest {
        prayerRepository.periodStats = PeriodStats(congregationPct = 0, individualPct = 0, missedPct = 0, completionPct = 80)
        viewModel = createViewModel()
        // Same fake stats for both the current and previous call -> delta should be 0.
        assertEquals(0, viewModel.uiState.value.summary.completionDelta)
    }

    @Test
    fun `clearError resets the error`() = runTest {
        viewModel = createViewModel()
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.error)
    }
}
