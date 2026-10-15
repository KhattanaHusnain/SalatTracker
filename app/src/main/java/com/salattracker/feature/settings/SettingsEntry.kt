package com.salattracker.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for Settings, reached from the drawer. [onDiscardAndBack] is called once
 * the user has confirmed discarding unsaved changes (or there were none to discard) — it
 * performs the actual navigation back, which the ViewModel itself has no way to trigger.
 */
@Composable
fun SettingsEntry(
    onDiscardAndBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Intercepts the back gesture/button only while there is something to lose — falls through
    // to the NavHost's normal pop otherwise. The top bar's back arrow goes through the same
    // OnBackPressedDispatcher (see MainActivity's TopBar), so it is caught here too.
    BackHandler(enabled = viewModel.hasUnsavedChanges()) {
        viewModel.onBackRequested()
    }

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
        onLatitudeChanged          = viewModel::onLatitudeChanged,
        onLongitudeChanged         = viewModel::onLongitudeChanged,
        onCalculationMethodChanged = viewModel::onCalculationMethodChanged,
        on24HourFormatChanged      = viewModel::on24HourFormatChanged,
        onLanguageChanged          = viewModel::onLanguageChanged,
        onThemeModeChanged         = viewModel::onThemeModeChanged,
        onSave                     = viewModel::onSave,
        onErrorDismiss             = viewModel::clearError,
        onClearSaveSuccess         = viewModel::clearSaveSuccess,
        onDiscardConfirm           = { viewModel.onDiscardConfirm(); onDiscardAndBack() },
        onKeepEditingClick         = viewModel::onKeepEditingClick,
    )
}
