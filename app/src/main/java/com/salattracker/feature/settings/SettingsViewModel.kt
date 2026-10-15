package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.LocaleApplier
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val useDeviceLocation: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val calculationMethod: CalculationMethod = CalculationMethod.DEFAULT,
    val use24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showCalculationMethodSheet: Boolean = false,
    val error: String? = null,
)

/**
 * Every control here writes immediately (live-apply), matching feature/reminders'
 * PrayerRemindersViewModel — there is no Save button or discard-confirmation flow. Depends on
 * [LocationProvider]/[PrayerReminderRescheduler]/[LocaleApplier] rather than a raw
 * `FusedLocationProviderClient`/`WorkManager`/the `LocaleManager` object directly, so it can be
 * unit-tested with fakes.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
    private val localeApplier: LocaleApplier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { prefs ->
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            useDeviceLocation = prefs.useDeviceLocation,
                            latitude          = prefs.latitude.toString(),
                            longitude         = prefs.longitude.toString(),
                            calculationMethod = CalculationMethod.fromId(prefs.calculationMethodId),
                            use24HourFormat   = prefs.use24HourFormat,
                            language          = prefs.language,
                            themeMode         = prefs.themeMode,
                        )
                    }
                }
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────

    fun onUseDeviceLocationChanged(enabled: Boolean) {
        _uiState.update { it.copy(useDeviceLocation = enabled) }
        writeAndReschedule { settingsRepository.setUseDeviceLocation(enabled) }
    }

    /** Called once ACCESS_FINE_LOCATION is confirmed granted (the device-location toggle just
     * turned on) — fills the fields and persists the fix in one step. */
    fun fetchDeviceLocation() {
        viewModelScope.launch {
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            _uiState.update { it.copy(latitude = lat.toString(), longitude = lon.toString()) }
            writeAndReschedule { settingsRepository.setLocation(lat, lon) }
        }
    }

    fun onLatitudeChanged(value: String) = _uiState.update { it.copy(latitude = value) }
    fun onLongitudeChanged(value: String) = _uiState.update { it.copy(longitude = value) }

    /** Commits the manual lat/lon fields — wired to the field's IME "Done" and focus-loss.
     * Silently a no-op if either hasn't parsed to a valid number yet, rather than writing
     * garbage mid-type. */
    fun onLocationCommit() {
        val state = _uiState.value
        val lat = state.latitude.toDoubleOrNull() ?: return
        val lon = state.longitude.toDoubleOrNull() ?: return
        writeAndReschedule { settingsRepository.setLocation(lat, lon) }
    }

    // ── Calculation method ────────────────────────────────────────────────────

    fun onCalculationMethodSheetOpen()    = _uiState.update { it.copy(showCalculationMethodSheet = true) }
    fun onCalculationMethodSheetDismiss() = _uiState.update { it.copy(showCalculationMethodSheet = false) }

    fun onCalculationMethodChanged(method: CalculationMethod) {
        _uiState.update { it.copy(calculationMethod = method, showCalculationMethodSheet = false) }
        writeAndReschedule { settingsRepository.setCalculationMethodId(method.id) }
    }

    // ── Display / appearance ──────────────────────────────────────────────────

    fun on24HourFormatChanged(enabled: Boolean) {
        _uiState.update { it.copy(use24HourFormat = enabled) }
        write { settingsRepository.setUse24HourFormat(enabled) }
    }

    fun onLanguageChanged(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            runCatching { settingsRepository.setLanguage(language) }
                .onSuccess { localeApplier.apply(language) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun onThemeModeChanged(themeMode: ThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        write { settingsRepository.setThemeMode(themeMode) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    /** Same as [write], but also reschedules prayer reminders on success — for the fields
     * (location, calculation method) that can shift prayer times. */
    private fun writeAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { prayerReminderRescheduler.rescheduleAll() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
