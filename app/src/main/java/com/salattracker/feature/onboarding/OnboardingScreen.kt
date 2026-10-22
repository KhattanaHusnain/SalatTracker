package com.salattracker.feature.onboarding

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.usecase.toHijriOrNull
import com.salattracker.core.ui.CitySearchField
import com.salattracker.core.ui.CitySearchResults
import com.salattracker.core.ui.DatePickerModal
import com.salattracker.feature.settings.CalculationMethodSheet
import java.time.LocalDate

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onWelcomeNext: () -> Unit,
    onRestoreFromBackup: () -> Unit,
    onGenderSelected: (Gender) -> Unit,
    onAboutYouNext: () -> Unit,
    onMadhabSelected: (Madhab) -> Unit,
    onSchoolNext: () -> Unit,
    onAllowLocationClick: () -> Unit,
    onEnterCityInstead: () -> Unit,
    onCityQueryChanged: (String) -> Unit,
    onCitySelected: (CityResult) -> Unit,
    onCitySearchContinue: () -> Unit,
    onUseMyLocationInstead: () -> Unit,
    onConfirmLocation: () -> Unit,
    onChangeLocationClick: () -> Unit,
    onCalculationMethodSheetOpen: () -> Unit,
    onCalculationMethodSheetDismiss: () -> Unit,
    onCalculationMethodSelected: (CalculationMethod) -> Unit,
    onPrayerRemindersChanged: (Boolean) -> Unit,
    onFullScreenAlertChanged: (Boolean) -> Unit,
    onRemindersSkip: () -> Unit,
    onRemindersNext: () -> Unit,
    onCatchUpAddClick: () -> Unit,
    onCatchUpSkip: () -> Unit,
    onCatchUpDurationChanged: (CatchUpDuration) -> Unit,
    onCatchUpCustomRangeChanged: (LocalDate, LocalDate) -> Unit,
    onApplyCatchUp: () -> Unit,
    onFinish: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SalatBackground)
            .padding(horizontal = 24.dp),
    ) {
        when (uiState.step) {
            OnboardingStep.WELCOME -> WelcomeStep(uiState.language, uiState.themeMode, onLanguageSelected, onThemeSelected, onWelcomeNext, onRestoreFromBackup)
            OnboardingStep.ABOUT_YOU -> AboutYouStep(uiState.gender, onGenderSelected, onAboutYouNext)
            OnboardingStep.SCHOOL -> SchoolStep(uiState.madhab, onMadhabSelected, onSchoolNext)
            OnboardingStep.LOCATION_INTRO -> LocationIntroStep(onAllowLocationClick, onEnterCityInstead)
            OnboardingStep.CITY_SEARCH -> CitySearchStep(
                query = uiState.cityQuery, results = uiState.cityResults, isSearching = uiState.isSearchingCity,
                selected = uiState.confirmedCity, onQueryChanged = onCityQueryChanged, onSelect = onCitySelected,
                onContinue = onCitySearchContinue, onUseMyLocationInstead = onUseMyLocationInstead,
            )
            OnboardingStep.LOCATION_CONFIRM -> LocationConfirmStep(
                city = uiState.confirmedCity, calculationMethod = uiState.calculationMethod,
                showCalculationMethodSheet = uiState.showCalculationMethodSheet,
                onConfirm = onConfirmLocation, onChangeLocation = onChangeLocationClick,
                onCalculationMethodClick = onCalculationMethodSheetOpen,
                onCalculationMethodDismiss = onCalculationMethodSheetDismiss,
                onCalculationMethodSelected = onCalculationMethodSelected,
            )
            OnboardingStep.REMINDERS -> RemindersStep(
                prayerRemindersEnabled = uiState.prayerRemindersEnabled, fullScreenAlertEnabled = uiState.fullScreenAlertEnabled,
                onPrayerRemindersChanged = onPrayerRemindersChanged, onFullScreenAlertChanged = onFullScreenAlertChanged,
                onNext = onRemindersNext, onSkip = onRemindersSkip,
            )
            OnboardingStep.CATCH_UP -> CatchUpStep(onAddMissedClick = onCatchUpAddClick, onSkip = onCatchUpSkip)
            OnboardingStep.CATCH_UP_ADD -> CatchUpAddStep(
                duration = uiState.catchUpDuration, prayerCount = uiState.catchUpPrayerCount,
                customFrom = uiState.catchUpCustomFrom, customTo = uiState.catchUpCustomTo,
                onDurationChanged = onCatchUpDurationChanged, onCustomRangeChanged = onCatchUpCustomRangeChanged,
                onApply = onApplyCatchUp,
            )
            OnboardingStep.DONE -> DoneStep(uiState, onFinish)
        }
    }
}

// ── Welcome (language + theme) ─────────────────────────────────────────────────

@Composable
private fun WelcomeStep(
    language: AppLanguage,
    themeMode: ThemeMode,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onContinue: () -> Unit,
    onRestoreFromBackup: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(48.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(SalatPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_mosque), null, tint = SalatOnPrimary, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.onboarding_welcome_title), color = SalatPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onboarding_welcome_subtitle), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))

        Text(stringResource(R.string.settings_language), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.values().forEach { lang ->
                SelectableRow(icon = R.drawable.ic_language, label = lang.displayName, isSelected = lang == language, onClick = { onLanguageSelected(lang) })
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.onboarding_theme_title), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(R.drawable.ic_light_mode, stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) { onThemeSelected(ThemeMode.LIGHT) }
            ThemeChip(R.drawable.ic_dark_mode, stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK, Modifier.weight(1f)) { onThemeSelected(ThemeMode.DARK) }
            ThemeChip(R.drawable.ic_contrast, stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM, Modifier.weight(1f)) { onThemeSelected(ThemeMode.SYSTEM) }
        }

        Spacer(Modifier.height(28.dp))

        PrimaryButton(text = stringResource(R.string.action_continue), onClick = onContinue)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRestoreFromBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_cloud_upload), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.backup_onboarding_restore_action), color = SalatPrimary, fontSize = 13.sp, fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_change_later),
            modifier = Modifier.fillMaxWidth(),
            color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeChip(icon: Int, label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SalatPrimary else SalatSurfaceContainerLowest,
        border = BorderStroke(1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(icon), null, tint = if (isSelected) SalatOnPrimary else SalatPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (isSelected) SalatOnPrimary else SalatOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
    }
}

// ── About you (gender) ──────────────────────────────────────────────────────────

@Composable
private fun AboutYouStep(selected: Gender?, onSelect: (Gender) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(stepIndex = OnboardingStep.ABOUT_YOU.ordinal, title = stringResource(R.string.onboarding_about_you_title), subtitle = stringResource(R.string.onboarding_about_you_subtitle))
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TitledOption(R.drawable.ic_person, stringResource(R.string.onboarding_gender_male), stringResource(R.string.onboarding_gender_male_desc), selected == Gender.MALE) { onSelect(Gender.MALE) }
            TitledOption(R.drawable.ic_female, stringResource(R.string.onboarding_gender_female), stringResource(R.string.onboarding_gender_female_desc), selected == Gender.FEMALE) { onSelect(Gender.FEMALE) }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_continue), enabled = selected != null, onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

// ── School of thought (Asr calculation) ─────────────────────────────────────────

@Composable
private fun SchoolStep(selected: Madhab, onSelect: (Madhab) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(stepIndex = OnboardingStep.SCHOOL.ordinal, title = stringResource(R.string.onboarding_school_title), subtitle = stringResource(R.string.onboarding_school_subtitle))
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_standard_title), stringResource(R.string.school_standard_desc), selected == Madhab.STANDARD) { onSelect(Madhab.STANDARD) }
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_hanafi_title), stringResource(R.string.school_hanafi_desc), selected == Madhab.HANAFI) { onSelect(Madhab.HANAFI) }
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_jafari_title), stringResource(R.string.school_jafari_desc), selected == Madhab.JAFARI) { onSelect(Madhab.JAFARI) }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_next), onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

// ── Location: before permission ─────────────────────────────────────────────────

@Composable
private fun LocationIntroStep(onAllowLocation: () -> Unit, onEnterCityInstead: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(SalatSecondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_my_location), null, tint = SalatPrimary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onboarding_location_title), color = SalatPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_location_desc),
            color = SalatOnSurfaceVariant, fontSize = 15.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center, lineHeight = 22.sp,
        )
        Spacer(Modifier.height(36.dp))
        PrimaryButton(text = stringResource(R.string.onboarding_allow_location), icon = R.drawable.ic_my_location, onClick = onAllowLocation)
        Spacer(Modifier.height(8.dp))
        SecondaryLink(text = stringResource(R.string.onboarding_location_enter_city), onClick = onEnterCityInstead)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.onboarding_location_privacy), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── City search ───────────────────────────────────────────────────────────────

@Composable
private fun CitySearchStep(
    query: String,
    results: List<CityResult>,
    isSearching: Boolean,
    selected: CityResult?,
    onQueryChanged: (String) -> Unit,
    onSelect: (CityResult) -> Unit,
    onContinue: () -> Unit,
    onUseMyLocationInstead: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(stepIndex = OnboardingStep.LOCATION_CONFIRM.ordinal, title = stringResource(R.string.onboarding_city_search_title), subtitle = null)
        Spacer(Modifier.height(20.dp))

        CitySearchField(query = query, onQueryChanged = onQueryChanged)

        Spacer(Modifier.height(16.dp))

        CitySearchResults(query = query, results = results, isSearching = isSearching, selected = selected, onSelect = onSelect)

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_continue), enabled = selected != null, onClick = onContinue)
        Spacer(Modifier.height(8.dp))
        SecondaryLink(text = stringResource(R.string.onboarding_location_use_my_location), onClick = onUseMyLocationInstead)
        Spacer(Modifier.height(24.dp))
    }
}

// ── Location: after permission / after city chosen (confirm) ───────────────────

@Composable
private fun LocationConfirmStep(
    city: CityResult?,
    calculationMethod: CalculationMethod,
    showCalculationMethodSheet: Boolean,
    onConfirm: () -> Unit,
    onChangeLocation: () -> Unit,
    onCalculationMethodClick: () -> Unit,
    onCalculationMethodDismiss: () -> Unit,
    onCalculationMethodSelected: (CalculationMethod) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val hijri = remember(today) { today.toHijriOrNull() }

    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(stepIndex = OnboardingStep.LOCATION_CONFIRM.ordinal, title = stringResource(R.string.onboarding_location_confirm_title), subtitle = stringResource(R.string.onboarding_location_confirm_subtitle))
        Spacer(Modifier.height(28.dp))

        SummaryRow(
            icon = R.drawable.ic_my_location,
            label = city?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.onboarding_location_label),
            value = city?.displayLine.orEmpty(),
            trailing = { SecondaryLink(stringResource(R.string.onboarding_location_change), onChangeLocation) },
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        SummaryRow(
            icon = R.drawable.ic_history_edu,
            label = stringResource(R.string.settings_calc_method),
            value = calculationMethod.displayName,
            trailing = { SecondaryLink(stringResource(R.string.onboarding_location_change), onCalculationMethodClick) },
        )
        if (hijri != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            SummaryRow(
                icon = R.drawable.ic_calendar_today,
                label = stringResource(R.string.onboarding_hijri_date_label),
                value = "${hijri.day}/${hijri.month}/${hijri.year} AH",
                trailing = null,
            )
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.onboarding_location_confirm_action), enabled = city != null, onClick = onConfirm)
        Spacer(Modifier.height(24.dp))
    }

    if (showCalculationMethodSheet) {
        CalculationMethodSheet(selected = calculationMethod, onSelect = onCalculationMethodSelected, onDismiss = onCalculationMethodDismiss)
    }
}

@Composable
private fun SummaryRow(icon: Int, label: String, value: String, trailing: (@Composable () -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(label, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                if (value.isNotBlank()) {
                    Text(value, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                }
            }
        }
        trailing?.invoke()
    }
}

// ── Reminders ────────────────────────────────────────────────────────────────

@Composable
private fun RemindersStep(
    prayerRemindersEnabled: Boolean,
    fullScreenAlertEnabled: Boolean,
    onPrayerRemindersChanged: (Boolean) -> Unit,
    onFullScreenAlertChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(40.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(SalatPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_notifications), null, tint = SalatOnPrimary, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.onboarding_notifications_title), color = SalatPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onboarding_notifications_subtitle), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))

        ToggleRow(icon = R.drawable.ic_schedule, label = stringResource(R.string.prayer_reminders), checked = prayerRemindersEnabled, onCheckedChange = onPrayerRemindersChanged)
        Spacer(Modifier.height(10.dp))
        ToggleRow(icon = R.drawable.ic_notifications, label = stringResource(R.string.onboarding_reminders_fullscreen_title), checked = fullScreenAlertEnabled, onCheckedChange = onFullScreenAlertChanged)
        Spacer(Modifier.height(10.dp))
        AzanPreviewRow()

        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = stringResource(R.string.onboarding_reminders_turn_on), icon = R.drawable.ic_arrow_forward, onClick = onNext)
        Spacer(Modifier.height(8.dp))
        SecondaryLink(text = stringResource(R.string.action_skip_for_now), onClick = onSkip)
        Spacer(Modifier.height(24.dp))
    }
}

/** Previews the app's default reminder sound (Azan) — the one sound onboarding offers; picking a
 * different per-prayer sound stays a Prayer Reminders / Settings thing, done after onboarding.
 * Uses the same explicit notification-usage [AudioAttributes] as
 * feature/reminders's SoundChooserDialog, so the preview matches what the real reminder sounds
 * like (and still respects ringer mode) rather than defaulting to the media/music stream. */
@Composable
private fun AzanPreviewRow() {
    val context = LocalContext.current
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        runCatching { activePlayer?.stop() }
        runCatching { activePlayer?.release() }
        activePlayer = null
    }

    DisposableEffect(Unit) { onDispose { stopPreview() } }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SalatSurfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_notifications), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Text(stringResource(R.string.reminders_sound_azan), color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
            }
            TextButton(onClick = {
                stopPreview()
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val player = MediaPlayer.create(context, R.raw.azan, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
                player?.setOnCompletionListener { mp -> runCatching { mp.release() } }
                activePlayer = player
                player?.start()
            }) {
                Text(stringResource(R.string.reminders_sound_preview), color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
        }
    }
}

// ── Catch up (optional) ─────────────────────────────────────────────────────────

@Composable
private fun CatchUpStep(onAddMissedClick: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(stepIndex = OnboardingStep.CATCH_UP.ordinal, title = stringResource(R.string.onboarding_catch_up_title), subtitle = stringResource(R.string.onboarding_catch_up_subtitle))
        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SalatSurfaceContainerLowest).padding(16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_pending_actions), null, tint = SalatTertiary, modifier = Modifier.size(28.dp))
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_continue), icon = R.drawable.ic_pending_actions, onClick = onAddMissedClick)
        Spacer(Modifier.height(8.dp))
        SecondaryLink(text = stringResource(R.string.action_skip_for_now), onClick = onSkip)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CatchUpAddStep(
    duration: CatchUpDuration,
    prayerCount: Int,
    customFrom: LocalDate,
    customTo: LocalDate,
    onDurationChanged: (CatchUpDuration) -> Unit,
    onCustomRangeChanged: (LocalDate, LocalDate) -> Unit,
    onApply: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StepHeader(stepIndex = OnboardingStep.CATCH_UP.ordinal, title = stringResource(R.string.onboarding_catch_up_add_title), subtitle = stringResource(R.string.onboarding_catch_up_add_subtitle))
        Spacer(Modifier.height(28.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatchUpDurationOption(CatchUpDuration.WEEK, duration, onDurationChanged)
            CatchUpDurationOption(CatchUpDuration.MONTH, duration, onDurationChanged)
            CatchUpDurationOption(CatchUpDuration.HALF_YEAR, duration, onDurationChanged)
            CatchUpDurationOption(CatchUpDuration.CUSTOM, duration, onDurationChanged)

            if (duration == CatchUpDuration.CUSTOM) {
                CustomRangePickers(from = customFrom, to = customTo, onChanged = onCustomRangeChanged)
            }
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = stringResource(R.string.onboarding_catch_up_add_action, prayerCount), onClick = onApply)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CatchUpDurationOption(option: CatchUpDuration, selected: CatchUpDuration, onSelect: (CatchUpDuration) -> Unit) {
    val titleRes = when (option) {
        CatchUpDuration.WEEK -> R.string.onboarding_catch_up_duration_week
        CatchUpDuration.MONTH -> R.string.onboarding_catch_up_duration_month
        CatchUpDuration.HALF_YEAR -> R.string.onboarding_catch_up_duration_half_year
        CatchUpDuration.CUSTOM -> R.string.bulk_dur_custom
    }
    val subtitle = if (option == CatchUpDuration.CUSTOM) {
        null
    } else {
        stringResource(R.string.onboarding_catch_up_prayer_count, option.days * 5)
    }
    TitledOption(
        icon = R.drawable.ic_pending_actions,
        title = stringResource(titleRes),
        subtitle = subtitle,
        isSelected = option == selected,
        onClick = { onSelect(option) },
    )
}

@Composable
private fun CustomRangePickers(from: LocalDate, to: LocalDate, onChanged: (LocalDate, LocalDate) -> Unit) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_calendar_today), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bulk_start, from.toString()), fontFamily = AppUiFontFamily, fontSize = 12.sp, color = SalatOnSurface)
        }
        OutlinedButton(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_calendar_today), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bulk_end, to.toString()), fontFamily = AppUiFontFamily, fontSize = 12.sp, color = SalatOnSurface)
        }
    }

    if (showFromPicker) {
        DatePickerModal(initialDate = from, onDatePicked = { picked -> showFromPicker = false; onChanged(picked, to) }, onDismiss = { showFromPicker = false })
    }
    if (showToPicker) {
        DatePickerModal(initialDate = to, onDatePicked = { picked -> showToPicker = false; onChanged(from, picked) }, onDismiss = { showToPicker = false })
    }
}

// ── Done ─────────────────────────────────────────────────────────────────────

@Composable
private fun DoneStep(uiState: OnboardingUiState, onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(48.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(SalatPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.onboarding_done_title), color = SalatPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onboarding_done_subtitle), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = SalatSurfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DoneSummaryLine(stringResource(R.string.onboarding_done_summary_language), uiState.language.displayName)
                DoneSummaryLine(stringResource(R.string.onboarding_location_label), uiState.confirmedCity?.name?.takeIf { it.isNotBlank() } ?: uiState.calculationMethod.displayName)
                DoneSummaryLine(
                    stringResource(R.string.onboarding_done_summary_reminders),
                    stringResource(if (uiState.prayerRemindersEnabled) R.string.onboarding_done_reminders_on else R.string.onboarding_done_reminders_off),
                )
                DoneSummaryLine(
                    stringResource(R.string.onboarding_done_summary_qaza),
                    stringResource(if (uiState.catchUpAppliedCount != null) R.string.onboarding_done_qaza_added else R.string.onboarding_done_qaza_set_up_later),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.onboarding_done_action), icon = R.drawable.ic_arrow_forward, onClick = onFinish, enabled = !uiState.isFinishing)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DoneSummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
        Text(value, color = SalatOnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
    }
}

// ── Shared pieces ──────────────────────────────────────────────────────────────

/** Title + subtitle + progress dots used by every mid-flow step (not Welcome or Done, which have
 * their own centered hero layout). [stepIndex] is the 0-based [OnboardingStep] ordinal, excluding
 * [OnboardingStep.WELCOME] and [OnboardingStep.DONE] from the dot count. */
@Composable
private fun StepHeader(stepIndex: Int, title: String, subtitle: String?) {
    val total = OnboardingStep.values().size - 2
    Spacer(Modifier.height(48.dp))
    StepDots(current = (stepIndex - 1).coerceIn(0, total - 1), total = total)
    Spacer(Modifier.height(24.dp))
    Text(title, color = SalatPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun TitledOption(icon: Int, title: String, subtitle: String?, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = if (isSelected) SalatSecondaryContainer.copy(alpha = 0.4f) else SalatSurfaceContainerLowest,
        border   = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SalatSurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, tint = SalatPrimary, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
                if (subtitle != null) {
                    Text(subtitle, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                }
            }
            SelectionIndicator(isSelected)
        }
    }
}

@Composable
private fun SelectableRow(icon: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Text(label, color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f))
            SelectionIndicator(isSelected)
        }
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    if (isSelected) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(SalatPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnPrimary, modifier = Modifier.size(16.dp))
        }
    } else {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SalatSurfaceContainerHigh)) {}
    }
}

@Composable
private fun ToggleRow(icon: Int, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLow,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Text(label, color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onCheckedChange,
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
private fun StepDots(current: Int, total: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                val active = i == current
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (active) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) SalatPrimary else SalatOutlineVariant),
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, icon: Int? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
    ) {
        Text(text, fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(icon), null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SecondaryLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingWelcomePreview() {
    OnboardingScreen(
        uiState = OnboardingUiState(step = OnboardingStep.WELCOME),
        onLanguageSelected = {}, onThemeSelected = {}, onWelcomeNext = {}, onRestoreFromBackup = {},
        onGenderSelected = {}, onAboutYouNext = {}, onMadhabSelected = {}, onSchoolNext = {},
        onAllowLocationClick = {}, onEnterCityInstead = {}, onCityQueryChanged = {}, onCitySelected = {},
        onCitySearchContinue = {}, onUseMyLocationInstead = {}, onConfirmLocation = {}, onChangeLocationClick = {},
        onCalculationMethodSheetOpen = {}, onCalculationMethodSheetDismiss = {}, onCalculationMethodSelected = {},
        onPrayerRemindersChanged = {}, onFullScreenAlertChanged = {}, onRemindersSkip = {}, onRemindersNext = {},
        onCatchUpAddClick = {}, onCatchUpSkip = {}, onCatchUpDurationChanged = {}, onCatchUpCustomRangeChanged = { _, _ -> },
        onApplyCatchUp = {}, onFinish = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingDonePreview() {
    OnboardingScreen(
        uiState = OnboardingUiState(step = OnboardingStep.DONE),
        onLanguageSelected = {}, onThemeSelected = {}, onWelcomeNext = {}, onRestoreFromBackup = {},
        onGenderSelected = {}, onAboutYouNext = {}, onMadhabSelected = {}, onSchoolNext = {},
        onAllowLocationClick = {}, onEnterCityInstead = {}, onCityQueryChanged = {}, onCitySelected = {},
        onCitySearchContinue = {}, onUseMyLocationInstead = {}, onConfirmLocation = {}, onChangeLocationClick = {},
        onCalculationMethodSheetOpen = {}, onCalculationMethodSheetDismiss = {}, onCalculationMethodSelected = {},
        onPrayerRemindersChanged = {}, onFullScreenAlertChanged = {}, onRemindersSkip = {}, onRemindersNext = {},
        onCatchUpAddClick = {}, onCatchUpSkip = {}, onCatchUpDurationChanged = {}, onCatchUpCustomRangeChanged = { _, _ -> },
        onApplyCatchUp = {}, onFinish = {},
    )
}
