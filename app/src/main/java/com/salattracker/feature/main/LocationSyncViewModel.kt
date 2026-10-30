package com.salattracker.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.repository.GeocodingRepository
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Fetches device location on app launch and persists it, so prayer times
 * are correct without the user ever visiting Settings.
 */
@HiltViewModel
class LocationSyncViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val geocodingRepository: GeocodingRepository,
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
) : ViewModel() {

    suspend fun isDeviceLocationEnabled(): Boolean =
        settingsRepository.getCurrentPrefs().useDeviceLocation

    fun syncDeviceLocation() {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPrefs()
            if (!prefs.useDeviceLocation) return@launch
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            val locationChanged =
                lat.roundedForComparison() != prefs.latitude.roundedForComparison() ||
                    lon.roundedForComparison() != prefs.longitude.roundedForComparison()
            // Prayer times now depend on location (on-device calculation). Reschedule only when
            // the location actually moved by a meaningful amount — comparing at ~1km precision
            // (2 decimal places) so ordinary GPS jitter on every launch doesn't churn the alarms.
            // A missing label is also retried so installs affected by an earlier failed lookup can
            // recover without waiting for the device to move.
            if (locationChanged || prefs.locationLabel.isNullOrBlank()) {
                val city = runCatching { geocodingRepository.reverseGeocode(lat, lon) }.getOrNull()
                if (locationChanged) {
                    settingsRepository.setLocation(lat, lon)
                    prayerReminderRescheduler.rescheduleAll()
                }
                city?.takeIf { it.name.isNotBlank() }?.let {
                    settingsRepository.setLocationLabel(it.fullLabel)
                }
            }
        }
    }

    private fun Double.roundedForComparison(): Double = (this * 100).roundToInt() / 100.0
}
