package com.salattracker.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.SettingsCard
import com.salattracker.core.ui.SettingsSectionLabel
import com.salattracker.core.ui.label
import com.salattracker.core.ui.salatSwitchColors
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode

// ── Settings Screen — a hub of 4 cards, every control live-applies ──────────────

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUseDeviceLocationChanged: (Boolean) -> Unit,
    onEditLocationClick: () -> Unit,
    onCalculationMethodSheetOpen: () -> Unit,
    onCalculationMethodSheetDismiss: () -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onMadhabChanged: (Madhab) -> Unit,
    onHighLatitudeRuleChanged: (HighLatitudeRule) -> Unit,
    onPrayerTimeAdjustChanged: (PrayerType, Int) -> Unit,
    on24HourFormatChanged: (Boolean) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onGenderChanged: (Gender) -> Unit,
    onErrorDismiss: () -> Unit,
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
                locationLabel     = uiState.locationLabel,
                calculationMethod = uiState.calculationMethod,
                madhab            = uiState.madhab,
                highLatitudeRule  = uiState.highLatitudeRule,
                onToggleDevice    = onUseDeviceLocationChanged,
                onEditLocationClick = onEditLocationClick,
                onCalculationMethodClick = onCalculationMethodSheetOpen,
                onMadhabChanged   = onMadhabChanged,
                onHighLatitudeRuleChanged = onHighLatitudeRuleChanged,
            )

            // ── Adjust Prayer Times ──────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_adjust_prayer_times))
            AdjustPrayerTimesCard(
                adjustMinutes = uiState.prayerTimeAdjustMinutes,
                onChanged = onPrayerTimeAdjustChanged,
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

            // ── About You ─────────────────────────────────────────────────────
            SettingsSectionLabel(stringResource(R.string.settings_about_you_section))
            AboutYouCard(gender = uiState.gender, onGenderChanged = onGenderChanged)
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
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

// ── Location & Prayer Times card ──────────────────────────────────────────────

@Composable
private fun LocationAndPrayerTimesCard(
    useDeviceLocation: Boolean,
    latitude: Double,
    longitude: Double,
    locationLabel: String?,
    calculationMethod: CalculationMethod,
    madhab: Madhab,
    highLatitudeRule: HighLatitudeRule,
    onToggleDevice: (Boolean) -> Unit,
    onEditLocationClick: () -> Unit,
    onCalculationMethodClick: () -> Unit,
    onMadhabChanged: (Madhab) -> Unit,
    onHighLatitudeRuleChanged: (HighLatitudeRule) -> Unit,
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
                Text(stringResource(R.string.settings_device_location), color = SalatOnSurface, fontSize = 16.sp, fontFamily = AppUiFontFamily)
            }
            Switch(
                checked         = useDeviceLocation,
                onCheckedChange = onToggleDevice,
                colors          = salatSwitchColors(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // Location — a single tappable row; editing coordinates/searching a city is its own
        // screen (Routes.EDIT_COORDINATES) rather than inline fields here.
        SettingsNavRow(
            icon    = R.drawable.ic_my_location,
            label   = stringResource(R.string.onboarding_location_label),
            onClick = onEditLocationClick,
        )
        Text(
            text     = locationLabel ?: stringResource(R.string.settings_coordinates_value, latitude, longitude),
            modifier = Modifier.padding(start = 36.dp, top = 2.dp),
            color    = SalatOnSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = AppUiFontFamily,
            maxLines = 1,
        )

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
            fontFamily = AppUiFontFamily,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // School of thought — changes Asr, and for Jafari also Isha.
        Text(stringResource(R.string.settings_school), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(8.dp))
        SchoolDropdown(selected = madhab, onSelect = onMadhabChanged)

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // High-latitude rule — bounds Fajr/Isha where the method's angle is never reached.
        Text(stringResource(R.string.settings_high_latitude), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.settings_high_latitude_desc), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
        HighLatitudeRuleDropdown(selected = highLatitudeRule, onSelect = onHighLatitudeRuleChanged)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighLatitudeRuleDropdown(selected: HighLatitudeRule, onSelect: (HighLatitudeRule) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            singleLine    = true,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle     = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 14.sp),
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
            HighLatitudeRule.values().forEach { rule ->
                DropdownMenuItem(
                    text    = { Text(stringResource(rule.labelRes), fontFamily = AppUiFontFamily, fontSize = 14.sp, color = SalatOnSurface) },
                    onClick = { onSelect(rule); expanded = false },
                    colors  = MenuDefaults.itemColors(
                        textColor = if (rule == selected) SalatPrimary else SalatOnSurface,
                    ),
                )
            }
        }
    }
}

private val HighLatitudeRule.labelRes get() = when (this) {
    HighLatitudeRule.AUTOMATIC            -> R.string.high_latitude_automatic
    HighLatitudeRule.MIDDLE_OF_THE_NIGHT  -> R.string.high_latitude_middle
    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> R.string.high_latitude_seventh
    HighLatitudeRule.TWILIGHT_ANGLE       -> R.string.high_latitude_twilight
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolDropdown(selected: Madhab, onSelect: (Madhab) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            singleLine    = true,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle     = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 14.sp),
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
            Madhab.values().forEach { school ->
                DropdownMenuItem(
                    text    = { Text(stringResource(school.labelRes), fontFamily = AppUiFontFamily, fontSize = 14.sp, color = SalatOnSurface) },
                    onClick = { onSelect(school); expanded = false },
                    colors  = MenuDefaults.itemColors(
                        textColor = if (school == selected) SalatPrimary else SalatOnSurface,
                    ),
                )
            }
        }
    }
}

private val Madhab.labelRes get() = when (this) {
    Madhab.STANDARD -> R.string.school_standard_title
    Madhab.HANAFI   -> R.string.school_hanafi_title
    Madhab.JAFARI   -> R.string.school_jafari_title
}

// ── Adjust Prayer Times card ──────────────────────────────────────────────────

@Composable
private fun AdjustPrayerTimesCard(
    adjustMinutes: Map<PrayerType, Int>,
    onChanged: (PrayerType, Int) -> Unit,
) {
    SettingsCard {
        Text(
            stringResource(R.string.settings_adjust_prayer_times_desc),
            color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily, lineHeight = 16.sp,
        )
        Spacer(Modifier.height(12.dp))
        PrayerType.values().forEachIndexed { index, type ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(type.label(), color = SalatOnSurface, fontSize = 15.sp, fontFamily = AppUiFontFamily)
                MinuteStepper(
                    minutes = adjustMinutes[type] ?: 0,
                    onMinutesChanged = { onChanged(type, it) },
                )
            }
            if (index != PrayerType.values().lastIndex) {
                Spacer(Modifier.height(10.dp))
            }
        }
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
        Text(stringResource(R.string.settings_theme), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
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
        Text(stringResource(R.string.settings_language), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
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
                textStyle     = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 14.sp),
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
                        text    = { Text(lang.displayName, fontFamily = AppUiFontFamily, fontSize = 14.sp, color = SalatOnSurface) },
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
            Text(stringResource(R.string.settings_24hour), color = SalatOnSurface, fontSize = 16.sp, fontFamily = AppUiFontFamily)
            Switch(
                checked         = use24Hour,
                onCheckedChange = on24HourChanged,
                colors          = salatSwitchColors(),
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
            fontFamily = AppUiFontFamily,
        )
    }
}

private val ThemeMode.themeLabelRes get() = when (this) {
    ThemeMode.LIGHT  -> R.string.theme_light
    ThemeMode.DARK   -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

// ── About You card (onboarding's gender question, editable afterward) ─────────

@Composable
private fun AboutYouCard(gender: Gender?, onGenderChanged: (Gender) -> Unit) {
    SettingsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeModeChip(
                label      = stringResource(R.string.onboarding_gender_male),
                isSelected = gender == Gender.MALE,
                onClick    = { onGenderChanged(Gender.MALE) },
                modifier   = Modifier.weight(1f),
            )
            ThemeModeChip(
                label      = stringResource(R.string.onboarding_gender_female),
                isSelected = gender == Gender.FEMALE,
                onClick    = { onGenderChanged(Gender.FEMALE) },
                modifier   = Modifier.weight(1f),
            )
        }
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
            latitude     = 24.4672,
            longitude    = 39.6111,
            calculationMethod = CalculationMethod.MWL,
            use24HourFormat = false,
        ),
        onUseDeviceLocationChanged = {},
        onEditLocationClick        = {},
        onCalculationMethodSheetOpen    = {},
        onCalculationMethodSheetDismiss = {},
        onCalculationMethodChanged = {},
        onMadhabChanged            = {},
        onHighLatitudeRuleChanged  = {},
        onPrayerTimeAdjustChanged  = { _, _ -> },
        on24HourFormatChanged      = {},
        onLanguageChanged          = {},
        onThemeModeChanged         = {},
        onGenderChanged            = {},
        onErrorDismiss             = {},
    )
}
