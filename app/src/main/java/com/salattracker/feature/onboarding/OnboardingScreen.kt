package com.salattracker.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.ThemeMode

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onMadhabSelected: (Madhab) -> Unit,
    onPrayerRemindersChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    onAllowLocation: () -> Unit,
    onFinish: () -> Unit,
    onRestoreFromBackup: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SalatBackground)
            .padding(horizontal = 24.dp),
    ) {
        when (uiState.step) {
            OnboardingStep.LANGUAGE      -> LanguageStep(uiState.language, onLanguageSelected, onNext, onRestoreFromBackup)
            OnboardingStep.THEME         -> ThemeStep(uiState.themeMode, onThemeSelected, onNext)
            OnboardingStep.SCHOOL        -> SchoolStep(uiState.madhab, onMadhabSelected, onNext)
            OnboardingStep.LOCATION      -> LocationStep(onAllowLocation = { onAllowLocation(); onNext() })
            OnboardingStep.NOTIFICATIONS -> NotificationsStep(
                prayerRemindersEnabled = uiState.prayerRemindersEnabled,
                isFinishing            = uiState.isFinishing,
                onPrayerRemindersChanged = onPrayerRemindersChanged,
                onFinish                 = onFinish,
            )
        }
    }
}

// ── Step 1: Language ───────────────────────────────────────────────────────────

@Composable
private fun LanguageStep(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
    onRestoreFromBackup: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
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

        Spacer(Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLanguage.values().forEach { lang ->
                SelectableRow(
                    icon       = R.drawable.ic_language,
                    label      = lang.displayName,
                    isSelected = lang == selected,
                    onClick    = { onSelect(lang) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

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

// ── Step 2: Theme ──────────────────────────────────────────────────────────────

@Composable
private fun ThemeStep(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(56.dp))
        StepDots(current = 1, total = OnboardingStep.values().size)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onboarding_theme_title), color = SalatPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_theme_subtitle), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        Spacer(Modifier.height(36.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TitledOption(R.drawable.ic_light_mode, stringResource(R.string.theme_light),  stringResource(R.string.theme_light_desc),  selected == ThemeMode.LIGHT)  { onSelect(ThemeMode.LIGHT) }
            TitledOption(R.drawable.ic_dark_mode,  stringResource(R.string.theme_dark),   stringResource(R.string.theme_dark_desc),   selected == ThemeMode.DARK)   { onSelect(ThemeMode.DARK) }
            TitledOption(R.drawable.ic_contrast,   stringResource(R.string.theme_system), stringResource(R.string.theme_system_desc), selected == ThemeMode.SYSTEM) { onSelect(ThemeMode.SYSTEM) }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_next), onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

// ── Step 3: School of thought (Asr calculation) ────────────────────────────────

@Composable
private fun SchoolStep(
    selected: Madhab,
    onSelect: (Madhab) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(56.dp))
        StepDots(current = 2, total = OnboardingStep.values().size)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onboarding_school_title), color = SalatPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_school_subtitle), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        Spacer(Modifier.height(36.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_standard_title), stringResource(R.string.school_standard_desc), selected == Madhab.STANDARD) { onSelect(Madhab.STANDARD) }
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_hanafi_title),   stringResource(R.string.school_hanafi_desc),   selected == Madhab.HANAFI)   { onSelect(Madhab.HANAFI) }
            TitledOption(R.drawable.ic_mosque, stringResource(R.string.school_jafari_title),   stringResource(R.string.school_jafari_desc),   selected == Madhab.JAFARI)   { onSelect(Madhab.JAFARI) }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_next), onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TitledOption(
    icon: Int,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
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
                Text(subtitle, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            }
            SelectionIndicator(isSelected)
        }
    }
}

// ── Step 3: Location ───────────────────────────────────────────────────────────

@Composable
private fun LocationStep(onAllowLocation: () -> Unit) {
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
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.onboarding_location_privacy), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── Step 4: Notifications ──────────────────────────────────────────────────────

@Composable
private fun NotificationsStep(
    prayerRemindersEnabled: Boolean,
    isFinishing: Boolean,
    onPrayerRemindersChanged: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(56.dp))
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

        Spacer(Modifier.height(36.dp))

        ToggleRow(
            icon    = R.drawable.ic_schedule,
            label   = stringResource(R.string.prayer_reminders),
            checked = prayerRemindersEnabled,
            onCheckedChange = onPrayerRemindersChanged,
        )

        Spacer(Modifier.height(40.dp))
        PrimaryButton(text = stringResource(R.string.action_get_started), icon = R.drawable.ic_arrow_forward, onClick = onFinish, enabled = !isFinishing)
        Spacer(Modifier.height(24.dp))
    }
}

// ── Shared pieces ──────────────────────────────────────────────────────────────

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

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingLanguagePreview() {
    OnboardingScreen(
        uiState = OnboardingUiState(step = OnboardingStep.LANGUAGE),
        onLanguageSelected = {}, onThemeSelected = {}, onMadhabSelected = {}, onPrayerRemindersChanged = {},
        onNext = {}, onAllowLocation = {}, onFinish = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingNotificationsPreview() {
    OnboardingScreen(
        uiState = OnboardingUiState(step = OnboardingStep.NOTIFICATIONS),
        onLanguageSelected = {}, onThemeSelected = {}, onMadhabSelected = {}, onPrayerRemindersChanged = {},
        onNext = {}, onAllowLocation = {}, onFinish = {},
    )
}
