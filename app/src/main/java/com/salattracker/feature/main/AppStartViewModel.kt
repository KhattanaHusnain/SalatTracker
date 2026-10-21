package com.salattracker.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** null = still reading DataStore; true/false = whether onboarding has been completed.
 * [themeMode] is read eagerly too — MainActivity needs it before the first frame to avoid a
 * light-then-dark flash on a device that opens the app with the system/saved mode set to dark.
 * [needsMadhabPrompt] is true only for an install that finished onboarding before [Madhab] was
 * introduced and hasn't been through the one-time prompt (MainActivity's MadhabPromptDialog)
 * yet — a fresh install always picks its school during onboarding instead. */
data class AppStartState(
    val onboardingCompleted: Boolean? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val needsMadhabPrompt: Boolean = false,
)

/**
 * Decides whether the app opens straight into the main UI or into the first-run
 * onboarding flow. Reactive: when onboarding writes onboardingCompleted = true, this
 * flips and MainActivity swaps the onboarding flow out for the main scaffold.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
) : ViewModel() {

    val state: StateFlow<AppStartState> = settingsRepository.observeSettings()
        .map {
            AppStartState(
                onboardingCompleted = it.onboardingCompleted,
                themeMode = it.themeMode,
                needsMadhabPrompt = it.onboardingCompleted && !it.madhabChosen,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppStartState())

    /** Saves the school chosen in the one-time prompt and reschedules reminders, since Asr (and
     * potentially other) times now depend on it. */
    fun onMadhabChosen(madhab: Madhab) {
        viewModelScope.launch {
            settingsRepository.setMadhab(madhab)
            prayerReminderRescheduler.rescheduleAll()
        }
    }
}
