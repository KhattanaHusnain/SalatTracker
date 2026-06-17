package com.salattracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.salattracker.data.sync.SyncOrchestrator
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val offsetMinutes: Int,
)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val useDeviceLocation: Boolean = true,
    val latitude: String = "",
    val longitude: String = "",
    val calculationMethod: CalculationMethod = CalculationMethod.MWL,
    val reminders: List<ReminderSetting> = emptyList(),
    val use24HourFormat: Boolean = false,
    val isSaving: Boolean = false,
    val signOutConfirmVisible: Boolean = false,
    val navigateToLogin: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val auth: FirebaseAuth,
    // ADDED: cancel background sync on sign-out
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val user = auth.currentUser
            settingsRepository.observeSettings()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { prefs ->
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            displayName       = user?.displayName ?: "",
                            email             = user?.email ?: "",
                            photoUrl          = user?.photoUrl?.toString(),
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
                                    offsetMinutes = prefs.reminderOffsetMinutes[type] ?: 0,
                                )
                            },
                            use24HourFormat   = prefs.use24HourFormat,
                        )
                    }
                }
        }
    }

    fun onUseDeviceLocationChanged(enabled: Boolean) = _uiState.update { it.copy(useDeviceLocation = enabled) }
    fun onLatitudeChanged(value: String)              = _uiState.update { it.copy(latitude = value) }
    fun onLongitudeChanged(value: String)             = _uiState.update { it.copy(longitude = value) }
    fun onCalculationMethodChanged(method: CalculationMethod) = _uiState.update { it.copy(calculationMethod = method) }

    fun onReminderEnabledChanged(prayerType: PrayerType, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { r ->
                if (r.prayerType == prayerType) r.copy(enabled = enabled) else r
            })
        }
    }

    fun onReminderOffsetChanged(prayerType: PrayerType, offset: String) {
        val offsetInt = offset.toIntOrNull() ?: return
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { r ->
                if (r.prayerType == prayerType) r.copy(offsetMinutes = offsetInt) else r
            })
        }
    }

    fun on24HourFormatChanged(enabled: Boolean) = _uiState.update { it.copy(use24HourFormat = enabled) }

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
                    reminderOffsetMinutes = state.reminders.associate { it.prayerType to it.offsetMinutes },
                    use24HourFormat       = state.use24HourFormat,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun onSignOutRequest() = _uiState.update { it.copy(signOutConfirmVisible = true) }
    fun onSignOutCancel()  = _uiState.update { it.copy(signOutConfirmVisible = false) }

    fun onSignOutConfirm() {
        viewModelScope.launch {
            // ADDED: cancel periodic + one-time sync before signing out
            syncOrchestrator.cancelAll()
            auth.signOut()
            _uiState.update { it.copy(signOutConfirmVisible = false, navigateToLogin = true) }
        }
    }

    fun clearError()        = _uiState.update { it.copy(error = null) }
    fun clearSaveSuccess()  = _uiState.update { it.copy(saveSuccess = false) }
    fun navigationHandled() = _uiState.update { it.copy(navigateToLogin = false) }
}