package com.salattracker.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.Gender
import com.salattracker.testutil.FakeGeocodingRepository
import com.salattracker.testutil.FakePrayerReminderRescheduler
import com.salattracker.testutil.FakePrayerRepository
import com.salattracker.testutil.FakeSettingsRepository
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun choicesAndStepSurviveSavedStateRestoration() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.onLanguageSelected(AppLanguage.URDU)
        first.onWelcomeNext()
        first.onGenderSelected(Gender.FEMALE)

        val restored = viewModel(handle)

        assertEquals(OnboardingStep.ABOUT_YOU, restored.uiState.value.step)
        assertEquals(AppLanguage.URDU, restored.uiState.value.language)
        assertEquals(Gender.FEMALE, restored.uiState.value.gender)
        restored.onBack()
        assertEquals(OnboardingStep.WELCOME, restored.uiState.value.step)
    }

    private fun viewModel(handle: SavedStateHandle) = OnboardingViewModel(
        FakeSettingsRepository(), FakeGeocodingRepository(), FakePrayerRepository(),
        FakePrayerReminderRescheduler(), handle,
    )
}
