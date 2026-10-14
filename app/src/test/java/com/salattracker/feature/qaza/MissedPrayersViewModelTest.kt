package com.salattracker.feature.qaza

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MissedPrayersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePrayerRepository

    private val today: LocalDate = LocalDate.now()

    @Before
    fun setUp() {
        repository = FakePrayerRepository()
    }

    private fun createViewModel(prayer: String? = null) = MissedPrayersViewModel(
        repository,
        SavedStateHandle(if (prayer != null) mapOf("prayer" to prayer) else emptyMap()),
    )

    @Test
    fun `no prayer arg starts unfiltered`() {
        val viewModel = createViewModel()
        assertNull(viewModel.uiState.value.filter)
    }

    @Test
    fun `arriving from a prayer tile preselects that filter`() {
        val viewModel = createViewModel(prayer = "FAJR")
        assertEquals(PrayerType.FAJR, viewModel.uiState.value.filter)
    }

    @Test
    fun `a blank or unrecognised prayer arg starts unfiltered`() {
        assertNull(createViewModel(prayer = "").uiState.value.filter)
        assertNull(createViewModel(prayer = "NOT_A_PRAYER").uiState.value.filter)
    }

    @Test
    fun `groups pending prayers by month, newest month first`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(LocalDate.of(today.year - 1, 1, 15), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()

        val months = viewModel.uiState.value.months
        assertEquals(2, months.size)
        assertEquals(YearMonth.from(today), months.first().month)
    }

    @Test
    fun `onFilterSelected narrows the grouped days to one prayer`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(1), PrayerType.ISHA, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()

        viewModel.onFilterSelected(PrayerType.FAJR)

        val day = viewModel.uiState.value.months.single().days.single()
        assertEquals(listOf(PrayerType.FAJR), day.prayers)
    }

    @Test
    fun `onToggleSortOrder flips day order`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(5), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()

        // default: newest first
        assertEquals(today.minusDays(1), viewModel.uiState.value.months.single().days.first().date)

        viewModel.onToggleSortOrder()

        assertEquals(today.minusDays(5), viewModel.uiState.value.months.single().days.first().date)
    }

    @Test
    fun `long-pressing a pill starts selection mode with it selected`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        val slot = QazaSlot(today.minusDays(1), PrayerType.FAJR)

        viewModel.onPillLongClick(slot)

        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(setOf(slot), viewModel.uiState.value.selectedSlots)
    }

    @Test
    fun `tapping a pill in selection mode toggles it instead of marking it up`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        val slot = QazaSlot(today.minusDays(1), PrayerType.FAJR)
        viewModel.onPillLongClick(slot) // enters selection mode, slot selected

        viewModel.onPillClick(slot) // toggles it back off

        assertTrue(viewModel.uiState.value.selectedSlots.isEmpty())
        assertEquals(0, repository.bulkUpserts.size) // never actually marked up
    }

    @Test
    fun `tapping a pill outside selection mode marks it up immediately`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        val slot = QazaSlot(today.minusDays(1), PrayerType.FAJR)

        viewModel.onPillClick(slot)

        assertEquals(1, repository.bulkUpserts.size)
        assertEquals(PrayerStatus.QAZA_COMPLETED, repository.bulkUpserts.last().second)
    }

    @Test
    fun `onSelectAll selects every slot currently visible, onClearSelection empties it`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(1), PrayerType.ISHA, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        viewModel.onSelectClick()

        viewModel.onSelectAll()
        assertEquals(2, viewModel.uiState.value.selectedSlots.size)

        viewModel.onClearSelection()
        assertTrue(viewModel.uiState.value.selectedSlots.isEmpty())
    }

    @Test
    fun `onMakeUpSelected marks every selected slot and exits selection mode`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        repository.seedStatus(today.minusDays(1), PrayerType.ISHA, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        viewModel.onSelectClick()
        viewModel.onSelectAll()

        viewModel.onMakeUpSelected()

        assertEquals(false, viewModel.uiState.value.isSelectionMode)
        assertEquals(0, viewModel.uiState.value.totalPending)
        assertEquals(2, viewModel.uiState.value.undo?.count)
    }

    @Test
    fun `onCancelSelection exits selection mode without marking anything up`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        viewModel.onSelectClick()
        viewModel.onSelectAll()

        viewModel.onCancelSelection()

        assertEquals(false, viewModel.uiState.value.isSelectionMode)
        assertEquals(0, repository.bulkUpserts.size)
        assertEquals(1, viewModel.uiState.value.totalPending)
    }

    @Test
    fun `onUndo reverts the slots behind the snackbar`() {
        repository.seedStatus(today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING)
        val viewModel = createViewModel()
        viewModel.onPillClick(QazaSlot(today.minusDays(1), PrayerType.FAJR))

        viewModel.onUndo()

        assertEquals(1, viewModel.uiState.value.totalPending)
        assertNull(viewModel.uiState.value.undo)
    }
}
