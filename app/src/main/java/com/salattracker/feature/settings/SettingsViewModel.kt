package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.core.locale.LocaleManager
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.notification.PrayerReminderWorker
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
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    /** True while the "discard unsaved changes?" dialog is up, after the user tried to
     * leave this screen (back arrow or system back) with unsaved edits. */
    val showDiscardConfirm: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** The persisted values as of the last DataStore emission (initial load, or right after a
     * save round-trips back through observeSettings()). Compared against the live form fields
     * to decide whether leaving the screen now would discard anything. */
    private var lastKnownPrefs: UserPrefs? = null

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { prefs ->
                    lastKnownPrefs = prefs
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

    fun onUseDeviceLocationChanged(enabled: Boolean) = _uiState.update { it.copy(useDeviceLocation = enabled) }
    fun onLatitudeChanged(value: String)              = _uiState.update { it.copy(latitude = value) }
    fun onLongitudeChanged(value: String)             = _uiState.update { it.copy(longitude = value) }

    /** Called once ACCESS_FINE_LOCATION is confirmed granted. Fills the fields; user still hits Save to persist. */
    fun fetchDeviceLocation() {
        viewModelScope.launch {
            val (lat, lon) = locationProvider.getCurrentLocation() ?: return@launch
            _uiState.update { it.copy(latitude = lat.toString(), longitude = lon.toString()) }
        }
    }
    fun onCalculationMethodChanged(method: CalculationMethod) = _uiState.update { it.copy(calculationMethod = method) }

    fun on24HourFormatChanged(enabled: Boolean) = _uiState.update { it.copy(use24HourFormat = enabled) }
    fun onLanguageChanged(language: AppLanguage) = _uiState.update { it.copy(language = language) }
    fun onThemeModeChanged(themeMode: ThemeMode) = _uiState.update { it.copy(themeMode = themeMode) }

    fun onSave() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val state = _uiState.value
                // Reminder enable/offset now live on their own screen (feature/reminders) with
                // their own granular writes — pass the last-known values through unchanged
                // rather than lose them, since this form no longer tracks them.
                val baseline = lastKnownPrefs
                settingsRepository.saveSettings(
                    useDeviceLocation     = state.useDeviceLocation,
                    latitude              = state.latitude.toDoubleOrNull() ?: 0.0,
                    longitude             = state.longitude.toDoubleOrNull() ?: 0.0,
                    calculationMethodId   = state.calculationMethod.id,
                    reminderEnabled       = baseline?.reminderEnabled ?: UserPrefs().reminderEnabled,
                    reminderOffsetMinutes = baseline?.reminderOffsetMinutes ?: UserPrefs().reminderOffsetMinutes,
                    use24HourFormat       = state.use24HourFormat,
                    language              = state.language,
                    themeMode             = state.themeMode,
                )
            }.onSuccess {
                // Location/calculation-method may have changed prayer times — reschedule alarms.
                PrayerReminderWorker.enqueueOneTime(workManager)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                // Apply language app-wide (no-op if unchanged; recreates the UI if it changed).
                LocaleManager.apply(_uiState.value.language)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError()        = _uiState.update { it.copy(error = null) }
    fun clearSaveSuccess()  = _uiState.update { it.copy(saveSuccess = false) }

    // ── Unsaved-changes guard ─────────────────────────────────────────────────
    // Settings used to be a bottom-nav tab, so its ViewModel just survived a tab switch and
    // unsaved edits sat there until the user came back. Now it is a screen you navigate away
    // from, so leaving without saving needs to be an explicit choice instead of a silent loss.

    /** Compares the live form against [lastKnownPrefs]. False before the first load, so the
     * back arrow/BackHandler never blocks leaving before there is anything to compare against. */
    fun hasUnsavedChanges(): Boolean {
        val baseline = lastKnownPrefs ?: return false
        val state = _uiState.value
        val sameLocation = state.useDeviceLocation == baseline.useDeviceLocation &&
            (state.latitude.toDoubleOrNull() ?: 0.0) == baseline.latitude &&
            (state.longitude.toDoubleOrNull() ?: 0.0) == baseline.longitude
        val sameMethod = state.calculationMethod.id == baseline.calculationMethodId
        val sameDisplay = state.use24HourFormat == baseline.use24HourFormat
        val sameAppearance = state.language == baseline.language &&
            state.themeMode == baseline.themeMode
        return !(sameLocation && sameMethod && sameDisplay && sameAppearance)
    }

    /** Called by the screen's BackHandler when [hasUnsavedChanges] is true — shows the
     * confirmation instead of letting the back press fall through to navigation. */
    fun onBackRequested() = _uiState.update { it.copy(showDiscardConfirm = true) }

    fun onKeepEditingClick() = _uiState.update { it.copy(showDiscardConfirm = false) }

    /** Only hides the dialog; the Entry performs the actual navigation afterward. */
    fun onDiscardConfirm() = _uiState.update { it.copy(showDiscardConfirm = false) }
}
