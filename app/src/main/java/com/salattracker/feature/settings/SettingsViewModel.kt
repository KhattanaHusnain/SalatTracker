package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.GeocodingRepository
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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** A human-readable name for [latitude]/[longitude] (e.g. "Karachi, Sindh, Pakistan"), when
     * known — see [com.salattracker.domain.repository.SettingsRepository.setLocationLabel]. Null
     * falls back to showing the raw coordinates. */
    val locationLabel: String? = null,
    val calculationMethod: CalculationMethod = CalculationMethod.DEFAULT,
    val hijriAdjustmentDays: Int = 0,
    val madhab: Madhab = Madhab.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTOMATIC,
    val prayerTimeAdjustMinutes: Map<PrayerType, Int> = PrayerType.values().associateWith { 0 },
    val use24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Onboarding's "About you" answer — null for an install that skipped/predates it; the About
     * You card shows no selection in that case rather than defaulting to one. */
    val gender: Gender? = null,
    val showCalculationMethodSheet: Boolean = false,
    val errorRes: Int? = null,
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
    private val geocodingRepository: GeocodingRepository,
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
                .catch   { _ -> _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } }
                .collect { prefs ->
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            useDeviceLocation = prefs.useDeviceLocation,
                            latitude          = prefs.latitude,
                            longitude         = prefs.longitude,
                            locationLabel     = prefs.locationLabel,
                            calculationMethod = CalculationMethod.fromId(prefs.calculationMethodId),
                            hijriAdjustmentDays = prefs.hijriAdjustmentDays,
                            madhab            = prefs.madhab,
                            highLatitudeRule  = prefs.highLatitudeRule,
                            prayerTimeAdjustMinutes = prefs.prayerTimeAdjustMinutes,
                            use24HourFormat   = prefs.use24HourFormat,
                            language          = prefs.language,
                            themeMode         = prefs.themeMode,
                            gender            = prefs.gender,
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

    fun onLocationPermissionDenied() = _uiState.update { it.copy(useDeviceLocation = false, errorRes = R.string.settings_edit_location_no_fix) }

    /** Called once ACCESS_FINE_LOCATION is confirmed granted (the device-location toggle just
     * turned on) — persists the fix; [loadSettings]'s reactive collector then updates the
     * displayed lat/lon on its own once the write lands. Manual coordinate entry and city search
     * now live in their own screen (feature/settings's EditCoordinatesViewModel), pushed by
     * MainActivity when the Settings screen's "Location" row is tapped. */
    fun fetchDeviceLocation() {
        viewModelScope.launch {
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            val city = runCatching { geocodingRepository.reverseGeocode(lat, lon) }.getOrNull()
            runCatching {
                settingsRepository.setLocation(lat, lon)
                settingsRepository.setLocationLabel(city?.fullLabel)
            }.onSuccess {
                prayerReminderRescheduler.rescheduleAll()
            }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    // ── Calculation method ────────────────────────────────────────────────────

    fun onCalculationMethodSheetOpen()    = _uiState.update { it.copy(showCalculationMethodSheet = true) }
    fun onCalculationMethodSheetDismiss() = _uiState.update { it.copy(showCalculationMethodSheet = false) }

    fun onCalculationMethodChanged(method: CalculationMethod) {
        _uiState.update { it.copy(calculationMethod = method, showCalculationMethodSheet = false) }
        writeAndReschedule { settingsRepository.setCalculationMethodId(method.id) }
    }

    fun onHijriAdjustmentChanged(days: Int) {
        val clamped = days.coerceIn(-2, 2)
        _uiState.update { it.copy(hijriAdjustmentDays = clamped) }
        write { settingsRepository.setHijriAdjustmentDays(clamped) }
    }

    fun onMadhabChanged(madhab: Madhab) {
        _uiState.update { it.copy(madhab = madhab) }
        writeAndReschedule { settingsRepository.setMadhab(madhab) }
    }

    fun onHighLatitudeRuleChanged(rule: HighLatitudeRule) {
        _uiState.update { it.copy(highLatitudeRule = rule) }
        writeAndReschedule { settingsRepository.setHighLatitudeRule(rule) }
    }

    fun onPrayerTimeAdjustChanged(type: PrayerType, minutes: Int) {
        _uiState.update { it.copy(prayerTimeAdjustMinutes = it.prayerTimeAdjustMinutes + (type to minutes)) }
        writeAndReschedule { settingsRepository.setPrayerTimeAdjustMinutes(type, minutes) }
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
                .onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    fun onThemeModeChanged(themeMode: ThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        write { settingsRepository.setThemeMode(themeMode) }
    }

    fun onGenderChanged(gender: Gender) {
        _uiState.update { it.copy(gender = gender) }
        write { settingsRepository.setGender(gender) }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    /** Same as [write], but also reschedules prayer reminders on success — for the fields
     * (location, calculation method) that can shift prayer times. */
    private fun writeAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { prayerReminderRescheduler.rescheduleAll() }
                .onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }
}
