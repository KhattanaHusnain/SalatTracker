package com.salattracker.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for Settings, reached from the drawer. Every control live-applies, so there
 * is nothing to lose on back navigation — no BackHandler needed here any more.
 */
@Composable
fun SettingsEntry(
    onPrayerRemindersClick: () -> Unit,
    onBackupClick: () -> Unit,
    backupNeedsAttention: Boolean,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.fetchDeviceLocation() }

    SettingsScreen(
        uiState                    = uiState,
        onUseDeviceLocationChanged = { enabled ->
            viewModel.onUseDeviceLocationChanged(enabled)
            if (enabled) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.fetchDeviceLocation()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        },
        onLatitudeChanged               = viewModel::onLatitudeChanged,
        onLongitudeChanged              = viewModel::onLongitudeChanged,
        onLocationCommit                = viewModel::onLocationCommit,
        onCalculationMethodSheetOpen    = viewModel::onCalculationMethodSheetOpen,
        onCalculationMethodSheetDismiss = viewModel::onCalculationMethodSheetDismiss,
        onCalculationMethodChanged      = viewModel::onCalculationMethodChanged,
        on24HourFormatChanged           = viewModel::on24HourFormatChanged,
        onLanguageChanged               = viewModel::onLanguageChanged,
        onThemeModeChanged              = viewModel::onThemeModeChanged,
        onErrorDismiss                  = viewModel::clearError,
        onPrayerRemindersClick          = onPrayerRemindersClick,
        onBackupClick                   = onBackupClick,
        backupNeedsAttention            = backupNeedsAttention,
    )
}
