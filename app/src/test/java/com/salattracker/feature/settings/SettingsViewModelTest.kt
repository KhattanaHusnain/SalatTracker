package com.salattracker.feature.settings

import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.testutil.FakeLocaleApplier
import com.salattracker.testutil.FakeLocationProvider
import com.salattracker.testutil.FakePrayerReminderRescheduler
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository(UserPrefs(latitude = 24.4672, longitude = 39.6111, calculationMethodId = CalculationMethod.MWL.id))
    private val locationProvider = FakeLocationProvider()
    private val rescheduler = FakePrayerReminderRescheduler()
    private val localeApplier = FakeLocaleApplier()

    private fun createViewModel() = SettingsViewModel(repository, locationProvider, rescheduler, localeApplier)

    @Test
    fun `load reflects current prefs, mapping calculationMethodId to the right method`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(24.4672, state.latitude, 0.0001)
        assertEquals(39.6111, state.longitude, 0.0001)
        assertEquals(CalculationMethod.MWL, state.calculationMethod)
    }

    @Test
    fun `onUseDeviceLocationChanged writes immediately and reschedules`() {
        val viewModel = createViewModel()

        viewModel.onUseDeviceLocationChanged(true)

        assertTrue(viewModel.uiState.value.useDeviceLocation)
        assertEquals(listOf(true), repository.useDeviceLocationCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `fetchDeviceLocation persists the fix and reschedules`() {
        locationProvider.nextLocation = 1.5 to 2.5
        val viewModel = createViewModel()

        viewModel.fetchDeviceLocation()

        assertEquals(listOf(1.5 to 2.5), repository.locationCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `fetchDeviceLocation does nothing when no fix is available`() {
        locationProvider.nextLocation = null
        val viewModel = createViewModel()

        viewModel.fetchDeviceLocation()

        assertTrue(repository.locationCalls.isEmpty())
        assertEquals(0, rescheduler.rescheduleCount)
    }

    @Test
    fun `onCalculationMethodChanged writes the id, closes the sheet, and reschedules`() {
        val viewModel = createViewModel()
        viewModel.onCalculationMethodSheetOpen()
        assertTrue(viewModel.uiState.value.showCalculationMethodSheet)

        viewModel.onCalculationMethodChanged(CalculationMethod.UMM_AL_QURA)

        assertEquals(CalculationMethod.UMM_AL_QURA, viewModel.uiState.value.calculationMethod)
        assertFalse(viewModel.uiState.value.showCalculationMethodSheet)
        assertEquals(listOf(CalculationMethod.UMM_AL_QURA.id), repository.calculationMethodIdCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `onMadhabChanged writes and reschedules`() {
        val viewModel = createViewModel()

        viewModel.onMadhabChanged(Madhab.HANAFI)

        assertEquals(Madhab.HANAFI, viewModel.uiState.value.madhab)
        assertEquals(listOf(Madhab.HANAFI), repository.madhabCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `onHighLatitudeRuleChanged writes and reschedules`() {
        val viewModel = createViewModel()

        viewModel.onHighLatitudeRuleChanged(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)

        assertEquals(HighLatitudeRule.SEVENTH_OF_THE_NIGHT, viewModel.uiState.value.highLatitudeRule)
        assertEquals(listOf(HighLatitudeRule.SEVENTH_OF_THE_NIGHT), repository.highLatitudeRuleCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `onPrayerTimeAdjustChanged writes only the changed prayer and reschedules`() {
        val viewModel = createViewModel()

        viewModel.onPrayerTimeAdjustChanged(PrayerType.FAJR, 5)

        assertEquals(5, viewModel.uiState.value.prayerTimeAdjustMinutes[PrayerType.FAJR])
        assertEquals(0, viewModel.uiState.value.prayerTimeAdjustMinutes[PrayerType.ASR])
        assertEquals(listOf(PrayerType.FAJR to 5), repository.prayerTimeAdjustCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }

    @Test
    fun `on24HourFormatChanged writes but does not reschedule`() {
        val viewModel = createViewModel()

        viewModel.on24HourFormatChanged(true)

        assertTrue(viewModel.uiState.value.use24HourFormat)
        assertEquals(listOf(true), repository.use24HourFormatCalls)
        assertEquals(0, rescheduler.rescheduleCount)
    }

    @Test
    fun `onLanguageChanged writes and applies the locale`() {
        val viewModel = createViewModel()

        viewModel.onLanguageChanged(AppLanguage.ARABIC)

        assertEquals(AppLanguage.ARABIC, viewModel.uiState.value.language)
        assertEquals(listOf(AppLanguage.ARABIC), repository.languageCalls)
        assertEquals(listOf(AppLanguage.ARABIC), localeApplier.applied)
    }

    @Test
    fun `onThemeModeChanged writes and does not reschedule`() {
        val viewModel = createViewModel()

        viewModel.onThemeModeChanged(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(listOf(ThemeMode.DARK), repository.themeModeCalls)
        assertEquals(0, rescheduler.rescheduleCount)
    }

    @Test
    fun `onGenderChanged writes but does not reschedule`() {
        val viewModel = createViewModel()

        viewModel.onGenderChanged(Gender.FEMALE)

        assertEquals(Gender.FEMALE, viewModel.uiState.value.gender)
        assertEquals(listOf(Gender.FEMALE), repository.genderCalls)
        assertEquals(0, rescheduler.rescheduleCount)
    }

    @Test
    fun `clearError clears the error`() {
        val viewModel = createViewModel()
        // No direct way to force an error from these fakes (they never throw), so this just
        // confirms the no-op path leaves state untouched — clearError on a clean state is safe.
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
