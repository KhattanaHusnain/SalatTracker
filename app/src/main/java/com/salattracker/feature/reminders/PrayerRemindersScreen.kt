package com.salattracker.feature.reminders

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.label
import com.salattracker.core.ui.salatSwitchColors
import com.salattracker.core.ui.rememberAzanPreviewPlayer
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.DEVICE_DEFAULT_SOUND_URI
import java.time.LocalDate
import java.time.LocalTime

private val SNOOZE_OPTIONS = listOf(5, 10, 15, 30)
@Composable
fun PrayerRemindersScreen(
    uiState: PrayerRemindersUiState,
    onExpandToggle: (PrayerType) -> Unit,
    onEnabledChanged: (PrayerType, Boolean) -> Unit,
    onOffsetChanged: (PrayerType, String) -> Unit,
    onSnoozeChanged: (PrayerType, Int) -> Unit,
    onFullScreenAlertChanged: (PrayerType, Boolean) -> Unit,
    onVibrationChanged: (PrayerType, Boolean) -> Unit,
    onSoundSelected: (PrayerType, String?) -> Unit,
    onChooseFromDevice: (PrayerType) -> Unit,
    onJumuahEnabledChanged: (Boolean) -> Unit,
    onJumuahReminderLeadChanged: (Int) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SalatPrimary)
        }
        return
    }

    val context = LocalContext.current
    val exactAlarmsUnavailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()
    val notificationsUnavailable = !androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (exactAlarmsUnavailable) {
            Text(
                text = stringResource(R.string.reminders_exact_alarm_unavailable),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
            )
            TextButton(onClick = {
                context.startActivity(android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }) {
                Text(stringResource(R.string.reminders_exact_alarm_fix))
            }
        }
        if (notificationsUnavailable) {
            Text(
                text = stringResource(R.string.reminders_notifications_blocked),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
            )
            TextButton(onClick = {
                context.startActivity(android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            }) {
                Text(stringResource(R.string.reminders_notifications_fix))
            }
        }
        uiState.reminders.forEach { setting ->
            ReminderCard(
                setting                  = setting,
                madhab                   = uiState.madhab,
                expanded                 = uiState.expandedPrayer == setting.prayerType,
                onExpandToggle           = { onExpandToggle(setting.prayerType) },
                onEnabledChanged         = { onEnabledChanged(setting.prayerType, it) },
                onOffsetChanged          = { onOffsetChanged(setting.prayerType, it) },
                onSnoozeChanged          = { onSnoozeChanged(setting.prayerType, it) },
                onFullScreenAlertChanged = { onFullScreenAlertChanged(setting.prayerType, it) },
                onVibrationChanged       = { onVibrationChanged(setting.prayerType, it) },
                onSoundSelected          = { onSoundSelected(setting.prayerType, it) },
                onChooseFromDevice       = { onChooseFromDevice(setting.prayerType) },
            )
        }

        // Women aren't obliged to attend Jumu'ah, so the whole card is hidden rather than just
        // togglable — matching how the status-picker screens hide Congregation/Menstruation.
        if (uiState.gender != Gender.FEMALE) JumuahCard(
            enabled         = uiState.jumuahEnabled,
            leadMinutes     = uiState.jumuahReminderLeadMinutes,
            onEnabledChanged = onJumuahEnabledChanged,
            onLeadChanged    = onJumuahReminderLeadChanged,
        )
    }
}

@Composable
private fun ReminderCard(
    setting: ReminderSetting,
    madhab: Madhab,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onOffsetChanged: (String) -> Unit,
    onSnoozeChanged: (Int) -> Unit,
    onFullScreenAlertChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onSoundSelected: (String?) -> Unit,
    onChooseFromDevice: () -> Unit,
) {
    val context = LocalContext.current
    val fullScreenUnavailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !context.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()
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
                        colors          = salatSwitchColors(),
                    )
                    Column {
                        Text(
                            text       = setting.prayerType.label(LocalDate.now(), jumuahEnabled = false, madhab = madhab),
                            color      = if (setting.enabled) SalatPrimary else SalatOnSurfaceVariant,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AppUiFontFamily,
                        )
                        Text(
                            text       = setting.prayerTime?.formatAppTime(LocalUse24HourFormat.current) ?: "--:--",
                            color      = SalatOnSurfaceVariant,
                            fontSize   = 12.sp,
                            fontFamily = AppUiFontFamily,
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
                    if (setting.fullScreenAlert && fullScreenUnavailable) {
                        Text(
                            text = stringResource(R.string.reminders_full_screen_unavailable),
                            color = SalatOnSurfaceVariant,
                            fontSize = 12.sp,
                            fontFamily = AppUiFontFamily,
                        )
                        TextButton(onClick = {
                            context.startActivity(android.content.Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
                        }) {
                            Text(stringResource(R.string.reminders_full_screen_fix))
                        }
                    }

                    SwitchDetailRow(
                        label   = stringResource(R.string.reminders_vibration),
                        checked = setting.vibrationEnabled,
                        onChanged = onVibrationChanged,
                    )

                    SoundRow(
                        soundUri           = setting.soundUri,
                        onSelect           = onSoundSelected,
                        onChooseFromDevice = onChooseFromDevice,
                    )
                }
            }
        }
    }
}

// ── Jumu'ah card ────────────────────────────────────────────────────────────

private val JUMUAH_LEAD_OPTIONS = listOf(0, 15, 30, 45, 60)

/** Separate from the five per-prayer cards: relabels Friday Dhuhr as "Jumu'ah" app-wide (Home,
 * the full-screen reminder, notifications) and optionally adds one early heads-up notification,
 * on top of (not instead of) the normal Dhuhr reminder. Off by default for the lead time, since
 * most users don't want an extra notification; the relabeling itself defaults on but can be
 * switched off entirely — women, travellers and the sick aren't obliged to attend Jumu'ah and
 * pray Dhuhr instead. */
@Composable
private fun JumuahCard(
    enabled: Boolean,
    leadMinutes: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onLeadChanged: (Int) -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = SalatSurfaceContainerLowest,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = stringResource(R.string.reminders_jumuah_title),
                        color      = SalatPrimary,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AppUiFontFamily,
                    )
                    Text(
                        text       = stringResource(R.string.reminders_jumuah_desc),
                        color      = SalatOnSurfaceVariant,
                        fontSize   = 12.sp,
                        fontFamily = AppUiFontFamily,
                    )
                }
                Switch(
                    checked         = enabled,
                    onCheckedChange = onEnabledChanged,
                    colors          = salatSwitchColors(),
                )
            }

            if (enabled) {
                HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.3f))
                Text(
                    text       = stringResource(R.string.reminders_jumuah_lead),
                    color      = SalatOnSurfaceVariant,
                    fontSize   = 14.sp,
                    fontFamily = AppUiFontFamily,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JUMUAH_LEAD_OPTIONS.forEach { minutes ->
                        JumuahLeadChip(
                            minutes    = minutes,
                            isSelected = leadMinutes == minutes,
                            onClick    = { onLeadChanged(minutes) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JumuahLeadChip(minutes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (isSelected) SalatPrimaryContainer else SalatSurfaceContainerLow,
        border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
    ) {
        Text(
            text       = if (minutes == 0) stringResource(R.string.reminders_jumuah_lead_off) else "${minutes}m",
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color      = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = AppUiFontFamily,
        )
    }
}

// A single continuous range rather than a magnitude + separate sign: scrolling down reveals
// positive values (after prayer time), scrolling up reveals negative ones (before) — the sign
// comes from scroll direction instead of a separate toggle control.
private val OFFSET_OPTIONS = -60..60
private val OFFSET_DIALOG_WIDTH = 240.dp

@Composable
private fun OffsetRow(enabled: Boolean, offsetMinutes: String, onChanged: (String) -> Unit) {
    val current = offsetMinutes.toIntOrNull() ?: 0
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.settings_minutes_offset), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily)
        Surface(
            onClick  = { showPicker = true },
            enabled  = enabled,
            shape    = RoundedCornerShape(8.dp),
            color    = SalatSurfaceContainerLow,
            border   = BorderStroke(1.dp, SalatOutlineVariant.copy(alpha = if (enabled) 1f else 0.4f)),
        ) {
            Text(
                text       = "$current",
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(min = 24.dp),
                color      = if (enabled) SalatOnSurface else SalatOnSurfaceVariant.copy(alpha = 0.4f),
                fontSize   = 14.sp,
                fontFamily = AppUiFontFamily,
                textAlign  = TextAlign.Center,
            )
        }
    }

    if (showPicker) {
        OffsetPickerDialog(
            current   = current,
            onSelect  = { minute ->
                showPicker = false
                onChanged(minute.toString())
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun OffsetPickerDialog(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = OFFSET_OPTIONS.toList()
    val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: options.indexOf(0)
    // Centers the current value in the visible window rather than pinning it to the top,
    // so it's immediately visible without an extra scroll.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceIn(0, options.lastIndex))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            // Width lives on the outer column so every child (title and list alike) shares
            // it — putting it only on the LazyColumn let the title's own width (or the
            // dialog's platform-default width) stretch the card wider than the list, leaving
            // blank space beside the selected row.
            modifier = Modifier.width(OFFSET_DIALOG_WIDTH),
            shape    = RoundedCornerShape(16.dp),
            color    = SalatSurfaceContainerLowest,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text       = stringResource(R.string.settings_minutes_offset),
                    modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color      = SalatPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppTitleFontFamily,
                )
                LazyColumn(
                    state    = listState,
                    modifier = Modifier.height(240.dp),
                ) {
                    items(options) { minute ->
                        val selected = minute == current
                        Surface(
                            onClick  = { onSelect(minute) },
                            modifier = Modifier.fillMaxWidth(),
                            color    = if (selected) SalatPrimaryContainer else SalatSurfaceContainerLowest,
                        ) {
                            Text(
                                text       = stringResource(R.string.reminders_offset_minutes_value, minute),
                                modifier   = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 20.dp),
                                color      = if (selected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
                                fontSize   = 15.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = AppUiFontFamily,
                                textAlign  = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnoozeRow(selected: Int, onChanged: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reminders_snooze_length), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily)
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
            fontFamily = AppUiFontFamily,
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
        Text(label, color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily)
        Switch(
            checked         = checked,
            onCheckedChange = onChanged,
            modifier        = Modifier.height(24.dp),
            colors          = salatSwitchColors(),
        )
    }
}

@Composable
private fun SoundRow(
    soundUri: String?,
    onSelect: (String?) -> Unit,
    onChooseFromDevice: () -> Unit,
) {
    val context = LocalContext.current
    var showChooser by remember { mutableStateOf(false) }
    val label = when (soundUri) {
        null                     -> stringResource(R.string.reminders_sound_azan)
        DEVICE_DEFAULT_SOUND_URI -> stringResource(R.string.reminders_sound_default)
        ""                       -> stringResource(R.string.reminders_sound_silent)
        else -> remember(soundUri) {
            runCatching {
                RingtoneManager.getRingtone(context, android.net.Uri.parse(soundUri))?.getTitle(context)
            }.getOrNull()
        } ?: stringResource(R.string.reminders_sound_default)
    }
    Row(
        modifier              = Modifier.fillMaxWidth().clickableRow { showChooser = true },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.reminders_sound), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = SalatPrimary, fontSize = 14.sp, fontFamily = AppUiFontFamily, maxLines = 1)
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }

    if (showChooser) {
        SoundChooserDialog(
            current = soundUri,
            onAzanSelected          = { showChooser = false; onSelect(null) },
            onDeviceDefaultSelected = { showChooser = false; onSelect(DEVICE_DEFAULT_SOUND_URI) },
            onSilentSelected        = { showChooser = false; onSelect("") },
            onChooseFromDevice      = { showChooser = false; onChooseFromDevice() },
            onDismiss               = { showChooser = false },
        )
    }
}

/** The system ringtone picker has no way to list a sound bundled inside our own app, so Azan —
 * the app's own default — is a distinct, always-available choice here rather than something
 * the system picker could ever surface; "Choose from device..." is the only option that falls
 * through to that picker. */
@Composable
private fun SoundChooserDialog(
    current: String?,
    onAzanSelected: () -> Unit,
    onDeviceDefaultSelected: () -> Unit,
    onSilentSelected: () -> Unit,
    onChooseFromDevice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }
    val azanPreviewPlayer = rememberAzanPreviewPlayer(R.raw.azan)
    // Informational only — the saved choice (e.g. Azan) is never overwritten by the device's
    // current ringer state. Checked once per dialog open rather than observed live; flipping
    // the ringer mode while this exact dialog instance is already open won't update it.
    val isRingerSilent = remember {
        context.getSystemService(AudioManager::class.java)?.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    fun stopPreviews() {
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
        azanPreviewPlayer.stop()
    }

    // Device Default goes through RingtoneManager/Ringtone — the right abstraction for a
    // system content:// URI we don't own. Azan does NOT: Ringtone.play() hands the Uri to a
    // remote system player service to resolve, and that service can't open an
    // android.resource:// Uri pointing into our own app's APK (unlike NotificationChannel's
    // sound, which the NotificationManager service resolves itself via PackageManager — a
    // different, working code path). MediaPlayer.create() plays our own bundled resource
    // directly and in-process, sidestepping that entirely.
    fun previewRingtone(uri: Uri) {
        stopPreviews()
        activeRingtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull()?.also { it.play() }
    }

    fun previewAzan() {
        stopPreviews()
        azanPreviewPlayer.play()
    }

    // A preview must never keep playing after the dialog closes, by any route — a choice
    // made, tapping outside, or system back all end up here via onDispose.
    DisposableEffect(Unit) {
        onDispose { stopPreviews() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SalatSurfaceContainerLowest,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text       = stringResource(R.string.reminders_sound),
                    modifier   = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    color      = SalatPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppTitleFontFamily,
                )
                if (isRingerSilent) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_bell_off),
                            contentDescription = null,
                            tint               = SalatOnSurfaceVariant,
                            modifier           = Modifier.size(16.dp),
                        )
                        Text(
                            text       = stringResource(R.string.reminders_sound_silenced_banner),
                            color      = SalatOnSurfaceVariant,
                            fontSize   = 12.sp,
                            fontFamily = AppUiFontFamily,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SoundChoiceRow(
                    label     = stringResource(R.string.reminders_sound_azan),
                    selected  = current == null,
                    onClick   = onAzanSelected,
                    onPreview = { previewAzan() },
                )
                SoundChoiceRow(
                    label     = stringResource(R.string.reminders_sound_default),
                    selected  = current == DEVICE_DEFAULT_SOUND_URI,
                    onClick   = onDeviceDefaultSelected,
                    onPreview = { previewRingtone(Settings.System.DEFAULT_NOTIFICATION_URI) },
                )
                SoundChoiceRow(
                    label    = stringResource(R.string.reminders_sound_silent),
                    selected = current == "",
                    onClick  = onSilentSelected,
                )
                HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                SoundChoiceRow(
                    label       = stringResource(R.string.reminders_sound_choose_device),
                    selected    = false,
                    showChevron = true,
                    onClick     = onChooseFromDevice,
                )
            }
        }
    }
}

@Composable
private fun SoundChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: (() -> Unit)? = null,
    showChevron: Boolean = false,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().clickableRow(onClick).padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = label,
            color      = if (selected) SalatPrimary else SalatOnSurface,
            fontSize   = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = AppUiFontFamily,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                showChevron -> Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
                selected    -> Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatPrimary, modifier = Modifier.size(18.dp))
            }
            // Always rightmost, after the selection mark, so its position never moves
            // depending on whether that row happens to be selected.
            if (onPreview != null) {
                IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_play),
                        contentDescription = stringResource(R.string.reminders_sound_preview),
                        tint               = SalatOnSurfaceVariant,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
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
        onSoundSelected          = { _, _ -> },
        onChooseFromDevice       = {},
        onJumuahEnabledChanged      = {},
        onJumuahReminderLeadChanged = {},
    )
}
