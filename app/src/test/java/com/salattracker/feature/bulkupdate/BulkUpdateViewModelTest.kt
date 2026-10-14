package com.salattracker.feature.bulkupdate

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BulkUpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(status: String? = null) = BulkUpdateViewModel(
        FakePrayerRepository(),
        SavedStateHandle(if (status != null) mapOf("status" to status) else emptyMap()),
    )

    @Test
    fun `no status arg keeps the Congregation default`() {
        val viewModel = createViewModel()
        assertEquals(PrayerStatus.CONGREGATION, viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun `a blank status arg keeps the Congregation default`() {
        val viewModel = createViewModel(status = "")
        assertEquals(PrayerStatus.CONGREGATION, viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun `an unrecognised status arg keeps the Congregation default`() {
        val viewModel = createViewModel(status = "NOT_A_STATUS")
        assertEquals(PrayerStatus.CONGREGATION, viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun `the Qaza tab's shortcut preselects Qaza Pending`() {
        val viewModel = createViewModel(status = "QAZA_PENDING")
        assertEquals(PrayerStatus.QAZA_PENDING, viewModel.uiState.value.selectedStatus)
    }
}
