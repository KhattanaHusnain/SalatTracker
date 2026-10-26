package com.salattracker.feature.main

import com.salattracker.domain.model.CityResult
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.testutil.FakeGeocodingRepository
import com.salattracker.testutil.FakeLocationProvider
import com.salattracker.testutil.FakePrayerReminderRescheduler
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationSyncViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `moved device location refreshes the city label used by Home`() {
        val settings = FakeSettingsRepository(UserPrefs(
            useDeviceLocation = true,
            latitude = 24.80,
            longitude = 66.90,
            locationLabel = "Old City, Pakistan",
        ))
        val location = FakeLocationProvider().apply { nextLocation = 24.86 to 67.01 }
        val geocoding = FakeGeocodingRepository().apply {
            nextReverseResult = CityResult("Karachi", "Sindh", "Pakistan", 24.86, 67.01, "PK")
        }
        val rescheduler = FakePrayerReminderRescheduler()
        val viewModel = LocationSyncViewModel(settings, location, geocoding, rescheduler)

        viewModel.syncDeviceLocation()

        assertEquals(listOf(24.86 to 67.01), settings.locationCalls)
        assertEquals(listOf("Karachi, Sindh, Pakistan"), settings.locationLabelCalls)
        assertEquals(listOf(24.86 to 67.01), geocoding.reverseGeocodeCalls)
        assertEquals(1, rescheduler.rescheduleCount)
    }
}
