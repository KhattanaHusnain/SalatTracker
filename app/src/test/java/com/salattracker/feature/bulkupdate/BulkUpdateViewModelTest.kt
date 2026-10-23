package com.salattracker.feature.bulkupdate

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BulkUpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(status: String? = null, gender: Gender? = null) = BulkUpdateViewModel(
        FakePrayerRepository(),
        FakeSettingsRepository(UserPrefs(gender = gender)),
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

    @Test
    fun `the Congregation default falls back to Individual for a Female gender`() {
        val viewModel = createViewModel(gender = Gender.FEMALE)
        assertEquals(PrayerStatus.INDIVIDUAL, viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun `a gender-hidden status never overrides an explicit non-default selection`() {
        val viewModel = createViewModel(status = "QAZA_PENDING", gender = Gender.FEMALE)
        assertEquals(PrayerStatus.QAZA_PENDING, viewModel.uiState.value.selectedStatus)
    }
}
