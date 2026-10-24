package com.salattracker.feature.qaza

import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class QazaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePrayerRepository
    private lateinit var viewModel: QazaViewModel

    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC)
    private val today: LocalDate = LocalDate.now(clock)

    @Before
    fun setUp() {
        repository = FakePrayerRepository()
    }

    private fun createViewModel() = QazaViewModel(repository, clock).also { viewModel = it }

    @Test
    fun `loads outstanding total and per-prayer counts`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(1), PrayerType.ISHA, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(2), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        val state = viewModel.uiState.value
        assertEquals(3, state.progress.outstanding)
        assertEquals(2, state.pendingByPrayer[PrayerType.FAJR])
        assertEquals(1, state.pendingByPrayer[PrayerType.ISHA])
        assertEquals(0, state.pendingByPrayer[PrayerType.ASR])
        assertTrue(state.hasOutstanding)
    }

    @Test
    fun `onMakeUp writes QAZA_COMPLETED and the undo count is 1`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        viewModel.onMakeUp(QazaSlot(today.minusDays(1), PrayerType.FAJR))

        assertEquals(0, viewModel.uiState.value.progress.outstanding)
        assertEquals(1, viewModel.uiState.value.undo?.count)
        assertEquals(1, repository.bulkUpserts.size)
        assertEquals(PrayerStatus.QAZA_COMPLETED, repository.bulkUpserts.last().second)
    }

    @Test
    fun `onUndo reverts the slots behind the snackbar back to pending`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        viewModel.onMakeUp(QazaSlot(today.minusDays(1), PrayerType.FAJR))
        viewModel.onUndo()

        assertEquals(1, viewModel.uiState.value.progress.outstanding)
        assertNull(viewModel.uiState.value.undo)
        assertEquals(PrayerStatus.QAZA_PENDING, repository.bulkUpserts.last().second)
    }

    @Test
    fun `onUndo is a no-op once the window has already expired`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        viewModel.onMakeUp(QazaSlot(today.minusDays(1), PrayerType.FAJR))
        // Simulates the undo window expiring (the real timer calls exactly this).
        viewModel.onUndoDismiss()
        val bulkUpsertsBefore = repository.bulkUpserts.size

        viewModel.onUndo()

        assertEquals(bulkUpsertsBefore, repository.bulkUpserts.size)
        assertEquals(0, viewModel.uiState.value.progress.outstanding)
    }

    @Test
    fun `onMakeUpDay marks every pending prayer on that day`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(1), PrayerType.ISHA, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(2), PrayerType.ASR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        val day = viewModel.uiState.value.latestDays.first { it.date == today.minusDays(1) }
        viewModel.onMakeUpDay(day)

        assertEquals(1, viewModel.uiState.value.progress.outstanding) // only the Asr day-2 entry remains
        assertEquals(2, viewModel.uiState.value.undo?.count)
    }

    @Test
    fun `onRecentUndo reverts one recently made-up entry directly, with no snackbar`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_COMPLETED, updatedAt = Instant.parse("2026-10-22T12:00:00Z").toEpochMilli())
        createViewModel()

        val entry = viewModel.uiState.value.recentMadeUp.first()
        viewModel.onRecentUndo(entry)

        assertNull(viewModel.uiState.value.undo)
        assertEquals(1, viewModel.uiState.value.progress.outstanding)
    }

    @Test
    fun `onLogClick seeds the sheet from current outstanding counts with zero selected`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(2), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()

        viewModel.onLogClick()

        val sheet = viewModel.uiState.value.logSheet
        assertTrue(sheet != null)
        val fajrRow = sheet!!.rows.first { it.prayerType == PrayerType.FAJR }
        assertEquals(2, fajrRow.outstanding)
        assertEquals(0, fajrRow.count)
    }

    @Test
    fun `onLogCountChanged clamps to the row's outstanding`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()
        viewModel.onLogClick()

        viewModel.onLogCountChanged(PrayerType.FAJR, 50)

        val row = viewModel.uiState.value.logSheet!!.rows.first { it.prayerType == PrayerType.FAJR }
        assertEquals(1, row.count)
    }

    @Test
    fun `onLogConfirm marks up the oldest pending prayers first and closes the sheet`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(5), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()
        viewModel.onLogClick()
        viewModel.onLogCountChanged(PrayerType.FAJR, 1)

        viewModel.onLogConfirm()

        assertNull(viewModel.uiState.value.logSheet)
        assertEquals(1, viewModel.uiState.value.progress.outstanding)
        val markedDate = repository.bulkUpserts.last().first.first().first
        assertEquals(today.minusDays(5), markedDate) // the older one was picked, not the newer
    }

    @Test
    fun `onLogConfirm with nothing selected does nothing`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        createViewModel()
        viewModel.onLogClick()

        viewModel.onLogConfirm()

        assertEquals(0, repository.bulkUpserts.size)
    }

    @Test
    fun `onRecentExpandToggle flips the expanded flag`() {
        createViewModel()
        assertFalse(viewModel.uiState.value.recentExpanded)
        viewModel.onRecentExpandToggle()
        assertTrue(viewModel.uiState.value.recentExpanded)
    }
}
