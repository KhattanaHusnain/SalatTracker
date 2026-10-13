package com.salattracker.feature.reminders

import android.media.RingtoneManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
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
import com.salattracker.domain.model.PrayerType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SNOOZE_OPTIONS = listOf(5, 10, 15, 30)
private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun PrayerRemindersScreen(
    uiState: PrayerRemindersUiState,
    onExpandToggle: (PrayerType) -> Unit,
    onEnabledChanged: (PrayerType, Boolean) -> Unit,
    onOffsetChanged: (PrayerType, String) -> Unit,
    onSnoozeChanged: (PrayerType, Int) -> Unit,
    onFullScreenAlertChanged: (PrayerType, Boolean) -> Unit,
    onVibrationChanged: (PrayerType, Boolean) -> Unit,
    onSoundClick: (PrayerType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.reminders.forEach { setting ->
            ReminderCard(
                setting                  = setting,
                expanded                 = uiState.expandedPrayer == setting.prayerType,
                onExpandToggle           = { onExpandToggle(setting.prayerType) },
                onEnabledChanged         = { onEnabledChanged(setting.prayerType, it) },
                onOffsetChanged          = { onOffsetChanged(setting.prayerType, it) },
                onSnoozeChanged          = { onSnoozeChanged(setting.prayerType, it) },
                onFullScreenAlertChanged = { onFullScreenAlertChanged(setting.prayerType, it) },
                onVibrationChanged       = { onVibrationChanged(setting.prayerType, it) },
                onSoundClick             = { onSoundClick(setting.prayerType) },
            )
        }
    }
}

@Composable
private fun ReminderCard(
    setting: ReminderSetting,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onOffsetChanged: (String) -> Unit,
    onSnoozeChanged: (Int) -> Unit,
    onFullScreenAlertChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onSoundClick: () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = SalatSurfaceContainerLowest,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth().clickableRow(onExpandToggle).padding(16.dp),
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
                        onCheckedChange = onEnabledChanged,
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
                Icon(
                    painter            = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint               = SalatOnSurfaceVariant,
                    modifier           = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.3f))

                    OffsetRow(
                        enabled       = setting.enabled,
                        offsetMinutes = setting.offsetMinutes,
                        onChanged     = onOffsetChanged,
                    )

                    SnoozeRow(
                        selected  = setting.snoozeMinutes,
                        onChanged = onSnoozeChanged,
                    )

                    SwitchDetailRow(
                        label   = stringResource(R.string.reminders_full_screen_alert),
                        checked = setting.fullScreenAlert,
                        onChanged = onFullScreenAlertChanged,
                    )

                    SwitchDetailRow(
                        label   = stringResource(R.string.reminders_vibration),
                        checked = setting.vibrationEnabled,
                        onChanged = onVibrationChanged,
                    )

                    SoundRow(soundUri = setting.soundUri, onClick = onSoundClick)
                }
            }
        }
    }
}

@Composable
private fun OffsetRow(enabled: Boolean, offsetMinutes: String, onChanged: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val isNegative   = offsetMinutes.startsWith("-")
    val magnitude    = offsetMinutes.removePrefix("-")

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.settings_minutes_offset), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Soft keyboards' number pad has no "-" key, so the sign is a dedicated toggle.
            SignToggleButton(
                isNegative = isNegative,
                enabled    = enabled,
                onClick    = {
                    val toggled = when {
                        isNegative                             -> magnitude
                        magnitude.isEmpty() || magnitude == "0" -> magnitude
                        else                                    -> "-$magnitude"
                    }
                    onChanged(toggled)
                },
            )
            OutlinedTextField(
                value         = magnitude,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all(Char::isDigit)) {
                        onChanged((if (isNegative) "-" else "") + input)
                    }
                },
                enabled       = enabled,
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
private fun SignToggleButton(isNegative: Boolean, enabled: Boolean, onClick: () -> Unit) {
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

@Composable
private fun SnoozeRow(selected: Int, onChanged: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reminders_snooze_length), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SNOOZE_OPTIONS.forEach { minutes ->
                SnoozeChip(minutes = minutes, isSelected = selected == minutes, onClick = { onChanged(minutes) })
            }
        }
    }
}

@Composable
private fun SnoozeChip(minutes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (isSelected) SalatPrimaryContainer else SalatSurfaceContainerLow,
        border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
    ) {
        Text(
            text       = "${minutes}m",
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color      = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
        )
    }
}

@Composable
private fun SwitchDetailRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
        Switch(
            checked         = checked,
            onCheckedChange = onChanged,
            modifier        = Modifier.height(24.dp),
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = SalatOnPrimary,
                checkedTrackColor   = SalatPrimary,
                uncheckedThumbColor = SalatOutline,
                uncheckedTrackColor = SalatOutlineVariant,
            ),
        )
    }
}

@Composable
private fun SoundRow(soundUri: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val label = when (soundUri) {
        null -> stringResource(R.string.reminders_sound_default)
        ""   -> stringResource(R.string.reminders_sound_silent)
        else -> remember(soundUri) {
            runCatching {
                RingtoneManager.getRingtone(context, android.net.Uri.parse(soundUri))?.getTitle(context)
            }.getOrNull()
        } ?: stringResource(R.string.reminders_sound_default)
    }
    Row(
        modifier              = Modifier.fillMaxWidth().clickableRow(onClick),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.reminders_sound), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = SalatPrimary, fontSize = 14.sp, fontFamily = InterFamily, maxLines = 1)
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrayerRemindersScreenPreview() {
    PrayerRemindersScreen(
        uiState = PrayerRemindersUiState(
            isLoading = false,
            expandedPrayer = PrayerType.FAJR,
            reminders = PrayerType.values().mapIndexed { i, type ->
                ReminderSetting(
                    prayerType    = type,
                    enabled       = true,
                    offsetMinutes = "-10",
                    prayerTime    = LocalTime.of(5 + i * 2, 30),
                    snoozeMinutes = 10,
                    fullScreenAlert = i != 1,
                    vibrationEnabled = true,
                    soundUri = if (i == 2) "" else null,
                )
            },
        ),
        onExpandToggle           = {},
        onEnabledChanged         = { _, _ -> },
        onOffsetChanged          = { _, _ -> },
        onSnoozeChanged          = { _, _ -> },
        onFullScreenAlertChanged = { _, _ -> },
        onVibrationChanged       = { _, _ -> },
        onSoundClick             = {},
    )
}
