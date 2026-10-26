package com.salattracker.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.CityResult
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

    @Test
    fun `city country suggests a calculation method until the user chooses one`() {
        val viewModel = viewModel(SavedStateHandle())
        val karachi = CityResult("Karachi", "Sindh", "Pakistan", 24.86, 67.01, "PK")

        viewModel.onCitySelected(karachi)
        assertEquals(CalculationMethod.KARACHI, viewModel.uiState.value.calculationMethod)

        viewModel.onCalculationMethodSelected(CalculationMethod.MWL)
        viewModel.onCitySelected(karachi.copy(country = "Turkey", countryCode = "TR"))
        assertEquals(CalculationMethod.MWL, viewModel.uiState.value.calculationMethod)
    }

    @Test
    fun `confirmed onboarding city is persisted before advancing to reminders`() {
        val repository = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(
            repository, FakeGeocodingRepository(), FakePrayerRepository(),
            FakePrayerReminderRescheduler(), SavedStateHandle(),
        )
        val karachi = CityResult("Karachi", "Sindh", "Pakistan", 24.86, 67.01, "PK")

        viewModel.onCitySelected(karachi)
        viewModel.onConfirmLocation()

        assertEquals(listOf(24.86 to 67.01), repository.locationCalls)
        assertEquals(listOf("Karachi, Sindh, Pakistan"), repository.locationLabelCalls)
        assertEquals(OnboardingStep.REMINDERS, viewModel.uiState.value.step)
    }

    private fun viewModel(handle: SavedStateHandle) = OnboardingViewModel(
        FakeSettingsRepository(), FakeGeocodingRepository(), FakePrayerRepository(),
        FakePrayerReminderRescheduler(), handle,
    )

    @Test
    fun catchUpPeriodRestoresAndBackReturnsToOverview() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.onRemindersNext()
        assertEquals(OnboardingStep.CATCH_UP, first.uiState.value.step)
        first.onCatchUpNext()
        first.onCatchUpDurationChanged(CatchUpDuration.WEEK)

        val restored = viewModel(handle)
        assertEquals(OnboardingStep.CATCH_UP_RANGE, restored.uiState.value.step)
        assertEquals(CatchUpDuration.WEEK, restored.uiState.value.catchUpDuration)
        restored.onBack()
        assertEquals(OnboardingStep.CATCH_UP, restored.uiState.value.step)
        restored.onCatchUpSkip()
        assertEquals(OnboardingStep.DONE, restored.uiState.value.step)
    }
}
