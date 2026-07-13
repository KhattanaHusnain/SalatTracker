package com.salattracker.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.data.location.LocationProvider
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fetches device location on app launch and persists it, so prayer times
 * are correct without the user ever visiting Settings.
 */
@HiltViewModel
class LocationSyncViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    suspend fun isDeviceLocationEnabled(): Boolean =
        settingsRepository.getCurrentPrefs().useDeviceLocation

    fun syncDeviceLocation() {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPrefs()
            if (!prefs.useDeviceLocation) return@launch
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            settingsRepository.saveSettings(
                useDeviceLocation     = prefs.useDeviceLocation,
                latitude              = lat,
                longitude             = lon,
                calculationMethodId   = prefs.calculationMethodId,
                reminderEnabled       = prefs.reminderEnabled,
                reminderOffsetMinutes = prefs.reminderOffsetMinutes,
                use24HourFormat       = prefs.use24HourFormat,
                language              = prefs.language,
                themeMode             = prefs.themeMode,
                dailyTipsEnabled      = prefs.dailyTipsEnabled,
            )
        }
    }
}
