package com.salattracker.feature.calendar

import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.CALENDAR_MIN_DATE
import com.salattracker.domain.usecase.CalendarViewType
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.FakeReminderNotifications
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.FakeVerseRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var prayerRepository: FakePrayerRepository
    private lateinit var reminderNotifications: FakeReminderNotifications
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        prayerRepository = FakePrayerRepository()
        reminderNotifications = FakeReminderNotifications()
    }

    private fun createViewModel() = CalendarViewModel(
        prayerRepository, FakeVerseRepository(), FakeSettingsRepository(), reminderNotifications,
    )

    @Test
    fun `initial state loads today's marks and the verse`() = runTest {
        prayerRepository.seedStatus(LocalDate.now(), PrayerType.FAJR, PrayerStatus.CONGREGATION)
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoadingMarks)
        assertEquals(PrayerStatus.CONGREGATION, state.marks[LocalDate.now()]?.get(PrayerType.FAJR))
        assertEquals("test verse", state.verse?.text)
    }

    @Test
    fun `onNextPeriod then onPreviousPeriod returns to the start`() = runTest {
        viewModel = createViewModel()
        viewModel.onViewChanged(CalendarViewType.WEEK) // ±7 days, no month-length clamping to worry about
        val start = viewModel.uiState.value.selectedDate

        viewModel.onNextPeriod()
        assertEquals(start.plusDays(7), viewModel.uiState.value.selectedDate)

        viewModel.onPreviousPeriod()
        assertEquals(start, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `paging to a different period refetches, but reselecting the same date does not`() = runTest {
        viewModel = createViewModel()
        val callsAfterInit = prayerRepository.observeMonthEntriesCallCount

        viewModel.onDaySelected(viewModel.uiState.value.selectedDate) // same date -> same observed range
        assertEquals(callsAfterInit, prayerRepository.observeMonthEntriesCallCount)

        viewModel.onNextPeriod() // different month -> different observed range
        assertTrue(prayerRepository.observeMonthEntriesCallCount > callsAfterInit)
    }

    @Test
    fun `onDatePicked clamps to the calendar range`() = runTest {
        viewModel = createViewModel()
        viewModel.onDatePicked(LocalDate.of(1900, 1, 1))
        assertEquals(CALENDAR_MIN_DATE, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `onPrayerClick opens the status sheet with the recorded status`() = runTest {
        val date = LocalDate.now().minusDays(1)
        prayerRepository.seedStatus(date, PrayerType.DHUHR, PrayerStatus.INDIVIDUAL)
        viewModel = createViewModel()

        viewModel.onPrayerClick(date, PrayerType.DHUHR)

        assertEquals(PrayerStatusSheetArgs(PrayerType.DHUHR, date, PrayerStatus.INDIVIDUAL), viewModel.uiState.value.statusSheet)
    }

    @Test
    fun `onPrayerClick on an unrecorded past day defaults to NOT_RECORDED`() = runTest {
        viewModel = createViewModel()
        val date = LocalDate.now().minusDays(2)

        viewModel.onPrayerClick(date, PrayerType.ASR)

        assertEquals(PrayerStatus.NOT_RECORDED, viewModel.uiState.value.statusSheet?.currentStatus)
    }

    @Test
    fun `onPrayerClick on a future day does nothing`() = runTest {
        viewModel = createViewModel()
        val future = LocalDate.now().plusDays(3)

        viewModel.onPrayerClick(future, PrayerType.FAJR)

        assertNull(viewModel.uiState.value.statusSheet)
    }

    @Test
    fun `onStatusSave persists and clears the sheet, and the mark updates reactively`() = runTest {
        val date = LocalDate.now().minusDays(1)
        viewModel = createViewModel()
        viewModel.onPrayerClick(date, PrayerType.MAGHRIB)
        assertEquals(PrayerStatus.NOT_RECORDED, viewModel.uiState.value.statusSheet?.currentStatus)

        viewModel.onStatusSave(PrayerStatus.CONGREGATION)

        assertNull(viewModel.uiState.value.statusSheet)
        assertEquals(false, viewModel.uiState.value.isSaving)
        assertEquals(PrayerStatus.CONGREGATION, viewModel.uiState.value.marks[date]?.get(PrayerType.MAGHRIB))
        assertEquals(listOf(Triple(date, PrayerType.MAGHRIB, PrayerStatus.CONGREGATION)), prayerRepository.upserts)
        assertTrue(reminderNotifications.dismissed.isEmpty())
    }

    @Test
    fun `onStatusSave dismisses today's prayer reminder`() = runTest {
        prayerRepository.seedTimes(LocalDate.now(), mapOf(PrayerType.FAJR to LocalTime.MIN))
        viewModel = createViewModel()
        viewModel.onPrayerClick(LocalDate.now(), PrayerType.FAJR)

        viewModel.onStatusSave(PrayerStatus.INDIVIDUAL)

        assertEquals(listOf(PrayerType.FAJR), reminderNotifications.dismissed)
    }

    @Test
    fun `onStatusSheetDismiss clears the sheet without saving`() = runTest {
        viewModel = createViewModel()
        viewModel.onPrayerClick(LocalDate.now().minusDays(1), PrayerType.FAJR)

        viewModel.onStatusSheetDismiss()

        assertNull(viewModel.uiState.value.statusSheet)
        assertTrue(prayerRepository.upserts.isEmpty())
    }

    @Test
    fun `onTodayClick returns the selected date to today`() = runTest {
        viewModel = createViewModel()
        viewModel.onNextPeriod()
        assertTrue(viewModel.uiState.value.awayFromToday)

        viewModel.onTodayClick()

        assertEquals(viewModel.uiState.value.today, viewModel.uiState.value.selectedDate)
        assertEquals(false, viewModel.uiState.value.awayFromToday)
    }

    @Test
    fun `onOpenDay jumps to Month view with that date selected, clamped to the calendar range`() = runTest {
        viewModel = createViewModel()
        viewModel.onMonthTileClick(java.time.YearMonth.of(viewModel.uiState.value.selectedDate.year, 3)) // sets cameFromYear
        viewModel.onViewChanged(CalendarViewType.YEAR)

        viewModel.onOpenDay(LocalDate.of(1900, 1, 1))

        val state = viewModel.uiState.value
        assertEquals(CalendarViewType.MONTH, state.view)
        assertEquals(CALENDAR_MIN_DATE, state.selectedDate)
        assertNull(state.cameFromYear)
    }

    @Test
    fun `clearError resets the error`() = runTest {
        viewModel = createViewModel()
        assertNull(viewModel.uiState.value.error)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
