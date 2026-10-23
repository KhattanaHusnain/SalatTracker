package com.salattracker.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.feature.backup.BackupEntry

/**
 * Nav entry-point for the first-run onboarding flow.
 * [onAllowLocation] is supplied by MainActivity so the Location step can trigger the system
 * permission request (the Activity owns the permission launcher) — the ViewModel itself just
 * watches for the location fix that flow writes to SettingsRepository once granted.
 * [onRequestNotifications] is fired when leaving the Reminders step with reminders turned on.
 */
@Composable
fun OnboardingEntry(
    onAllowLocation: ((Boolean) -> Unit) -> Unit,
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

    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME, onBack = viewModel::onBack)

    OnboardingScreen(
        uiState                   = uiState,
        onLanguageSelected        = viewModel::onLanguageSelected,
        onThemeSelected           = viewModel::onThemeSelected,
        onWelcomeNext             = viewModel::onWelcomeNext,
        onRestoreFromBackup       = viewModel::onRestoreClick,
        onGenderSelected          = viewModel::onGenderSelected,
        onAboutYouNext            = viewModel::onAboutYouNext,
        onMadhabSelected          = viewModel::onMadhabSelected,
        onSchoolNext              = viewModel::onSchoolNext,
        onAllowLocationClick      = {
            viewModel.onLocationRequestStarted()
            onAllowLocation(viewModel::onLocationPermissionResult)
        },
        onEnterCityInstead        = viewModel::onEnterCityInstead,
        onCityQueryChanged        = viewModel::onCityQueryChanged,
        onCitySelected            = viewModel::onCitySelected,
        onCitySearchContinue      = viewModel::onCitySearchContinue,
        onUseMyLocationInstead    = viewModel::onUseMyLocationInstead,
        onConfirmLocation         = viewModel::onConfirmLocation,
        onChangeLocationClick     = viewModel::onChangeLocationClick,
        onCalculationMethodSheetOpen    = viewModel::onCalculationMethodSheetOpen,
        onCalculationMethodSheetDismiss = viewModel::onCalculationMethodSheetDismiss,
        onCalculationMethodSelected     = viewModel::onCalculationMethodSelected,
        onPrayerRemindersChanged  = viewModel::onPrayerRemindersChanged,
        onFullScreenAlertChanged  = viewModel::onFullScreenAlertChanged,
        onRemindersSkip           = viewModel::onRemindersNext,
        onRemindersNext           = {
            onRequestNotifications()
            viewModel.onRemindersNext()
        },
        onCatchUpAddClick         = viewModel::onCatchUpAddClick,
        onCatchUpSkip             = viewModel::onCatchUpSkip,
        onCatchUpDurationChanged  = viewModel::onCatchUpDurationChanged,
        onCatchUpCustomRangeChanged = viewModel::onCatchUpCustomRangeChanged,
        onApplyCatchUp            = viewModel::onApplyCatchUp,
        onUndoCatchUp             = viewModel::onUndoCatchUp,
        onFinish                  = viewModel::onFinish,
    )
}
