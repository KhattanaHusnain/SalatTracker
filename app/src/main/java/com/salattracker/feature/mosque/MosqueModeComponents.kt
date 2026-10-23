package com.salattracker.feature.mosque

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatErrorContainer
import com.salattracker.core.theme.SalatOnErrorContainer
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSecondaryContainer
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatSurfaceContainerLow
import com.salattracker.core.theme.SalatSurfaceContainerLowest
import com.salattracker.domain.model.MosqueMode

// ── Notice card (permission / location / unavailable) ──────────────────────────

@Composable
fun MosqueNoticeCard(
    text: String,
    isError: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = if (isError) SalatErrorContainer else SalatSecondaryContainer,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text, color = if (isError) SalatOnErrorContainer else SalatOnSecondaryContainer,
                fontSize = 13.sp, fontFamily = AppUiFontFamily,
            )
            if (actionLabel != null) {
                TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                    Text(actionLabel, color = SalatPrimary, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                }
            }
        }
    }
}

// ── Saved-mosque row ────────────────────────────────────────────────────────────

@Composable
fun MosqueRow(name: String, radiusMeters: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_mosque_outline), null, tint = SalatPrimary, modifier = Modifier.size(24.dp))
        Text(
            name, modifier = Modifier.weight(1f), color = SalatOnSurface, fontSize = 16.sp,
            fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(R.string.mosque_distance_meters, radiusMeters),
            color = SalatOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
        )
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

// ── Add / edit sheet ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueEditSheet(
    draft: MosqueDraft,
    onNameChange: (String) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenInMaps: (latitude: Double, longitude: Double, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = draft.id != null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SalatSurfaceContainerLowest,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(if (isEditing) R.string.mosque_sheet_edit_title else R.string.mosque_sheet_add_title),
                color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = draft.nameMissing,
                label = { Text(stringResource(R.string.mosque_name_label), fontFamily = AppUiFontFamily) },
                supportingText = if (draft.nameMissing) {
                    { Text(stringResource(R.string.mosque_name_required), fontFamily = AppUiFontFamily) }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { onOpenInMaps(draft.latitude, draft.longitude, draft.name) }) {
                        Icon(
                            painterResource(R.drawable.ic_map_pin),
                            contentDescription = stringResource(R.string.mosque_open_in_maps),
                            tint = SalatPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                textStyle = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 14.sp, color = SalatOnSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SalatPrimary,
                    unfocusedBorderColor = SalatOutlineVariant,
                    focusedContainerColor = SalatSurfaceContainerLow,
                    unfocusedContainerColor = SalatSurfaceContainerLow,
                ),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.mosque_radius_label), color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MosqueMode.RADIUS_OPTIONS_METERS.forEach { radius ->
                    RadiusChip(
                        label = stringResource(R.string.mosque_distance_meters, radius),
                        isSelected = radius == draft.radiusMeters,
                        onClick = { onRadiusChange(radius) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.mosque_radius_hint), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)

            Spacer(Modifier.height(20.dp))
            com.salattracker.core.ui.SalatPrimaryButton(
                onClick = onSave,
            ) { Text(stringResource(R.string.mosque_save), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }

            if (isEditing) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_delete), color = SalatError, fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RadiusChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SalatPrimary else SalatSurfaceContainerLow,
        border = BorderStroke(1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            color = if (isSelected) SalatOnPrimary else SalatOnSurfaceVariant,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
        )
    }
}

// ── Nearby sheet ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyMosquesSheet(
    state: NearbyUiState,
    onSelect: (NearbyItem) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.mosque_nearby_title),
                color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SalatPrimary)
                }
                state.failure != null -> Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val message = when (state.failure) {
                        NearbyFailure.LOCATION_UNAVAILABLE -> stringResource(R.string.mosque_nearby_location_failed)
                        NearbyFailure.SEARCH_FAILED -> stringResource(R.string.mosque_nearby_failed)
                    }
                    Text(message, color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.mosque_nearby_retry), color = SalatPrimary, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                    }
                }
                state.results.isEmpty() -> Text(
                    stringResource(R.string.mosque_nearby_empty),
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
                )
                else -> LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(state.results) { item -> NearbyRow(item, onClick = { onSelect(item) }) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.mosque_osm_credit),
                color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
    }
}

@Composable
private fun NearbyRow(item: NearbyItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !item.alreadyAdded, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.mosque.name ?: stringResource(R.string.mosque_nearby_unnamed),
                color = if (item.alreadyAdded) SalatOnSurfaceVariant else SalatOnSurface,
                fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.mosque_distance_meters, item.mosque.distanceMeters),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
        if (item.alreadyAdded) {
            Text(
                stringResource(R.string.mosque_nearby_added),
                color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
            )
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────────

/** Google Play's "prominent disclosure" for background location — shown before the system prompt. */
@Composable
fun BackgroundLocationDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SalatSurfaceContainerLowest,
        title = {
            Text(
                stringResource(R.string.mosque_disclosure_title),
                color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
        },
        text = {
            Text(
                stringResource(R.string.mosque_disclosure_body),
                color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
            )
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.action_continue), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
        },
    )
}

@Composable
fun DeleteMosqueDialog(mosqueName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SalatSurfaceContainerLowest,
        icon = {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(50)).background(SalatErrorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_delete), null, tint = SalatOnErrorContainer, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                stringResource(R.string.mosque_delete_title),
                color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.mosque_delete_desc), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
                Spacer(Modifier.height(12.dp))
                Text(mosqueName, color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SalatError, contentColor = SalatOnPrimary),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.action_delete), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
        },
    )
}
