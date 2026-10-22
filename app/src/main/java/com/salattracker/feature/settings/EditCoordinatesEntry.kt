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
 * Nav entry-point for Settings' "Location" row (Routes.EDIT_COORDINATES). [onDone] pops this
 * screen back to Settings — fired once a coordinate save, device-location fix, or city pick
 * succeeds (see EditCoordinatesViewModel.isDone).
 */
@Composable
fun EditCoordinatesEntry(
    onDone: () -> Unit,
    viewModel: EditCoordinatesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onUseMyLocationClick() }

    // While searching a city, the top bar's back arrow (and system back) should return to the
    // coordinates view rather than pop this whole screen — matches onboarding's own
    // "back from any search state returns to the location screen" convention.
    BackHandler(enabled = uiState.mode == EditLocationMode.CITY_SEARCH, onBack = viewModel::onBackFromCitySearch)

    EditCoordinatesScreen(
        uiState = uiState,
        onLatitudeChanged = viewModel::onLatitudeChanged,
        onLongitudeChanged = viewModel::onLongitudeChanged,
        onSaveCoordinates = viewModel::onSaveCoordinates,
        onUseMyLocationClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                viewModel.onUseMyLocationClick()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
        onSearchCityClick = viewModel::onSearchCityClick,
        onCityQueryChanged = viewModel::onCityQueryChanged,
        onCitySelected = viewModel::onCitySelected,
        onErrorDismiss = viewModel::clearError,
        onDone = onDone,
    )
}
