package com.salattracker.feature.mosque

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatInverseOnSurface
import com.salattracker.core.theme.SalatInverseSurface
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.ui.SettingsCard
import com.salattracker.core.ui.SettingsSectionLabel
import com.salattracker.core.ui.salatSwitchColors
import com.salattracker.domain.model.LocationAccess
import com.salattracker.domain.model.Mosque
import androidx.compose.foundation.BorderStroke

@Composable
fun MosqueModeScreen(
    uiState: MosqueModeUiState,
    accessActionIsSettings: Boolean,
    showDisclosure: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onAccessAction: () -> Unit,
    onTurnOnLocation: () -> Unit,
    onRestoreSound: () -> Unit,
    onAddCurrentLocation: () -> Unit,
    onFindNearby: () -> Unit,
    onMosqueClick: (Mosque) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onDraftRadiusChange: (Int) -> Unit,
    onDraftSave: () -> Unit,
    onDraftDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenInMaps: (latitude: Double, longitude: Double, name: String) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onNearbySelect: (NearbyItem) -> Unit,
    onNearbyRetry: () -> Unit,
    onNearbyDismiss: () -> Unit,
    onDisclosureContinue: () -> Unit,
    onDisclosureDismiss: () -> Unit,
    onMessageDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            MainCard(uiState, onEnabledChange, onRestoreSound)

            if (!uiState.canVibrate) {
                MosqueNoticeCard(stringResource(R.string.mosque_unavailable), isError = true)
            } else if (uiState.enabled) {
                if (uiState.access == LocationAccess.NONE || uiState.access == LocationAccess.FOREGROUND_ONLY) {
                    MosqueNoticeCard(
                        text = stringResource(R.string.mosque_access_needed),
                        isError = true,
                        actionLabel = stringResource(if (accessActionIsSettings) R.string.mosque_access_open_settings else R.string.mosque_access_allow),
                        onAction = onAccessAction,
                    )
                }
                if (uiState.locationOff) {
                    MosqueNoticeCard(
                        text = stringResource(R.string.mosque_location_off),
                        isError = true,
                        actionLabel = stringResource(R.string.mosque_location_turn_on),
                        onAction = onTurnOnLocation,
                    )
                }
            }

            SettingsSectionLabel(stringResource(R.string.mosque_my_mosques))
            if (uiState.mosques.isEmpty()) {
                SettingsCard {
                    Text(stringResource(R.string.mosque_empty), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
                }
            } else {
                SettingsCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    uiState.mosques.forEachIndexed { index, mosque ->
                        if (index > 0) HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
                        MosqueRow(mosque.name, mosque.radiusMeters, onClick = { onMosqueClick(mosque) })
                    }
                }
            }

            com.salattracker.core.ui.SalatPrimaryButton(
                onClick = onAddCurrentLocation,
                enabled = !uiState.isLocating && !uiState.atLimit,
            ) {
                if (uiState.isLocating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
                } else {
                    Icon(painterResource(R.drawable.ic_my_location), null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mosque_add_current), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onFindNearby,
                enabled = !uiState.atLimit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SalatOutlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SalatPrimary),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mosque_find_nearby), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }

        uiState.message?.let { message ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onMessageDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(message.textRes), fontFamily = AppUiFontFamily) }
        }
    }

    uiState.draft?.let { draft ->
        MosqueEditSheet(
            draft = draft,
            onNameChange = onDraftNameChange,
            onRadiusChange = onDraftRadiusChange,
            onSave = onDraftSave,
            onDelete = onDeleteClick,
            onOpenInMaps = onOpenInMaps,
            onDismiss = onDraftDismiss,
        )
        if (uiState.showDeleteConfirm) {
            DeleteMosqueDialog(draft.name, onConfirm = onDeleteConfirm, onDismiss = onDeleteDismiss)
        }
    }

    uiState.nearby?.let { nearby ->
        NearbyMosquesSheet(nearby, onSelect = onNearbySelect, onRetry = onNearbyRetry, onDismiss = onNearbyDismiss)
    }

    if (showDisclosure) {
        BackgroundLocationDisclosureDialog(onContinue = onDisclosureContinue, onDismiss = onDisclosureDismiss)
    }
}

@Composable
private fun MainCard(
    uiState: MosqueModeUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRestoreSound: () -> Unit,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_mosque_outline), null, tint = SalatPrimary, modifier = Modifier.size(24.dp))
            Text(
                stringResource(R.string.mosque_toggle_title),
                modifier = Modifier.weight(1f), color = SalatOnSurface, fontSize = 16.sp, fontFamily = AppUiFontFamily,
            )
            Switch(
                checked = uiState.enabled,
                onCheckedChange = onEnabledChange,
                enabled = uiState.canVibrate,
                colors = salatSwitchColors(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.mosque_toggle_desc),
            modifier = Modifier.padding(start = 36.dp),
            color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
        )

        uiState.vibrateActiveAt?.let { name ->
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.mosque_vibrate_on, name),
                    modifier = Modifier.weight(1f), color = SalatOnSurface, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily,
                )
                TextButton(onClick = onRestoreSound) {
                    Text(stringResource(R.string.mosque_restore_sound), color = SalatPrimary, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                }
            }
        }
    }
}

private val MosqueMessage.textRes: Int
    get() = when (this) {
        MosqueMessage.LOCATION_UNAVAILABLE -> R.string.mosque_msg_location_unavailable
        MosqueMessage.LIMIT_REACHED -> R.string.mosque_msg_limit
        MosqueMessage.SAVE_FAILED -> R.string.mosque_msg_save_failed
        MosqueMessage.SYNC_FAILED -> R.string.mosque_msg_sync_failed
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MosqueModeScreenPreview() {
    MosqueModeScreen(
        uiState = MosqueModeUiState(
            enabled = true,
            mosques = listOf(
                Mosque(1, "Masjid Al-Noor", 24.4672, 39.6111, 100),
                Mosque(2, "Jamia Masjid", 24.47, 39.62, 150),
            ),
            vibrateActiveAt = "Masjid Al-Noor",
            access = LocationAccess.FOREGROUND_ONLY,
        ),
        accessActionIsSettings = false, showDisclosure = false,
        onEnabledChange = {}, onAccessAction = {}, onTurnOnLocation = {}, onRestoreSound = {},
        onAddCurrentLocation = {}, onFindNearby = {}, onMosqueClick = {},
        onDraftNameChange = {}, onDraftRadiusChange = {}, onDraftSave = {}, onDraftDismiss = {},
        onDeleteClick = {}, onOpenInMaps = { _, _, _ -> }, onDeleteConfirm = {}, onDeleteDismiss = {},
        onNearbySelect = {}, onNearbyRetry = {}, onNearbyDismiss = {},
        onDisclosureContinue = {}, onDisclosureDismiss = {}, onMessageDismiss = {},
    )
}
