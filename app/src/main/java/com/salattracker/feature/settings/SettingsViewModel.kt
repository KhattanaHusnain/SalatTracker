package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.core.locale.LocaleManager
import com.salattracker.data.location.LocationProvider
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.BackupRepository
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class CalculationMethod(val id: Int, val label: String) {
    MWL(3,          "Muslim World League (MWL)"),
    ISNA(2,         "Islamic Society of North America (ISNA)"),
    UMM_AL_QURA(4,  "Umm al-Qura University, Makkah"),
    EGYPT(5,        "Egyptian General Authority of Survey"),
    KARACHI(1,      "University of Islamic Sciences, Karachi"),
}

data class ReminderSetting(
    val prayerType: PrayerType,
    val enabled: Boolean,
    val offsetMinutes: String,   // raw text as typed; parsed to Int only at save time
    val prayerTime: LocalTime? = null,
)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val useDeviceLocation: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val calculationMethod: CalculationMethod = CalculationMethod.MWL,
    val reminders: List<ReminderSetting> = emptyList(),
    val use24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyTipsEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    // Backup status, for the "Backup & Restore" card's subtitle only — the full
    // flow (connect/back up/restore) lives in feature/backup, not here.
    val backupConnected: Boolean = false,
    val backupAttention: BackupAttention = BackupAttention.NONE,
    val backupLastBackupAt: Long? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val prayerRepository: PrayerRepository,
    private val locationProvider: LocationProvider,
    private val backupRepository: BackupRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeBackupStatus()
    }

    // Separate collector from loadSettings(): backup status lives in its own DataStore
    // file and changes independently of salat_user_prefs, so it gets its own stream
    // rather than being folded into the settings-load collector above.
    private fun observeBackupStatus() {
        viewModelScope.launch {
            backupRepository.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        backupConnected   = status.connected,
                        backupAttention    = status.attention,
                        backupLastBackupAt = status.lastBackupAt,
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { prefs ->
                    val times = prayerRepository.getCachedPrayerTimes(LocalDate.now())
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            useDeviceLocation = prefs.useDeviceLocation,
                            latitude          = prefs.latitude.toString(),
                            longitude         = prefs.longitude.toString(),
                            calculationMethod = CalculationMethod.values()
                                .firstOrNull { m -> m.id == prefs.calculationMethodId }
                                ?: CalculationMethod.MWL,
                            reminders         = PrayerType.values().map { type ->
                                ReminderSetting(
                                    prayerType    = type,
                                    enabled       = prefs.reminderEnabled[type] ?: true,
                                    offsetMinutes = (prefs.reminderOffsetMinutes[type] ?: 0).toString(),
                                    prayerTime    = times?.get(type),
                                )
                            },
                            use24HourFormat   = prefs.use24HourFormat,
                            language          = prefs.language,
                            themeMode         = prefs.themeMode,
                            dailyTipsEnabled  = prefs.dailyTipsEnabled,
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

    fun onReminderEnabledChanged(prayerType: PrayerType, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { r ->
                if (r.prayerType == prayerType) r.copy(enabled = enabled) else r
            })
        }
    }

    fun onReminderOffsetChanged(prayerType: PrayerType, offset: String) {
        // Accept any in-progress input (empty, a lone "-", or a valid int) so the
        // field's text always mirrors exactly what the user typed — parsed at save time.
        if (offset.isNotEmpty() && offset != "-" && offset.toIntOrNull() == null) return
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { r ->
                if (r.prayerType == prayerType) r.copy(offsetMinutes = offset) else r
            })
        }
    }

    fun on24HourFormatChanged(enabled: Boolean) = _uiState.update { it.copy(use24HourFormat = enabled) }
    fun onLanguageChanged(language: AppLanguage) = _uiState.update { it.copy(language = language) }
    fun onThemeModeChanged(themeMode: ThemeMode) = _uiState.update { it.copy(themeMode = themeMode) }
    fun onDailyTipsChanged(enabled: Boolean)     = _uiState.update { it.copy(dailyTipsEnabled = enabled) }

    fun onSave() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                val state = _uiState.value
                settingsRepository.saveSettings(
                    useDeviceLocation     = state.useDeviceLocation,
                    latitude              = state.latitude.toDoubleOrNull() ?: 0.0,
                    longitude             = state.longitude.toDoubleOrNull() ?: 0.0,
                    calculationMethodId   = state.calculationMethod.id,
                    reminderEnabled       = state.reminders.associate { it.prayerType to it.enabled },
                    reminderOffsetMinutes = state.reminders.associate { it.prayerType to (it.offsetMinutes.toIntOrNull() ?: 0) },
                    use24HourFormat       = state.use24HourFormat,
                    language              = state.language,
                    themeMode             = state.themeMode,
                    dailyTipsEnabled      = state.dailyTipsEnabled,
                )
            }.onSuccess {
                // Reminder settings changed — reschedule alarms with the new values right away.
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
}
