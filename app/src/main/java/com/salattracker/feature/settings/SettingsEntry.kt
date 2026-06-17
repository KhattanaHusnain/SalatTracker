package com.salattracker.feature.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.feature.auth.LoginActivity

/**
 * Nav entry-point for the Settings tab.
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Settings.route) { SettingsEntry(onNavigateToLogin) }
 */
@Composable
fun SettingsEntry(
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to LoginActivity when sign-out completes
    LaunchedEffect(uiState.navigateToLogin) {
        if (uiState.navigateToLogin) {
            viewModel.navigationHandled()
            onNavigateToLogin()
        }
    }

    SettingsScreen(
        uiState                    = uiState,
        onUseDeviceLocationChanged = viewModel::onUseDeviceLocationChanged,
        onLatitudeChanged          = viewModel::onLatitudeChanged,
        onLongitudeChanged         = viewModel::onLongitudeChanged,
        onCalculationMethodChanged = viewModel::onCalculationMethodChanged,
        onReminderEnabledChanged   = viewModel::onReminderEnabledChanged,
        onReminderOffsetChanged    = viewModel::onReminderOffsetChanged,
        on24HourFormatChanged      = viewModel::on24HourFormatChanged,
        onSave                     = viewModel::onSave,
        onSignOutRequest           = viewModel::onSignOutRequest,
        onErrorDismiss             = viewModel::clearError,
        onClearSaveSuccess         = viewModel::clearSaveSuccess,
    )

    // Sign-out confirmation dialog wired to ViewModel
    if (uiState.signOutConfirmVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::onSignOutCancel,
            containerColor   = com.salattracker.core.theme.SalatSurface,
            title = {
                androidx.compose.material3.Text(
                    "Sign Out?",
                    color      = com.salattracker.core.theme.SalatPrimary,
                    fontFamily = com.salattracker.core.theme.MontserratFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
            },
            text = {
                androidx.compose.material3.Text(
                    "You'll be signed out of your account. Local data will remain on this device.",
                    color      = com.salattracker.core.theme.SalatOnSurfaceVariant,
                    fontFamily = com.salattracker.core.theme.InterFamily,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::onSignOutConfirm) {
                    androidx.compose.material3.Text(
                        "Sign Out",
                        color      = com.salattracker.core.theme.SalatError,
                        fontFamily = com.salattracker.core.theme.InterFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::onSignOutCancel) {
                    androidx.compose.material3.Text(
                        "Cancel",
                        color      = com.salattracker.core.theme.SalatOnSurfaceVariant,
                        fontFamily = com.salattracker.core.theme.InterFamily,
                    )
                }
            },
        )
    }
}