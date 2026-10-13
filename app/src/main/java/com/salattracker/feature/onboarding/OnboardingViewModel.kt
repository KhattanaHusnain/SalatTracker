package com.salattracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.core.locale.LocaleManager
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The four first-run onboarding steps, in order. */
enum class OnboardingStep { LANGUAGE, THEME, LOCATION, NOTIFICATIONS }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val prayerRemindersEnabled: Boolean = true,
    val dailyTipsEnabled: Boolean = false,
    val isFinishing: Boolean = false,
    /** True while the "Restore from Google Drive" flow (feature/backup's BackupEntry) is shown
     * in place of the normal onboarding steps. A successful restore sets onboarding_completed
     * itself (see BackupLocalSource.applySnapshot), so MainActivity's reactive gate swaps to the
     * main app on its own — no extra signal needed here. */
    val showRestore: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onLanguageSelected(language: AppLanguage) = _uiState.update { it.copy(language = language) }
    fun onThemeSelected(themeMode: ThemeMode) = _uiState.update { it.copy(themeMode = themeMode) }
    fun onPrayerRemindersChanged(enabled: Boolean) = _uiState.update { it.copy(prayerRemindersEnabled = enabled) }
    fun onDailyTipsChanged(enabled: Boolean) = _uiState.update { it.copy(dailyTipsEnabled = enabled) }

    fun onNext() {
        val next = OnboardingStep.values().getOrNull(_uiState.value.step.ordinal + 1) ?: return
        _uiState.update { it.copy(step = next) }
    }

    fun onBack() {
        val prev = OnboardingStep.values().getOrNull(_uiState.value.step.ordinal - 1) ?: return
        _uiState.update { it.copy(step = prev) }
    }

    fun onRestoreClick() = _uiState.update { it.copy(showRestore = true) }
    fun onRestoreDismiss() = _uiState.update { it.copy(showRestore = false) }

    /**
     * Persists every onboarding choice and marks onboarding complete. Setting
     * onboardingCompleted last flips the reactive app-start gate over to the main app.
     */
    fun onFinish() {
        val state = _uiState.value
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            settingsRepository.setLanguage(state.language)
            settingsRepository.setThemeMode(state.themeMode)
            settingsRepository.setDailyTipsEnabled(state.dailyTipsEnabled)
            settingsRepository.setAllRemindersEnabled(state.prayerRemindersEnabled)
            // Reschedule prayer reminders to honour the master toggle chosen here.
            PrayerReminderWorker.enqueueOneTime(workManager)
            settingsRepository.setOnboardingCompleted(true)
            // Apply the chosen language app-wide (recreates the UI into that locale).
            LocaleManager.apply(state.language)
        }
    }
}
