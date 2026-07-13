package com.salattracker.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.salattracker.core.ui.label
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDailyTipsChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
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
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Location ──────────────────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_location))
            LocationSection(
                useDeviceLocation = uiState.useDeviceLocation,
                latitude          = uiState.latitude,
                longitude         = uiState.longitude,
                onToggleDevice    = onUseDeviceLocationChanged,
                onLatChanged      = onLatitudeChanged,
                onLonChanged      = onLongitudeChanged,
            )

            // ── Calculation method ────────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_prayer_calc))
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
                SettingsSectionLabel(stringResource(R.string.prayer_reminders), modifier = Modifier)
                Text(stringResource(R.string.settings_minutes_offset), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
            RemindersSection(
                reminders       = uiState.reminders,
                onToggle        = onReminderEnabledChanged,
                onOffsetChanged = onReminderOffsetChanged,
            )

            // ── Display ───────────────────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_display))
            DisplaySection(
                use24Hour   = uiState.use24HourFormat,
                onToggle    = on24HourFormatChanged,
            )

            // ── Appearance & language ─────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_appearance_language))
            AppearanceSection(
                language         = uiState.language,
                themeMode        = uiState.themeMode,
                dailyTipsEnabled = uiState.dailyTipsEnabled,
                onLanguageChanged = onLanguageChanged,
                onThemeChanged    = onThemeModeChanged,
                onDailyTipsChanged = onDailyTipsChanged,
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
                    Text(stringResource(R.string.settings_saving), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(painterResource(R.drawable.ic_save), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_save), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
                }
            }
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

        // ── Save success snackbar ─────────────────────────────────────────────
        if (uiState.saveSuccess) {
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onClearSaveSuccess) { Text(stringResource(R.string.action_ok), color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) { Text(stringResource(R.string.settings_saved), fontFamily = InterFamily) }
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
                label    = stringResource(R.string.settings_latitude),
                value    = latitude,
                onChanged = onLatChanged,
                enabled  = !useDeviceLocation,
                modifier = Modifier.weight(1f),
            )
            CoordField(
                label    = stringResource(R.string.settings_longitude),
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
    val focusManager = LocalFocusManager.current

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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
            Text(stringResource(R.string.settings_calc_method), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
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

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
private fun ReminderRow(
    setting: ReminderSetting,
    onToggle: (Boolean) -> Unit,
    onOffsetChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isNegative   = setting.offsetMinutes.startsWith("-")
    val magnitude    = setting.offsetMinutes.removePrefix("-")

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
            Column {
                Text(
                    text       = setting.prayerType.label(),
                    color      = if (setting.enabled) SalatPrimary else SalatOnSurfaceVariant,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFamily,
                )
                Text(
                    text       = setting.prayerTime?.format(timeFormatter) ?: "--:--",
                    color      = SalatOnSurfaceVariant,
                    fontSize   = 12.sp,
                    fontFamily = InterFamily,
                )
            }
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Soft keyboards' number pad has no "-" key, so the sign is a dedicated
            // toggle rather than something typed into the field.
            SignToggleButton(
                isNegative = isNegative,
                enabled    = setting.enabled,
                onClick    = {
                    val toggled = when {
                        isNegative                              -> magnitude
                        magnitude.isEmpty() || magnitude == "0"  -> magnitude
                        else                                     -> "-$magnitude"
                    }
                    onOffsetChanged(toggled)
                },
            )
            OutlinedTextField(
                value         = magnitude,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all(Char::isDigit)) {
                        onOffsetChanged((if (isNegative) "-" else "") + input)
                    }
                },
                enabled       = setting.enabled,
                modifier      = Modifier.width(56.dp),
                singleLine    = true,
                textStyle     = LocalTextStyle.current.copy(
                    fontFamily = InterFamily, fontSize = 14.sp,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
}

@Composable
private fun SignToggleButton(
    isNegative: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.size(32.dp),
        shape    = CircleShape,
        color    = if (isNegative) SalatPrimary else SalatSurfaceContainerLow,
        border   = BorderStroke(1.dp, SalatOutlineVariant.copy(alpha = if (enabled) 1f else 0.4f)),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text       = if (isNegative) "−" else "+",
                color      = if (isNegative) SalatOnPrimary else SalatOnSurfaceVariant,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFamily,
            )
        }
    }
}

// ── Appearance & language section ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(
    language: AppLanguage,
    themeMode: ThemeMode,
    dailyTipsEnabled: Boolean,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onDailyTipsChanged: (Boolean) -> Unit,
) {
    SettingsCard {
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

        // Daily spiritual tips
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.daily_spiritual_tips), color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
            Switch(
                checked         = dailyTipsEnabled,
                onCheckedChange = onDailyTipsChanged,
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
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
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
            Text(stringResource(R.string.settings_24hour), color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
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
            reminders    = PrayerType.values().mapIndexed { i, t ->
                ReminderSetting(
                    prayerType    = t,
                    enabled       = i != 4,
                    offsetMinutes = listOf(0, -5, 0, 3, 0)[i].toString(),
                    prayerTime    = listOf(
                        LocalTime.of(5, 42), LocalTime.of(12, 15), LocalTime.of(15, 30),
                        LocalTime.of(17, 58), LocalTime.of(19, 15),
                    )[i],
                )
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
        onLanguageChanged          = {},
        onThemeModeChanged         = {},
        onDailyTipsChanged         = {},
        onSave                     = {},
        onErrorDismiss             = {},
        onClearSaveSuccess         = {},
    )
}