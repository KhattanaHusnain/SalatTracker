package com.salattracker.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.CalculationMethodSheet
import com.salattracker.core.ui.DatePickerModal
import com.salattracker.core.ui.fullLabel
import com.salattracker.core.ui.rememberAzanPreviewPlayer
import com.salattracker.domain.model.*
import com.salattracker.domain.usecase.toHijriOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    onCatchUpSkip: () -> Unit,
    onCatchUpDurationChanged: (CatchUpDuration) -> Unit,
    onCatchUpCustomRangeChanged: (LocalDate, LocalDate) -> Unit,
    onApplyCatchUp: () -> Unit,
    onUndoCatchUp: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onCatchUpNext: () -> Unit = {},
) {
    var confirmCatchUp by remember { mutableStateOf(false) }
    val s = uiState
    OnboardingDesign(s.themeMode) {
        val title = when (s.step) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_title
            OnboardingStep.ABOUT_YOU -> R.string.onboarding_about_you_title
            OnboardingStep.SCHOOL -> R.string.onboarding_school_title
            OnboardingStep.LOCATION_INTRO -> R.string.onboarding_location_title
            OnboardingStep.CITY_SEARCH -> R.string.onboarding_city_search_title
            OnboardingStep.LOCATION_CONFIRM -> R.string.onboarding_location_confirm_title
            OnboardingStep.REMINDERS -> R.string.onboarding_notifications_title
            OnboardingStep.CATCH_UP -> R.string.onboarding_catch_up_title
            OnboardingStep.CATCH_UP_RANGE -> R.string.onboarding_catch_up_add_title
            OnboardingStep.DONE -> R.string.onboarding_done_title
        }
        val subtitle = when (s.step) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_subtitle
            OnboardingStep.ABOUT_YOU -> R.string.onboarding_about_you_subtitle
            OnboardingStep.SCHOOL -> R.string.onboarding_school_subtitle
            OnboardingStep.LOCATION_INTRO -> R.string.onboarding_location_desc
            OnboardingStep.LOCATION_CONFIRM -> R.string.onboarding_location_confirm_subtitle
            OnboardingStep.REMINDERS -> R.string.onboarding_notifications_subtitle
            OnboardingStep.CATCH_UP -> R.string.onboarding_catch_up_subtitle
            OnboardingStep.CATCH_UP_RANGE -> R.string.onboarding_catch_up_add_subtitle
            OnboardingStep.DONE -> R.string.onboarding_done_subtitle
            OnboardingStep.CITY_SEARCH -> null
        }
        val action = when (s.step) {
            OnboardingStep.SCHOOL -> stringResource(R.string.action_next)
            OnboardingStep.LOCATION_INTRO -> stringResource(if (s.isLocating) R.string.onboarding_location_loading else R.string.onboarding_allow_location)
            OnboardingStep.LOCATION_CONFIRM -> stringResource(R.string.onboarding_location_confirm_action)
            OnboardingStep.REMINDERS -> stringResource(if (s.prayerRemindersEnabled) R.string.onboarding_reminders_turn_on else R.string.action_continue)
            OnboardingStep.CATCH_UP -> stringResource(R.string.onboarding_add_missed)
            OnboardingStep.CATCH_UP_RANGE -> pluralStringResource(R.plurals.onboarding_catch_up_add_action, s.catchUpPrayerCount, s.catchUpPrayerCount)
            OnboardingStep.DONE -> stringResource(R.string.onboarding_done_action)
            else -> stringResource(R.string.action_continue)
        }
        val secondary = when (s.step) {
            OnboardingStep.WELCOME -> R.string.onboarding_restore_backup
            OnboardingStep.LOCATION_INTRO, OnboardingStep.LOCATION_CONFIRM -> R.string.onboarding_location_enter_city
            OnboardingStep.CITY_SEARCH -> R.string.onboarding_location_use_my_location
            OnboardingStep.REMINDERS, OnboardingStep.CATCH_UP -> R.string.action_skip_for_now
            else -> null
        }
        OnboardingFrame(
            step = s.step, title = stringResource(title), subtitle = subtitle?.let { stringResource(it) },
            action = action, enabled = when (s.step) {
                OnboardingStep.ABOUT_YOU -> s.gender != null
                OnboardingStep.CITY_SEARCH, OnboardingStep.LOCATION_CONFIRM -> s.confirmedCity != null
                OnboardingStep.LOCATION_INTRO -> !s.isLocating
                OnboardingStep.CATCH_UP_RANGE -> !s.isApplyingCatchUp
                OnboardingStep.DONE -> !s.isFinishing
                else -> true
            },
            onAction = {
                when (s.step) {
                    OnboardingStep.WELCOME -> onWelcomeNext()
                    OnboardingStep.ABOUT_YOU -> onAboutYouNext()
                    OnboardingStep.SCHOOL -> onSchoolNext()
                    OnboardingStep.LOCATION_INTRO -> onAllowLocationClick()
                    OnboardingStep.CITY_SEARCH -> onCitySearchContinue()
                    OnboardingStep.LOCATION_CONFIRM -> onConfirmLocation()
                    OnboardingStep.REMINDERS -> onRemindersNext()
                    OnboardingStep.CATCH_UP -> onCatchUpNext()
                    OnboardingStep.CATCH_UP_RANGE -> { confirmCatchUp = true }
                    OnboardingStep.DONE -> onFinish()
                }
            },
            secondary = secondary?.let { stringResource(it) },
            onSecondary = {
                when (s.step) {
                    OnboardingStep.WELCOME -> onRestoreFromBackup()
                    OnboardingStep.LOCATION_INTRO -> onEnterCityInstead()
                    OnboardingStep.LOCATION_CONFIRM -> onChangeLocationClick()
                    OnboardingStep.CITY_SEARCH -> onUseMyLocationInstead()
                    OnboardingStep.REMINDERS -> onRemindersSkip()
                    OnboardingStep.CATCH_UP -> onCatchUpSkip()
                    else -> Unit
                }
            },
            onBack = onBack,
        ) {
            when (s.step) {
                OnboardingStep.WELCOME -> {
                    SectionLabel(stringResource(R.string.settings_language))
                    AppLanguage.values().forEach { language ->
                        OptionRow(language.displayName, selected = s.language == language,
                            onClick = { onLanguageSelected(language) })
                    }
                    Spacer(Modifier.height(12.dp))
                    SectionLabel(stringResource(R.string.onboarding_theme_label))
                    Row(Modifier.fillMaxWidth().testTag("onboarding-theme-tray").clip(RoundedCornerShape(10.dp))
                        .background(SalatSurfaceContainerLow).padding(2.dp)) {
                        listOf(Triple(ThemeMode.LIGHT, R.drawable.ic_light_mode, R.string.theme_light),
                            Triple(ThemeMode.DARK, R.drawable.ic_dark_mode, R.string.theme_dark),
                            Triple(ThemeMode.SYSTEM, R.drawable.ic_contrast, R.string.theme_system)).forEach { (mode, icon, label) ->
                            val selected = mode == s.themeMode
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) SalatBackground else Color.Transparent)
                                .selectable(selected, onClick = { onThemeSelected(mode) }, role = Role.RadioButton)
                                .heightIn(min = 40.dp).padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(icon), null, Modifier.size(13.dp), tint = SalatPrimary)
                                Spacer(Modifier.width(5.dp))
                                BodyText(stringResource(label), size = 11, weight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
                OnboardingStep.ABOUT_YOU -> {
                    OptionRow(stringResource(R.string.onboarding_gender_male), stringResource(R.string.onboarding_gender_male_desc),
                        s.gender == Gender.MALE, R.drawable.onboarding_male) { onGenderSelected(Gender.MALE) }
                    OptionRow(stringResource(R.string.onboarding_gender_female), stringResource(R.string.onboarding_gender_female_desc),
                        s.gender == Gender.FEMALE, R.drawable.onboarding_female) { onGenderSelected(Gender.FEMALE) }
                    Spacer(Modifier.height(14.dp))
                    BodyText(stringResource(R.string.onboarding_excused_explanation))
                    Spacer(Modifier.height(12.dp))
                    BodyText(stringResource(R.string.onboarding_location_privacy), size = 11, muted = true)
                }
                OnboardingStep.SCHOOL -> {
                    listOf(Triple(Madhab.STANDARD, R.string.school_standard_title, R.string.school_standard_desc),
                        Triple(Madhab.HANAFI, R.string.school_hanafi_title, R.string.school_hanafi_desc),
                        Triple(Madhab.JAFARI, R.string.school_jafari_title, R.string.school_jafari_desc)).forEach { (school, label, description) ->
                        OptionRow(stringResource(label), stringResource(description), s.madhab == school) { onMadhabSelected(school) }
                    }
                }
                OnboardingStep.LOCATION_INTRO -> {
                    OnboardingLocationHero()
                    SummaryRow(stringResource(R.string.onboarding_prayer_times), stringResource(R.string.onboarding_exact_city), R.drawable.ic_schedule)
                    SummaryRow(stringResource(R.string.onboarding_hijri_date_label), stringResource(R.string.onboarding_region_date), R.drawable.ic_calendar_today)
                    Spacer(Modifier.height(12.dp))
                    BodyText(stringResource(R.string.onboarding_location_privacy), size = 11, muted = true)
                    if (s.locationError) {
                        Spacer(Modifier.height(12.dp))
                        BodyText(stringResource(R.string.onboarding_location_unavailable), color = SalatError)
                    }
                }
                OnboardingStep.CITY_SEARCH -> CitySearchBody(s, onCityQueryChanged, onCitySelected)
                OnboardingStep.LOCATION_CONFIRM -> {
                    SummaryRow(s.confirmedCity?.fullLabel.orEmpty(), stringResource(R.string.onboarding_location_label),
                        R.drawable.onboarding_pin, stringResource(R.string.onboarding_location_change), onChangeLocationClick)
                    SummaryRow(s.calculationMethod.displayName + " · " + s.madhab.name.lowercase().replaceFirstChar { it.titlecase() },
                        stringResource(R.string.settings_calc_method), R.drawable.onboarding_method,
                        stringResource(R.string.onboarding_location_change), onCalculationMethodSheetOpen)
                    val hijri = remember { LocalDate.now().toHijriOrNull() }
                    if (hijri != null) SummaryRow(hijri.fullLabel(), stringResource(R.string.onboarding_hijri_date_label), R.drawable.ic_calendar_today)
                    Spacer(Modifier.height(12.dp))
                    BodyText(stringResource(R.string.onboarding_location_privacy), size = 11, muted = true)
                }
                OnboardingStep.REMINDERS -> {
                    ReminderRow(stringResource(R.string.prayer_reminders), stringResource(R.string.onboarding_each_adhan),
                        R.drawable.ic_notifications, s.prayerRemindersEnabled, onPrayerRemindersChanged)
                    ReminderRow(stringResource(R.string.onboarding_reminders_fullscreen_title), stringResource(R.string.onboarding_reminders_fullscreen_desc),
                        R.drawable.onboarding_screen, s.fullScreenAlertEnabled, onFullScreenAlertChanged)
                    val preview = rememberAzanPreviewPlayer(R.raw.azan)
                    SummaryRow(stringResource(R.string.reminders_sound_azan), stringResource(R.string.onboarding_sound),
                        R.drawable.onboarding_sound, stringResource(R.string.onboarding_preview), { preview.play() })
                    Spacer(Modifier.height(12.dp))
                    BodyText(stringResource(R.string.onboarding_android_permissions), size = 11, muted = true)
                }
                OnboardingStep.CATCH_UP -> {
                    val arabic = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
                    PrayerType.values().forEachIndexed { index, prayer ->
                        Row(Modifier.fillMaxWidth().heightIn(min = LocalOnboardingRowMinHeight.current).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            BodyText(prayer.name.lowercase().replaceFirstChar { it.titlecase() }, Modifier.weight(1f), size = 15)
                            Text(arabic[index], color = SalatOnSurfaceVariant, fontFamily = OnboardingArabicFont, fontSize = 20.sp)
                            Spacer(Modifier.width(14.dp))
                            BodyText("0", weight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = SalatOutlineVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    BodyText(stringResource(R.string.onboarding_catch_up_explanation))
                }
                OnboardingStep.CATCH_UP_RANGE -> {
                    CatchUpDuration.values().forEach { duration ->
                        val label = when (duration) {
                            CatchUpDuration.WEEK -> R.string.onboarding_catch_up_duration_week
                            CatchUpDuration.MONTH -> R.string.onboarding_catch_up_duration_month
                            CatchUpDuration.HALF_YEAR -> R.string.onboarding_catch_up_duration_half_year
                            CatchUpDuration.CUSTOM -> R.string.bulk_dur_custom
                        }
                        OptionRow(stringResource(label),
                            if (duration == CatchUpDuration.CUSTOM) stringResource(R.string.onboarding_pick_dates)
                            else pluralStringResource(R.plurals.onboarding_catch_up_prayer_count, duration.days * 5, duration.days * 5),
                            s.catchUpDuration == duration) { onCatchUpDurationChanged(duration) }
                    }
                    if (s.catchUpDuration == CatchUpDuration.CUSTOM)
                        CustomRangePickers(s.catchUpCustomFrom, s.catchUpCustomTo, onCatchUpCustomRangeChanged)
                    SummaryRow(stringResource(R.string.onboarding_all_prayers), stringResource(R.string.onboarding_prayers_included))
                    Spacer(Modifier.height(12.dp))
                    BodyText(stringResource(R.string.onboarding_edit_calendar), size = 11, muted = true)
                }
                OnboardingStep.DONE -> {
                    SummaryRow(s.language.displayName, stringResource(R.string.onboarding_done_summary_language))
                    SummaryRow(listOfNotNull(s.confirmedCity?.name, s.madhab.name.lowercase().replaceFirstChar { it.titlecase() }).joinToString(", "),
                        stringResource(R.string.settings_calc_method))
                    SummaryRow(stringResource(if (s.prayerRemindersEnabled) R.string.onboarding_done_reminders_on else R.string.onboarding_done_reminders_off),
                        stringResource(R.string.onboarding_done_summary_reminders))
                    SummaryRow(stringResource(if (s.catchUpAppliedCount != null) R.string.onboarding_done_qaza_added else R.string.onboarding_done_qaza_set_up_later),
                        stringResource(R.string.onboarding_done_summary_qaza))
                    if (s.catchUpUndoAvailable) TextButton(onClick = onUndoCatchUp) { BodyText(stringResource(R.string.action_undo), color = SalatPrimary) }
                }
            }
        }
        if (s.showCalculationMethodSheet) CalculationMethodSheet(s.calculationMethod, onCalculationMethodSelected, onCalculationMethodSheetDismiss)
        if (confirmCatchUp) AlertDialog(
            onDismissRequest = { confirmCatchUp = false },
            title = { BodyText(stringResource(R.string.onboarding_catch_up_confirm_title), size = 20) },
            text = { BodyText(stringResource(R.string.onboarding_catch_up_confirm_message, s.catchUpPrayerCount)) },
            confirmButton = { TextButton(onClick = { confirmCatchUp = false; onApplyCatchUp() }) { BodyText(stringResource(R.string.action_continue), color = SalatPrimary) } },
            dismissButton = { TextButton(onClick = { confirmCatchUp = false }) { BodyText(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The body receives bounded space; actions are measured first and never scroll out of reach. */
@Composable
private fun OnboardingFrame(
    step: OnboardingStep, title: String, subtitle: String?,
    action: String, enabled: Boolean, onAction: () -> Unit,
    secondary: String?, onSecondary: () -> Unit, onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SalatBackground).safeDrawingPadding().imePadding()) {
        Box(Modifier.fillMaxWidth()) {
            OnboardingLanterns(Modifier.matchParentSize().wrapContentWidth(Alignment.End, unbounded = true)
                .width(190.dp).offset(x = 8.dp, y = (-16).dp).testTag("onboarding-lanterns"))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp)) {
                val progress = when (step) {
                    OnboardingStep.WELCOME -> 1
                    OnboardingStep.ABOUT_YOU -> 2
                    OnboardingStep.SCHOOL, OnboardingStep.LOCATION_INTRO, OnboardingStep.LOCATION_CONFIRM, OnboardingStep.CITY_SEARCH -> 3
                    OnboardingStep.REMINDERS -> 4
                    OnboardingStep.CATCH_UP, OnboardingStep.CATCH_UP_RANGE -> 5
                    OnboardingStep.DONE -> 0
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (progress != 0) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { index -> Box(Modifier.weight(1f).height(2.dp).clip(CircleShape)
                                .background(if (index < progress) SalatPrimary else SalatOutlineVariant)) }
                        }
                        Spacer(Modifier.width(10.dp))
                        BodyText(stringResource(R.string.onboarding_step_progress, progress, 5), size = 10, muted = true)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(title, color = SalatOnSurface, fontFamily = if (LocalConfiguration.current.locales[0].language in listOf("ar", "ur")) OnboardingArabicFont else OnboardingTitleFont,
                    fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp,
                    textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(26.dp).height(2.dp).background(OnboardingGold))
                Spacer(Modifier.height(10.dp))
                if (subtitle != null) BodyText(subtitle, size = 14, muted = true)
                Spacer(Modifier.height(14.dp))
            }
        }
        key(step) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Share spare height across the functional rows, reserving room for
                // descriptions and helper text. Keep content near the heading; do not
                // move the entire body down into the middle of a tall window.
                val (rows, supportingContent, rowLimit) = when (step) {
                    OnboardingStep.WELCOME -> Triple(4, 152.dp, 96.dp)
                    OnboardingStep.ABOUT_YOU -> Triple(2, 148.dp, 144.dp)
                    OnboardingStep.SCHOOL -> Triple(3, 32.dp, 144.dp)
                    OnboardingStep.LOCATION_INTRO -> Triple(2, 201.dp, 120.dp)
                    OnboardingStep.CITY_SEARCH -> Triple(3, 128.dp, 128.dp)
                    OnboardingStep.LOCATION_CONFIRM, OnboardingStep.REMINDERS -> Triple(3, 80.dp, 128.dp)
                    OnboardingStep.CATCH_UP -> Triple(5, 100.dp, 96.dp)
                    OnboardingStep.CATCH_UP_RANGE -> Triple(5, 80.dp, 112.dp)
                    OnboardingStep.DONE -> Triple(4, 24.dp, 128.dp)
                }
                val rowHeight = ((maxHeight - supportingContent) / rows).coerceIn(64.dp, rowLimit)
                CompositionLocalProvider(LocalOnboardingRowMinHeight provides rowHeight) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight).padding(horizontal = 20.dp, vertical = 12.dp), content = content)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 12.dp)
            .testTag("onboarding-footer")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (step != OnboardingStep.WELCOME && step != OnboardingStep.DONE) {
                    OutlinedIconButton(onClick = onBack, modifier = Modifier.size(48.dp),
                        shape = CircleShape, border = BorderStroke(1.dp, SalatOutlineVariant)) {
                        Icon(painterResource(R.drawable.onboarding_back), stringResource(R.string.cd_back), Modifier.size(18.dp), tint = SalatOnSurfaceVariant)
                    }
                }
                Button(onClick = onAction, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = CircleShape, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary,
                        disabledContainerColor = SalatOutlineVariant, disabledContentColor = SalatOnSurfaceVariant)) {
                    BodyText(action, size = 13, color = if (enabled) SalatOnPrimary else SalatOnSurfaceVariant, weight = FontWeight.SemiBold)
                    if (step != OnboardingStep.DONE) {
                        Spacer(Modifier.width(6.dp))
                        Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(15.dp))
                    }
                }
            }
            if (secondary != null) {
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 36.dp).clickable(role = Role.Button, onClick = onSecondary).padding(3.dp),
                    contentAlignment = Alignment.Center) {
                    BodyText(secondary, size = 12, muted = true)
                }
            }
        }
    }
}

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier, size: Int = 14, muted: Boolean = false,
    weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified) {
    val language = LocalConfiguration.current.locales[0].language
    val font = when {
        language == "ur" -> NastaliqUrduFamily
        language == "ar" || text.any { it in '\u0600'..'\u06FF' } -> OnboardingArabicFont
        else -> OnboardingBodyFont
    }
    Text(text, modifier, color = if (color != Color.Unspecified) color else if (muted) SalatOnSurfaceVariant else SalatOnSurface,
        fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontWeight = weight, fontFamily = font)
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(10.dp))
    BodyText(text.uppercase(), size = 11, muted = true)
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun OptionRow(label: String, description: String? = null, selected: Boolean, icon: Int? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = Role.RadioButton)
        .heightIn(min = LocalOnboardingRowMinHeight.current).padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            BodyText(label, size = 15, color = if (selected) SalatPrimary else SalatOnSurface)
            if (description != null) BodyText(description, size = 12, muted = true)
        }
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(20.dp), tint = SalatPrimary)
        Surface(shape = CircleShape, color = SalatBackground, border = BorderStroke(if (selected) 5.dp else 1.dp,
            if (selected) SalatPrimary else SalatOutlineVariant), modifier = Modifier.size(16.dp)) {}
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

/** Reserve the action's width before measuring the content, including long method/city names. */
@Composable
private fun SummaryRow(value: String, label: String, icon: Int? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = maxOf(72.dp, LocalOnboardingRowMinHeight.current)).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(18.dp), tint = SalatPrimary)
        Column(Modifier.weight(1f)) {
            BodyText(label, size = 12, muted = true)
            BodyText(value, size = 16, weight = FontWeight.SemiBold)
        }
        if (action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
            BodyText(action, size = 12, color = SalatPrimary, weight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

@Composable
private fun ReminderRow(label: String, description: String, icon: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = maxOf(76.dp, LocalOnboardingRowMinHeight.current)).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon), null, Modifier.size(18.dp), tint = SalatPrimary)
        Column(Modifier.weight(1f)) {
            BodyText(label, size = 15, weight = FontWeight.SemiBold)
            BodyText(description, size = 12, muted = true)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = SalatPrimary,
            checkedThumbColor = SalatBackground, uncheckedTrackColor = SalatOutlineVariant))
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

@Composable
private fun CitySearchBody(s: OnboardingUiState, onQuery: (String) -> Unit, onSelect: (CityResult) -> Unit) {
    TextField(value = s.cityQuery, onValueChange = onQuery, singleLine = true,
        modifier = Modifier.fillMaxWidth(), placeholder = { BodyText(stringResource(R.string.onboarding_city_search_placeholder), size = 16, muted = true) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(18.dp)) },
        textStyle = LocalTextStyle.current.copy(fontFamily = OnboardingBodyFont, fontSize = 16.sp),
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = SalatPrimary, unfocusedIndicatorColor = SalatOutlineVariant))
    Spacer(Modifier.height(12.dp))
    when {
        s.isSearchingCity -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), color = SalatPrimary, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            BodyText(stringResource(R.string.onboarding_city_search_searching), muted = true)
        }
        s.cityQuery.isNotBlank() && s.cityResults.isEmpty() ->
            BodyText(stringResource(R.string.onboarding_city_search_no_results, s.cityQuery))
        else -> {
            val results = if (s.cityQuery.isBlank()) suggestedCities else s.cityResults
            if (s.cityQuery.isBlank()) SectionLabel(stringResource(R.string.onboarding_suggested_cities))
            results.forEach { city ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(city) }
                    .heightIn(min = maxOf(72.dp, LocalOnboardingRowMinHeight.current)).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BodyText(city.name, size = 15, weight = FontWeight.SemiBold)
                        BodyText(city.displayLine, size = 12, muted = true)
                    }
                    if (s.confirmedCity == city) Icon(painterResource(R.drawable.ic_check_circle_filled), null, Modifier.size(18.dp), tint = SalatPrimary)
                }
                HorizontalDivider(color = SalatOutlineVariant)
            }
        }
    }
}

private val suggestedCities = listOf(
    CityResult("Karachi", "Sindh", "Pakistan", 24.8607, 67.0011, "PK"),
    CityResult("Lahore", "Punjab", "Pakistan", 31.5204, 74.3587, "PK"),
    CityResult("Islamabad", "Capital Territory", "Pakistan", 33.6844, 73.0479, "PK"),
)

@Composable
private fun CustomRangePickers(from: LocalDate, to: LocalDate, onChanged: (LocalDate, LocalDate) -> Unit) {
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    // Stacked fields remain legible for localized dates and enlarged fonts.
    TextButton(onClick = { pickFrom = true }) { BodyText(stringResource(R.string.bulk_start, from.format(formatter)), color = SalatPrimary) }
    TextButton(onClick = { pickTo = true }) { BodyText(stringResource(R.string.bulk_end, to.format(formatter)), color = SalatPrimary) }
    if (pickFrom) DatePickerModal(initialDate = from, onDatePicked = { pickFrom = false; onChanged(it, to) }, onDismiss = { pickFrom = false })
    if (pickTo) DatePickerModal(initialDate = to, onDatePicked = { pickTo = false; onChanged(from, it) }, onDismiss = { pickTo = false })
}
