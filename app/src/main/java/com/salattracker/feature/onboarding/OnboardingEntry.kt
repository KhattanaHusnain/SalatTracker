package com.salattracker.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the first-run onboarding flow.
 * [onAllowLocation] is supplied by MainActivity so the Location step can trigger the
 * system permission request (the Activity owns the permission launcher).
 */
@Composable
fun OnboardingEntry(
    onAllowLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    OnboardingScreen(
        uiState                  = uiState,
        onLanguageSelected       = viewModel::onLanguageSelected,
        onThemeSelected          = viewModel::onThemeSelected,
        onPrayerRemindersChanged = viewModel::onPrayerRemindersChanged,
        onDailyTipsChanged       = viewModel::onDailyTipsChanged,
        onNext                   = viewModel::onNext,
        onAllowLocation          = onAllowLocation,
        onFinish                 = {
            onRequestNotifications()
            viewModel.onFinish()
        },
    )
}
