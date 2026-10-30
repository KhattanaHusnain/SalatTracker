package com.salattracker.feature.settings

import com.salattracker.R
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.testutil.FakeGeocodingRepository
import com.salattracker.testutil.FakeLocationProvider
import com.salattracker.testutil.FakePrayerReminderRescheduler
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditCoordinatesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository(UserPrefs(
        latitude = 24.4672,
        longitude = 39.6111,
        locationLabel = "Madinah, Saudi Arabia",
    ))
    private val geocodingRepository = FakeGeocodingRepository()
    private val locationProvider = FakeLocationProvider()
    private val rescheduler = FakePrayerReminderRescheduler()

    private fun createViewModel() = EditCoordinatesViewModel(repository, geocodingRepository, locationProvider, rescheduler)

    @Test
    fun `loads the current coordinates on start`() {
        val viewModel = createViewModel()

        assertEquals("24.4672", viewModel.uiState.value.latitude)
        assertEquals("39.6111", viewModel.uiState.value.longitude)
    }

    @Test
    fun `onSaveCoordinates replaces the city only after a successful lookup`() {
        geocodingRepository.nextReverseResult = CityResult("New City", "Region", "Country", 12.3, 45.6)
        val viewModel = createViewModel()
        viewModel.onLatitudeChanged("12.3")
        viewModel.onLongitudeChanged("45.6")

        viewModel.onSaveCoordinates()

        assertEquals(listOf(12.3 to 45.6), repository.locationCalls)
        assertEquals(listOf("New City, Region, Country"), repository.locationLabelCalls)
        assertEquals(listOf(false), repository.useDeviceLocationCalls)
        assertEquals(1, rescheduler.rescheduleCount)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onSaveCoordinates keeps the previous city when lookup fails`() {
        geocodingRepository.nextReverseResult = null
        val viewModel = createViewModel()
        viewModel.onLatitudeChanged("12.3")
        viewModel.onLongitudeChanged("45.6")

        viewModel.onSaveCoordinates()

        assertEquals(listOf(12.3 to 45.6), repository.locationCalls)
        assertTrue(repository.locationLabelCalls.isEmpty())
        assertEquals(1, rescheduler.rescheduleCount)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onSaveCoordinates does nothing while either field fails to parse`() {
        val viewModel = createViewModel()
        viewModel.onLatitudeChanged("12.3")
        viewModel.onLongitudeChanged("")

        viewModel.onSaveCoordinates()

        assertTrue(repository.locationCalls.isEmpty())
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onUseMyLocationClick persists the fix with its reverse-geocoded label and is done`() {
        locationProvider.nextLocation = 1.5 to 2.5
        geocodingRepository.nextReverseResult = CityResult("Karachi", "Sindh", "Pakistan", 1.5, 2.5)
        val viewModel = createViewModel()

        viewModel.onUseMyLocationClick()

        assertEquals(listOf(1.5 to 2.5), repository.locationCalls)
        assertEquals(listOf(true), repository.useDeviceLocationCalls)
        assertEquals(listOf("Karachi, Sindh, Pakistan"), repository.locationLabelCalls)
        assertEquals(1, rescheduler.rescheduleCount)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onUseMyLocationClick keeps the previous city when reverse geocoding fails`() {
        locationProvider.nextLocation = 1.5 to 2.5
        geocodingRepository.nextReverseResult = null
        val viewModel = createViewModel()

        viewModel.onUseMyLocationClick()

        assertEquals(listOf(1.5 to 2.5), repository.locationCalls)
        assertTrue(repository.locationLabelCalls.isEmpty())
        assertEquals(1, rescheduler.rescheduleCount)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onUseMyLocationClick surfaces an error and does not reschedule when no fix is available`() {
        locationProvider.nextLocation = null
        val viewModel = createViewModel()

        viewModel.onUseMyLocationClick()

        assertTrue(repository.locationCalls.isEmpty())
        assertEquals(0, rescheduler.rescheduleCount)
        assertFalse(viewModel.uiState.value.isDone)
        assertEquals(R.string.settings_edit_location_no_fix, viewModel.uiState.value.errorRes)
    }

    @Test
    fun `onCityQueryChanged debounces then searches, and onCitySelected persists the city`() {
        geocodingRepository.nextSearchResults = listOf(CityResult("Karachi", "Sindh", "Pakistan", 24.86, 67.0))
        val viewModel = createViewModel()

        viewModel.onCityQueryChanged("Kar")
        assertTrue(viewModel.uiState.value.isSearchingCity)
        assertTrue(geocodingRepository.searchQueries.isEmpty())

        mainDispatcherRule.scheduler.advanceTimeBy(400)
        mainDispatcherRule.scheduler.runCurrent()

        assertEquals(listOf("Kar"), geocodingRepository.searchQueries)
        assertEquals(1, viewModel.uiState.value.cityResults.size)
        assertFalse(viewModel.uiState.value.isSearchingCity)

        viewModel.onCitySelected(viewModel.uiState.value.cityResults.first())

        assertEquals(listOf(24.86 to 67.0), repository.locationCalls)
        assertEquals(listOf("Karachi, Sindh, Pakistan"), repository.locationLabelCalls)
        assertEquals(listOf(false), repository.useDeviceLocationCalls)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `onCityQueryChanged with a blank query clears results without searching`() {
        val viewModel = createViewModel()
        viewModel.onCityQueryChanged("")

        assertTrue(viewModel.uiState.value.cityResults.isEmpty())
        assertFalse(viewModel.uiState.value.isSearchingCity)
        assertTrue(geocodingRepository.searchQueries.isEmpty())
    }

    @Test
    fun `onSearchCityClick and onBackFromCitySearch toggle the mode`() {
        val viewModel = createViewModel()

        viewModel.onSearchCityClick()
        assertEquals(EditLocationMode.CITY_SEARCH, viewModel.uiState.value.mode)

        viewModel.onBackFromCitySearch()
        assertEquals(EditLocationMode.COORDINATES, viewModel.uiState.value.mode)
    }

    @Test
    fun `clearError clears the error`() {
        locationProvider.nextLocation = null
        val viewModel = createViewModel()
        viewModel.onUseMyLocationClick()
        assertEquals(R.string.settings_edit_location_no_fix, viewModel.uiState.value.errorRes)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorRes)
    }
}
