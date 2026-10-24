package com.salattracker.feature.stats

import com.salattracker.domain.repository.StatusCounts
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var prayerRepository: FakePrayerRepository
    private lateinit var viewModel: StatsViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)

    @Before
    fun setUp() {
        prayerRepository = FakePrayerRepository()
    }

    private fun createViewModel() = StatsViewModel(prayerRepository, FakeSettingsRepository(), clock)

    @Test
    fun `initial state loads every stats section for the month period`() = runTest {
        prayerRepository.seedDailyCounts(today, StatusCounts(congregation = 5))
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.MONTH, state.period)
        assertEquals(false, state.overview.isLoading)
        assertEquals(false, state.prayerBreakdown.isLoading)
    }

    @Test
    fun `onPeriodChanged queries stats sections with the new period's date range`() = runTest {
        viewModel = createViewModel()
        prayerRepository.dailyStatusCountsCalls.clear()
        prayerRepository.prayerBreakdownCalls.clear()

        viewModel.onPeriodChanged(StatsPeriod.NINETY_DAYS)

        assertEquals(StatsPeriod.NINETY_DAYS, viewModel.uiState.value.period)

        val expectedFrom = today.minusDays(89)
        // getDailyStatusCounts is called twice: current period, then the previous period for the delta.
        assertEquals(expectedFrom to today, prayerRepository.dailyStatusCountsCalls[0])
        assertEquals(expectedFrom.minusDays(90) to expectedFrom.minusDays(1), prayerRepository.dailyStatusCountsCalls[1])

        assertEquals(expectedFrom to today, prayerRepository.prayerBreakdownCalls.single())
    }

    @Test
    fun `the delta is null when the previous period has nothing countable`() = runTest {
        prayerRepository.seedDailyCounts(today, StatusCounts(congregation = 5))
        viewModel = createViewModel()
        assertEquals(null, viewModel.uiState.value.overview.completionDelta)
    }

    @Test
    fun `the delta is computed against the previous period of the same length`() = runTest {
        prayerRepository.seedDailyCounts(today, StatusCounts(congregation = 8, qazaPending = 2)) // 80%
        prayerRepository.seedDailyCounts(today.minusDays(StatsPeriod.MONTH.days.toLong()), StatusCounts(congregation = 5, qazaPending = 5)) // 50%
        viewModel = createViewModel()
        assertEquals(30, viewModel.uiState.value.overview.completionDelta)
    }

    @Test
    fun `clearError resets the error`() = runTest {
        viewModel = createViewModel()
        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.errorRes)
    }
}
