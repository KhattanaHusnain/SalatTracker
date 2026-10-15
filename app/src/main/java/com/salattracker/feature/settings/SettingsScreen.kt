package com.salattracker.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.SettingsCard
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.ThemeMode

// ── Settings Screen — a hub of 3 cards, every control live-applies ──────────────

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUseDeviceLocationChanged: (Boolean) -> Unit,
    onLatitudeChanged: (String) -> Unit,
    onLongitudeChanged: (String) -> Unit,
    onLocationCommit: () -> Unit,
    onCalculationMethodSheetOpen: () -> Unit,
    onCalculationMethodSheetDismiss: () -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    on24HourFormatChanged: (Boolean) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onErrorDismiss: () -> Unit,
    onPrayerRemindersClick: () -> Unit,
    onBackupClick: () -> Unit,
    backupNeedsAttention: Boolean,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Location & Prayer Times ──────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_location_prayer_times))
            LocationAndPrayerTimesCard(
                useDeviceLocation = uiState.useDeviceLocation,
                latitude          = uiState.latitude,
                longitude         = uiState.longitude,
                calculationMethod = uiState.calculationMethod,
                onToggleDevice    = onUseDeviceLocationChanged,
                onLatChanged      = onLatitudeChanged,
                onLonChanged      = onLongitudeChanged,
                onLocationCommit  = onLocationCommit,
                onCalculationMethodClick = onCalculationMethodSheetOpen,
            )

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_appearance))
            AppearanceCard(
                language          = uiState.language,
                themeMode         = uiState.themeMode,
                use24Hour         = uiState.use24HourFormat,
                onLanguageChanged = onLanguageChanged,
                onThemeChanged    = onThemeModeChanged,
                on24HourChanged   = on24HourFormatChanged,
            )

            // ── Notifications & Data ──────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_notifications_data))
            NotificationsAndDataCard(
                onPrayerRemindersClick = onPrayerRemindersClick,
                onBackupClick           = onBackupClick,
                backupNeedsAttention    = backupNeedsAttention,
            )
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }
    }

    if (uiState.showCalculationMethodSheet) {
        CalculationMethodSheet(
            selected  = uiState.calculationMethod,
            onSelect  = onCalculationMethodChanged,
            onDismiss = onCalculationMethodSheetDismiss,
        )
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier.fillMaxWidth(),
        color         = SalatPrimaryContainer,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        fontFamily    = InterFamily,
        letterSpacing = 1.5.sp,
    )
}

// ── Location & Prayer Times card ──────────────────────────────────────────────

@Composable
private fun LocationAndPrayerTimesCard(
    useDeviceLocation: Boolean,
    latitude: String,
    longitude: String,
    calculationMethod: CalculationMethod,
    onToggleDevice: (Boolean) -> Unit,
    onLatChanged: (String) -> Unit,
    onLonChanged: (String) -> Unit,
    onLocationCommit: () -> Unit,
    onCalculationMethodClick: () -> Unit,
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
                Text(stringResource(R.string.settings_device_location), color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
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
                label     = stringResource(R.string.settings_latitude),
                value     = latitude,
                onChanged = onLatChanged,
                onCommit  = onLocationCommit,
                enabled   = !useDeviceLocation,
                modifier  = Modifier.weight(1f),
            )
            CoordField(
                label     = stringResource(R.string.settings_longitude),
                value     = longitude,
                onChanged = onLonChanged,
                onCommit  = onLocationCommit,
                enabled   = !useDeviceLocation,
                modifier  = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // Calculation method — opens a bottom sheet (23 entries don't fit a dropdown)
        SettingsNavRow(
            icon    = R.drawable.ic_history_edu,
            label   = stringResource(R.string.settings_calc_method),
            onClick = onCalculationMethodClick,
        )
        Text(
            text     = calculationMethod.displayName,
            modifier = Modifier.padding(start = 36.dp, top = 2.dp),
            color    = SalatOnSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = InterFamily,
            maxLines = 1,
        )
    }
}

@Composable
private fun CoordField(
    label: String,
    value: String,
    onChanged: (String) -> Unit,
    onCommit: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onChanged,
            enabled       = enabled,
            modifier      = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) onCommit() },
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(
                fontFamily = InterFamily, fontSize = 14.sp, color = SalatOnSurface,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onCommit() }),
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

// ── Appearance card (theme, language, 24-hour) ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(
    language: AppLanguage,
    themeMode: ThemeMode,
    use24Hour: Boolean,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    on24HourChanged: (Boolean) -> Unit,
) {
    SettingsCard {
        // Theme
        Text(stringResource(R.string.settings_theme), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { mode ->
                ThemeModeChip(
                    label      = stringResource(mode.themeLabelRes),
                    isSelected = mode == themeMode,
                    onClick    = { onThemeChanged(mode) },
                    modifier   = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))

        // Language
        Text(stringResource(R.string.settings_language), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(Modifier.height(4.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value         = language.displayName,
                onValueChange = {},
                readOnly      = true,
                modifier      = Modifier.fillMaxWidth().menuAnchor(),
                singleLine    = true,
                leadingIcon   = { Icon(painterResource(R.drawable.ic_language), null, tint = SalatPrimary, modifier = Modifier.size(20.dp)) },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                textStyle     = LocalTextStyle.current.copy(fontFamily = InterFamily, fontSize = 14.sp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = SalatPrimary,
                    unfocusedBorderColor    = SalatOutlineVariant,
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
                AppLanguage.values().forEach { lang ->
                    DropdownMenuItem(
                        text    = { Text(lang.displayName, fontFamily = InterFamily, fontSize = 14.sp, color = SalatOnSurface) },
                        onClick = { onLanguageChanged(lang); expanded = false },
                        colors  = MenuDefaults.itemColors(
                            textColor = if (lang == language) SalatPrimary else SalatOnSurface,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))

        // 24-hour format
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_24hour), color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
            Switch(
                checked         = use24Hour,
                onCheckedChange = on24HourChanged,
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

@Composable
private fun ThemeModeChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = if (isSelected) SalatPrimary else SalatSurfaceContainerLow,
        border   = BorderStroke(1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Text(
            text       = label,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign  = TextAlign.Center,
            color      = if (isSelected) SalatOnPrimary else SalatOnSurfaceVariant,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
        )
    }
}

private val ThemeMode.themeLabelRes get() = when (this) {
    ThemeMode.LIGHT  -> R.string.theme_light
    ThemeMode.DARK   -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

// ── Notifications & Data card (navigation rows into Reminders / Backup) ──────

@Composable
private fun NotificationsAndDataCard(
    onPrayerRemindersClick: () -> Unit,
    onBackupClick: () -> Unit,
    backupNeedsAttention: Boolean,
) {
    SettingsCard {
        SettingsNavRow(
            icon    = R.drawable.ic_notifications,
            label   = stringResource(R.string.prayer_reminders),
            onClick = onPrayerRemindersClick,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            icon          = R.drawable.ic_cloud_upload,
            label         = stringResource(R.string.backup_title),
            onClick       = onBackupClick,
            trailingText  = if (backupNeedsAttention) stringResource(R.string.backup_action_needed) else null,
            trailingColor = SalatError,
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        uiState = SettingsUiState(
            isLoading    = false,
            useDeviceLocation = true,
            latitude     = "24.4672",
            longitude    = "39.6111",
            calculationMethod = CalculationMethod.MWL,
            use24HourFormat = false,
        ),
        onUseDeviceLocationChanged = {},
        onLatitudeChanged          = {},
        onLongitudeChanged         = {},
        onLocationCommit           = {},
        onCalculationMethodSheetOpen    = {},
        onCalculationMethodSheetDismiss = {},
        onCalculationMethodChanged = {},
        on24HourFormatChanged      = {},
        onLanguageChanged          = {},
        onThemeModeChanged         = {},
        onErrorDismiss             = {},
        onPrayerRemindersClick     = {},
        onBackupClick              = {},
        backupNeedsAttention       = true,
    )
}
