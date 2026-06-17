package com.salattracker.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.PrayerType

// ── Settings Screen ───────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUseDeviceLocationChanged: (Boolean) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onReminderEnabledChanged: (PrayerType, Boolean) -> Unit,
    onReminderOffsetChanged: (PrayerType, String) -> Unit,
    on24HourFormatChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onSignOutRequest: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Account ───────────────────────────────────────────────────────
            AccountSection(
                displayName    = uiState.displayName,
                email          = uiState.email,
                photoUrl       = uiState.photoUrl,
                onSignOutClick = onSignOutRequest,
            )

            // ── Location ──────────────────────────────────────────────────────
            SettingsSectionLabel("Location")
            LocationSection(
                useDeviceLocation = uiState.useDeviceLocation,
                latitude          = uiState.latitude,
                longitude         = uiState.longitude,
                onToggleDevice    = onUseDeviceLocationChanged,
                onLatChanged      = onLatitudeChanged,
                onLonChanged      = onLongitudeChanged,
            )

            // ── Calculation method ────────────────────────────────────────────
            SettingsSectionLabel("Prayer Calculation")
            CalculationSection(
                selected  = uiState.calculationMethod,
                onChanged = onCalculationMethodChanged,
            )

            // ── Reminders ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                SettingsSectionLabel("Prayer Reminders", modifier = Modifier)
                Text("Minutes Offset", color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
            RemindersSection(
                reminders       = uiState.reminders,
                onToggle        = onReminderEnabledChanged,
                onOffsetChanged = onReminderOffsetChanged,
            )

            // ── Display ───────────────────────────────────────────────────────
            SettingsSectionLabel("Display")
            DisplaySection(
                use24Hour   = uiState.use24HourFormat,
                onToggle    = on24HourFormatChanged,
            )

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = onSave,
                enabled  = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SalatPrimary,
                    contentColor   = SalatOnPrimary,
                ),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(painterResource(R.drawable.ic_save), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Sign-out confirm dialog ───────────────────────────────────────────
        if (uiState.signOutConfirmVisible) {
            AlertDialog(
                onDismissRequest = onSignOutRequest,
                containerColor   = SalatSurface,
                title = { Text("Sign Out?", color = SalatPrimary, fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold) },
                text  = { Text("You'll be signed out of your account. Local data will remain.", color = SalatOnSurfaceVariant, fontFamily = InterFamily) },
                confirmButton = {
                    TextButton(onClick = {}) { // wired in SettingsActivity
                        Text("Sign Out", color = SalatError, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {}) {
                        Text("Cancel", color = SalatOnSurfaceVariant, fontFamily = InterFamily)
                    }
                },
            )
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text("OK", color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }

        // ── Save success snackbar ─────────────────────────────────────────────
        if (uiState.saveSuccess) {
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onClearSaveSuccess) { Text("OK", color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) { Text("Settings saved.", fontFamily = InterFamily) }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier,
        color         = SalatPrimaryContainer,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        fontFamily    = InterFamily,
        letterSpacing = 1.5.sp,
    )
}

// ── Account section ───────────────────────────────────────────────────────────

@Composable
private fun AccountSection(
    displayName: String,
    email: String,
    photoUrl: String?,
    onSignOutClick: () -> Unit,
) {
    SettingsCard {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar
            Box(modifier = Modifier.size(64.dp)) {
                if (photoUrl != null) {
                    AsyncImage(
                        model             = photoUrl,
                        contentDescription = "Profile photo",
                        modifier          = Modifier.fillMaxSize().clip(CircleShape)
                            .border(2.dp, SalatPrimaryFixed, CircleShape),
                        contentScale      = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier         = Modifier.fillMaxSize().clip(CircleShape)
                            .background(SalatSecondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_person), null,
                            tint = SalatOnSecondaryContainer, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = displayName.ifEmpty { "User" },
                    color      = SalatPrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
                Text(
                    text     = email,
                    color    = SalatOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = InterFamily,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Out button
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SalatOnErrorContainer),
            border   = BorderStroke(1.dp, SalatErrorContainer),
        ) {
            Icon(painterResource(R.drawable.ic_logout), null,
                modifier = Modifier.size(18.dp), tint = SalatOnErrorContainer)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = SalatOnErrorContainer)
        }
    }
}

// ── Location section ──────────────────────────────────────────────────────────

@Composable
private fun LocationSection(
    useDeviceLocation: Boolean,
    latitude: String,
    longitude: String,
    onToggleDevice: (Boolean) -> Unit,
    onLatChanged: (String) -> Unit,
    onLonChanged: (String) -> Unit,
) {
    SettingsCard {
        // Device location toggle
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_my_location), null,
                    tint = SalatPrimary, modifier = Modifier.size(24.dp))
                Text("Device Location", color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
            }
            Switch(
                checked         = useDeviceLocation,
                onCheckedChange = onToggleDevice,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = SalatOnPrimary,
                    checkedTrackColor   = SalatPrimary,
                    uncheckedThumbColor = SalatOutline,
                    uncheckedTrackColor = SalatOutlineVariant,
                ),
            )
        }

        // Manual lat/lon (only editable when device location is off)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoordField(
                label    = "Latitude",
                value    = latitude,
                onChanged = onLatChanged,
                enabled  = !useDeviceLocation,
                modifier = Modifier.weight(1f),
            )
            CoordField(
                label    = "Longitude",
                value    = longitude,
                onChanged = onLonChanged,
                enabled  = !useDeviceLocation,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CoordField(
    label: String,
    value: String,
    onChanged: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onChanged,
            enabled       = enabled,
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(
                fontFamily = InterFamily, fontSize = 14.sp, color = SalatOnSurface,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = SalatPrimary,
                unfocusedBorderColor = SalatOutlineVariant,
                disabledBorderColor  = SalatOutlineVariant.copy(alpha = 0.5f),
                focusedContainerColor   = SalatSurfaceContainerLow,
                unfocusedContainerColor = SalatSurfaceContainerLow,
                disabledContainerColor  = SalatSurfaceContainerLow.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

// ── Calculation method section ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationSection(
    selected: CalculationMethod,
    onChanged: (CalculationMethod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsCard {
        Column {
            Text("Calculation Method", color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded         = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value         = selected.label,
                    onValueChange = {},
                    readOnly      = true,
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine    = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    textStyle     = LocalTextStyle.current.copy(fontFamily = InterFamily, fontSize = 14.sp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SalatPrimary,
                        unfocusedBorderColor = SalatOutlineVariant,
                        focusedContainerColor   = SalatSurfaceContainerLow,
                        unfocusedContainerColor = SalatSurfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false },
                    modifier         = Modifier.background(SalatSurface),
                ) {
                    CalculationMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text   = { Text(method.label, fontFamily = InterFamily, fontSize = 14.sp, color = SalatOnSurface) },
                            onClick = {
                                onChanged(method)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (method == selected) SalatPrimary else SalatOnSurface,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Reminders section ─────────────────────────────────────────────────────────

@Composable
private fun RemindersSection(
    reminders: List<ReminderSetting>,
    onToggle: (PrayerType, Boolean) -> Unit,
    onOffsetChanged: (PrayerType, String) -> Unit,
) {
    SettingsCard(contentPadding = PaddingValues(0.dp)) {
        reminders.forEachIndexed { index, setting ->
            if (index > 0) {
                HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.3f))
            }
            ReminderRow(
                setting         = setting,
                onToggle        = { onToggle(setting.prayerType, it) },
                onOffsetChanged = { onOffsetChanged(setting.prayerType, it) },
            )
        }
    }
}

@Composable
private fun ReminderRow(
    setting: ReminderSetting,
    onToggle: (Boolean) -> Unit,
    onOffsetChanged: (String) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.weight(1f),
        ) {
            Switch(
                checked         = setting.enabled,
                onCheckedChange = onToggle,
                modifier        = Modifier.height(24.dp),
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = SalatOnPrimary,
                    checkedTrackColor   = SalatPrimary,
                    uncheckedThumbColor = SalatOutline,
                    uncheckedTrackColor = SalatOutlineVariant,
                ),
            )
            Text(
                text       = setting.prayerType.displayName,
                color      = if (setting.enabled) SalatPrimary else SalatOnSurfaceVariant,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
            )
        }
        OutlinedTextField(
            value         = setting.offsetMinutes.toString(),
            onValueChange = onOffsetChanged,
            enabled       = setting.enabled,
            modifier      = Modifier.width(72.dp),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(
                fontFamily = InterFamily, fontSize = 14.sp,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = SalatPrimary,
                unfocusedBorderColor = SalatOutlineVariant,
                disabledBorderColor  = SalatOutlineVariant.copy(alpha = 0.4f),
                focusedContainerColor   = SalatSurfaceContainerLow,
                unfocusedContainerColor = SalatSurfaceContainerLow,
                disabledContainerColor  = SalatSurfaceContainerLow.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

// ── Display section ───────────────────────────────────────────────────────────

@Composable
private fun DisplaySection(
    use24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    SettingsCard {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("24-Hour Format", color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
            Switch(
                checked         = use24Hour,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = SalatOnPrimary,
                    checkedTrackColor   = SalatPrimary,
                    uncheckedThumbColor = SalatOutline,
                    uncheckedTrackColor = SalatOutlineVariant,
                ),
            )
        }
    }
}

// ── Shared card wrapper ───────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = SalatSurfaceContainerLowest,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val PrayerType.displayName get() = when (this) {
    PrayerType.FAJR    -> "Fajr"
    PrayerType.DHUHR   -> "Dhuhr"
    PrayerType.ASR     -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA    -> "Isha"
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        uiState = SettingsUiState(
            isLoading    = false,
            displayName  = "Zaid Al-Farsi",
            email        = "zaid.alfarsi@example.com",
            useDeviceLocation = true,
            latitude     = "24.4672",
            longitude    = "39.6111",
            calculationMethod = CalculationMethod.MWL,
            reminders    = PrayerType.values().mapIndexed { i, t ->
                ReminderSetting(t, i != 4, listOf(0, -5, 0, 3, 0)[i])
            },
            use24HourFormat = false,
        ),
        onUseDeviceLocationChanged = {},
        onLatitudeChanged          = {},
        onLongitudeChanged         = {},
        onCalculationMethodChanged = {},
        onReminderEnabledChanged   = { _, _ -> },
        onReminderOffsetChanged    = { _, _ -> },
        on24HourFormatChanged      = {},
        onSave                     = {},
        onSignOutRequest           = {},
        onErrorDismiss             = {},
        onClearSaveSuccess         = {},
    )
}