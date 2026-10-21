package com.salattracker.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.notification.PrayerReminderChannels
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class ReminderSetting(
    val prayerType: PrayerType,
    val enabled: Boolean,
    val offsetMinutes: String,   // raw text as typed; parsed to Int only when written
    val prayerTime: LocalTime? = null,
    val snoozeMinutes: Int = 10,
    val fullScreenAlert: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /** Null = app default (Azan). See SettingsRepository.setReminderSoundUri for the full encoding. */
    val soundUri: String? = null,
)

data class PrayerRemindersUiState(
    val isLoading: Boolean = true,
    val reminders: List<ReminderSetting> = emptyList(),
    val expandedPrayer: PrayerType? = null,
    val jumuahEnabled: Boolean = true,
    /** 0 = no separate early reminder for Jumu'ah. */
    val jumuahReminderLeadMinutes: Int = 0,
    val madhab: Madhab = Madhab.STANDARD,
)

@HiltViewModel
class PrayerRemindersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val prayerRepository: PrayerRepository,
    private val channels: PrayerReminderChannels,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerRemindersUiState())
    val uiState: StateFlow<PrayerRemindersUiState> = _uiState.asStateFlow()

    init { loadReminders() }

    private fun loadReminders() {
        viewModelScope.launch {
            val prefs = settingsRepository.getCurrentPrefs()
            val times = prayerRepository.getPrayerTimes(LocalDate.now())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    jumuahEnabled = prefs.jumuahEnabled,
                    jumuahReminderLeadMinutes = prefs.jumuahReminderLeadMinutes,
                    madhab = prefs.madhab,
                    reminders = PrayerType.values().map { type ->
                        ReminderSetting(
                            prayerType       = type,
                            enabled          = prefs.reminderEnabled[type] ?: true,
                            offsetMinutes    = (prefs.reminderOffsetMinutes[type] ?: 0).toString(),
                            prayerTime       = times?.get(type),
                            snoozeMinutes    = prefs.reminderSnoozeMinutes[type] ?: 10,
                            fullScreenAlert  = prefs.reminderFullScreenAlert[type] ?: true,
                            vibrationEnabled = prefs.reminderVibrationEnabled[type] ?: true,
                            soundUri         = prefs.reminderSoundUri[type],
                        )
                    },
                )
            }
        }
    }

    fun onExpandToggle(type: PrayerType) {
        _uiState.update { it.copy(expandedPrayer = if (it.expandedPrayer == type) null else type) }
    }

    fun onEnabledChanged(type: PrayerType, enabled: Boolean) {
        updateSetting(type) { it.copy(enabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(type, enabled)
            PrayerReminderWorker.enqueueOneTime(workManager)
        }
    }

    fun onOffsetChanged(type: PrayerType, offset: String) {
        if (offset.isNotEmpty() && offset != "-" && offset.toIntOrNull() == null) return
        updateSetting(type) { it.copy(offsetMinutes = offset) }
        val minutes = offset.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setReminderOffsetMinutes(type, minutes)
            PrayerReminderWorker.enqueueOneTime(workManager)
        }
    }

    fun onSnoozeChanged(type: PrayerType, minutes: Int) {
        updateSetting(type) { it.copy(snoozeMinutes = minutes) }
        viewModelScope.launch { settingsRepository.setReminderSnoozeMinutes(type, minutes) }
    }

    fun onFullScreenAlertChanged(type: PrayerType, enabled: Boolean) {
        updateSetting(type) { it.copy(fullScreenAlert = enabled) }
        viewModelScope.launch { settingsRepository.setReminderFullScreenAlert(type, enabled) }
    }

    fun onVibrationChanged(type: PrayerType, enabled: Boolean) {
        updateSetting(type) { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setReminderVibrationEnabled(type, enabled)
            channels.recreate(type, currentSoundUri(type), enabled)
        }
    }

    fun onSoundSelected(type: PrayerType, uri: String?) {
        updateSetting(type) { it.copy(soundUri = uri) }
        viewModelScope.launch {
            settingsRepository.setReminderSoundUri(type, uri)
            channels.recreate(type, uri, currentVibration(type))
        }
    }

    fun onJumuahEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(jumuahEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setJumuahEnabled(enabled)
            PrayerReminderWorker.enqueueOneTime(workManager)
        }
    }

    fun onJumuahReminderLeadChanged(minutes: Int) {
        _uiState.update { it.copy(jumuahReminderLeadMinutes = minutes) }
        viewModelScope.launch {
            settingsRepository.setJumuahReminderLeadMinutes(minutes)
            PrayerReminderWorker.enqueueOneTime(workManager)
        }
    }

    private fun currentSoundUri(type: PrayerType) =
        _uiState.value.reminders.firstOrNull { it.prayerType == type }?.soundUri

    private fun currentVibration(type: PrayerType) =
        _uiState.value.reminders.firstOrNull { it.prayerType == type }?.vibrationEnabled ?: true

    private fun updateSetting(type: PrayerType, transform: (ReminderSetting) -> ReminderSetting) {
        _uiState.update { state ->
            state.copy(reminders = state.reminders.map { if (it.prayerType == type) transform(it) else it })
        }
    }
}
