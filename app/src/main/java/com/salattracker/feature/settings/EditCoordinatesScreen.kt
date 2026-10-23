package com.salattracker.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.CitySearchField
import com.salattracker.core.ui.CitySearchResults
import com.salattracker.core.ui.SalatPrimaryButton
import com.salattracker.domain.model.CityResult

@Composable
fun EditCoordinatesScreen(
    uiState: EditCoordinatesUiState,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onSaveCoordinates: () -> Unit,
    onUseMyLocationClick: () -> Unit,
    onSearchCityClick: () -> Unit,
    onCityQueryChanged: (String) -> Unit,
    onCitySelected: (CityResult) -> Unit,
    onErrorDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    LaunchedEffect(uiState.isDone) { if (uiState.isDone) onDone() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.mode) {
            EditLocationMode.COORDINATES -> CoordinatesContent(
                latitude = uiState.latitude, longitude = uiState.longitude, latitudeInvalid = uiState.latitudeInvalid, longitudeInvalid = uiState.longitudeInvalid, isLocating = uiState.isLocating,
                onLatitudeChanged = onLatitudeChanged, onLongitudeChanged = onLongitudeChanged,
                onSave = onSaveCoordinates, onUseMyLocationClick = onUseMyLocationClick, onSearchCityClick = onSearchCityClick,
            )
            EditLocationMode.CITY_SEARCH -> Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                CitySearchField(query = uiState.cityQuery, onQueryChanged = onCityQueryChanged)
                Spacer(Modifier.height(16.dp))
                CitySearchResults(
                    query = uiState.cityQuery, results = uiState.cityResults, isSearching = uiState.isSearchingCity,
                    selected = null, onSelect = onCitySelected,
                )
            }
        }

        uiState.errorRes?.let { res ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(res), fontFamily = AppUiFontFamily) }
        }
    }
}

@Composable
private fun CoordinatesContent(
    latitude: String,
    longitude: String,
    latitudeInvalid: Boolean,
    longitudeInvalid: Boolean,
    isLocating: Boolean,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onSave: () -> Unit,
    onUseMyLocationClick: () -> Unit,
    onSearchCityClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.settings_edit_location_coordinates_label), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoordField(
                label = stringResource(R.string.settings_latitude), value = latitude, onChanged = onLatitudeChanged, isError = latitudeInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
            CoordField(
                label = stringResource(R.string.settings_longitude), value = longitude, onChanged = onLongitudeChanged, isError = longitudeInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onSave() }),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))
        SalatPrimaryButton(
            onClick = onSave,
            enabled = latitude.toDoubleOrNull() != null && longitude.toDoubleOrNull() != null && !latitudeInvalid && !longitudeInvalid,
        ) {
            Text(stringResource(R.string.settings_edit_location_save), fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onUseMyLocationClick,
            enabled = !isLocating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SalatOutlineVariant),
        ) {
            if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SalatPrimary)
            } else {
                Icon(painterResource(R.drawable.ic_my_location), null, tint = SalatPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_edit_location_use_my_location), color = SalatPrimary, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSearchCityClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_edit_location_search_city), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun CoordField(
    label: String,
    value: String,
    onChanged: (String) -> Unit,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier = modifier) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
        value = value,
        isError = isError,
            onValueChange = onChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 14.sp, color = SalatOnSurface),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            supportingText = if (isError) { { Text(stringResource(R.string.settings_coordinate_range_error), fontFamily = AppUiFontFamily) } } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SalatPrimary, unfocusedBorderColor = SalatOutlineVariant,
                focusedContainerColor = SalatSurfaceContainerLow, unfocusedContainerColor = SalatSurfaceContainerLow,
            ),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditCoordinatesScreenPreview() {
    EditCoordinatesScreen(
        uiState = EditCoordinatesUiState(latitude = "24.4672", longitude = "39.6111"),
        onLatitudeChanged = {}, onLongitudeChanged = {}, onSaveCoordinates = {}, onUseMyLocationClick = {},
        onSearchCityClick = {}, onCityQueryChanged = {}, onCitySelected = {}, onErrorDismiss = {}, onDone = {},
    )
}
