package com.salattracker.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.feature.backup.BackupEntry

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

    if (uiState.showRestore) {
        // A successful restore sets onboarding_completed itself (BackupLocalSource.applySnapshot),
        // so MainActivity's reactive gate swaps to the main app on its own — nothing to do here.
        BackHandler(onBack = viewModel::onRestoreDismiss)
        BackupEntry(isOnboarding = true)
        return
    }

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
        onRestoreFromBackup      = viewModel::onRestoreClick,
    )
}
