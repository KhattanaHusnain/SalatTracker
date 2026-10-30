package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.repository.GeocodingRepository
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.salattracker.core.util.DebouncedCitySearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditLocationMode { COORDINATES, CITY_SEARCH }

data class EditCoordinatesUiState(
    val mode: EditLocationMode = EditLocationMode.COORDINATES,
    val latitude: String = "",
    val longitude: String = "",
    val latitudeInvalid: Boolean = false,
    val longitudeInvalid: Boolean = false,
    val cityQuery: String = "",
    val cityResults: List<CityResult> = emptyList(),
    val isSearchingCity: Boolean = false,
    val isLocating: Boolean = false,
    /** One-shot: the Screen watches this and calls back to pop the nav stack once true — see
     * EditCoordinatesEntry. */
    val isDone: Boolean = false,
    /** A string resource id, resolved by the Screen — there's nothing dynamic to interpolate for
     * the one error case this screen has (no GPS fix available). */
    val errorRes: Int? = null,
)

/**
 * Mirrors onboarding's own Location Confirm / City Search steps (same [GeocodingRepository]
 * search-and-select shape) but as a standalone pushed screen reached from the Settings screen's
 * "Location" row — see Routes.EDIT_COORDINATES. Every successful path (manual save, "Use my
 * location", picking a search result) writes straight to [SettingsRepository] and reschedules,
 * matching the rest of Settings' live-apply convention.
 */
@HiltViewModel
class EditCoordinatesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val geocodingRepository: GeocodingRepository,
    private val locationProvider: LocationProvider,
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditCoordinatesUiState())
    val uiState: StateFlow<EditCoordinatesUiState> = _uiState.asStateFlow()

    private val citySearch = DebouncedCitySearch(
        scope = viewModelScope,
        repository = geocodingRepository,
        onSearching = { searching -> _uiState.update { it.copy(isSearchingCity = searching) } },
        onResults = { results -> _uiState.update { it.copy(cityResults = results) } },
    )

    init {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPrefs()
            _uiState.update { it.copy(latitude = prefs.latitude.toString(), longitude = prefs.longitude.toString()) }
        }
    }

    fun onLatitudeChanged(value: String) = _uiState.update { it.copy(latitude = value, latitudeInvalid = value.toDoubleOrNull()?.let { latitude -> latitude !in -90.0..90.0 } ?: value.isNotBlank()) }
    fun onLongitudeChanged(value: String) = _uiState.update { it.copy(longitude = value, longitudeInvalid = value.toDoubleOrNull()?.let { longitude -> longitude !in -180.0..180.0 } ?: value.isNotBlank()) }

    fun onSaveCoordinates() {
        val state = _uiState.value
        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()
        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _uiState.update { it.copy(latitudeInvalid = lat == null || lat !in -90.0..90.0, longitudeInvalid = lon == null || lon !in -180.0..180.0) }
            return
        }
        viewModelScope.launch {
            val city = runCatching { geocodingRepository.reverseGeocode(lat, lon) }.getOrNull()
            settingsRepository.setUseDeviceLocation(false)
            settingsRepository.setLocation(lat, lon)
            city?.takeIf { it.name.isNotBlank() }?.let {
                settingsRepository.setLocationLabel(it.fullLabel)
            }
            prayerReminderRescheduler.rescheduleAll()
            _uiState.update { it.copy(isDone = true) }
        }
    }

    fun onUseMyLocationClick() {
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val fix = locationProvider.getCurrentLocation()
            if (fix == null) {
                _uiState.update { it.copy(isLocating = false, errorRes = com.salattracker.R.string.settings_edit_location_no_fix) }
                return@launch
            }
            val (lat, lon) = fix
            settingsRepository.setUseDeviceLocation(true)
            settingsRepository.setLocation(lat, lon)
            val city = runCatching { geocodingRepository.reverseGeocode(lat, lon) }.getOrNull()
            city?.takeIf { it.name.isNotBlank() }?.let {
                settingsRepository.setLocationLabel(it.fullLabel)
            }
            prayerReminderRescheduler.rescheduleAll()
            _uiState.update { it.copy(isLocating = false, isDone = true) }
        }
    }

    fun onSearchCityClick() = _uiState.update { it.copy(mode = EditLocationMode.CITY_SEARCH) }
    fun onBackFromCitySearch() = _uiState.update { it.copy(mode = EditLocationMode.COORDINATES) }

    fun onCityQueryChanged(query: String) {
        _uiState.update { it.copy(cityQuery = query) }
        citySearch.search(query)
    }

    fun onCitySelected(city: CityResult) {
        viewModelScope.launch {
            settingsRepository.setUseDeviceLocation(false)
            settingsRepository.setLocation(city.latitude, city.longitude)
            settingsRepository.setLocationLabel(city.fullLabel)
            prayerReminderRescheduler.rescheduleAll()
            _uiState.update { it.copy(isDone = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

}
