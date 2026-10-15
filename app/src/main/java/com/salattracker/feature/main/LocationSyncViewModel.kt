package com.salattracker.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.repository.LocationProvider
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
            if (!settingsRepository.getCurrentPrefs().useDeviceLocation) return@launch
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            settingsRepository.setLocation(lat, lon)
        }
    }
}
