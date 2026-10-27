package com.salattracker.feature.calendar

import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.FakeReminderNotifications
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarV4ViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 18)
    private lateinit var repository: FakePrayerRepository

    @Before fun setup() {
        repository = FakePrayerRepository().apply {
            seedDailyCounts(today.minusDays(20), StatusCounts(individual = 1))
            seedTimes(today, PrayerType.values().associateWith { LocalTime.of(1, 0) })
        }
    }

    private fun viewModel() = CalendarV4ViewModel(
        repository, FakeSettingsRepository(), FakeReminderNotifications(), clock,
    )

    @Test fun `range selection clamps to tracking start and today`() = runTest {
        val vm = viewModel(); runCurrent(); vm.toggleEditMode()
        vm.selectRange(today.minusDays(100), today.plusDays(5))
        runCurrent()
        assertEquals(21 * 5, vm.uiState.value.selectedSlots.size)
        assertTrue(vm.uiState.value.selectedSlots.all { it.date in today.minusDays(20)..today })
    }

    @Test fun `qaza done preview includes only pending qaza slots`() = runTest {
        val day = today.minusDays(1)
        repository.seedStatus(day, PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(day, PrayerType.DHUHR, PrayerStatus.INDIVIDUAL)
        val vm = viewModel(); runCurrent(); vm.toggleEditMode(); vm.onDayTapped(day)
        vm.prepareBatch(PrayerStatus.QAZA_COMPLETED)
        assertEquals(1, vm.uiState.value.preview?.updates?.size)
        assertEquals(PrayerType.FAJR, vm.uiState.value.preview?.updates?.keys?.single()?.prayerType)
    }

    @Test fun `fill empty recomputes preview and excludes overwrites`() = runTest {
        val day = today.minusDays(1)
        repository.seedStatus(day, PrayerType.FAJR, PrayerStatus.INDIVIDUAL)
        val vm = viewModel(); runCurrent(); vm.toggleEditMode(); vm.onDayTapped(day)
        vm.prepareBatch(PrayerStatus.QAZA_PENDING)
        assertEquals(5, vm.uiState.value.preview?.updates?.size)
        vm.setFillEmptyOnly(true)
        assertEquals(4, vm.uiState.value.preview?.updates?.size)
        assertEquals(0, vm.uiState.value.preview?.overwriteCount)
    }

    @Test fun `apply then undo restores exact previous state`() = runTest {
        val day = today.minusDays(1)
        repository.seedStatus(day, PrayerType.FAJR, PrayerStatus.INDIVIDUAL)
        val vm = viewModel(); runCurrent(); vm.toggleEditMode(); vm.setPrayerFilter(PrayerType.FAJR); vm.onDayTapped(day)
        vm.prepareBatch(PrayerStatus.QAZA_PENDING); vm.applyPreview(); runCurrent()
        assertEquals(PrayerStatus.QAZA_PENDING, vm.uiState.value.entries[day]?.get(PrayerType.FAJR)?.status)
        assertEquals(1, vm.uiState.value.undoCount)
        vm.undoBatch(); runCurrent()
        assertEquals(PrayerStatus.INDIVIDUAL, vm.uiState.value.entries[day]?.get(PrayerType.FAJR)?.status)
        assertFalse(vm.uiState.value.undoCount != null)
    }
}
