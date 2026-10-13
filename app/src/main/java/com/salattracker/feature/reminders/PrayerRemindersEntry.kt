package com.salattracker.feature.reminders

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.domain.model.PrayerType

/**
 * Nav entry-point for the Prayer Reminders screen, reached from the drawer. Owns the system
 * ringtone-picker launcher, since it needs an Activity-hosted ActivityResult contract that a
 * ViewModel can't drive itself (same reasoning as BackupEntry's consent-intent launcher).
 */
@Composable
fun PrayerRemindersEntry(viewModel: PrayerRemindersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var pickerTarget by remember { mutableStateOf<PrayerType?>(null) }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val type = pickerTarget ?: return@rememberLauncherForActivityResult
        pickerTarget = null
        // RESULT_CANCELED (back button, no choice made) must leave the existing setting
        // alone — only RESULT_OK means the user actually picked something, including Silent,
        // which the picker reports as a null Uri (re-encoded as "" so it stays distinguishable
        // from "never touched" / null once persisted).
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pickedUri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.onSoundSelected(type, pickedUri?.toString() ?: "")
    }

    PrayerRemindersScreen(
        uiState                  = uiState,
        onExpandToggle           = viewModel::onExpandToggle,
        onEnabledChanged         = viewModel::onEnabledChanged,
        onOffsetChanged          = viewModel::onOffsetChanged,
        onSnoozeChanged          = viewModel::onSnoozeChanged,
        onFullScreenAlertChanged = viewModel::onFullScreenAlertChanged,
        onVibrationChanged       = viewModel::onVibrationChanged,
        onSoundClick = { type ->
            pickerTarget = type
            val existing = uiState.reminders.firstOrNull { it.prayerType == type }?.soundUri
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    existing?.takeIf { it.isNotEmpty() }?.let(Uri::parse),
                )
            }
            soundPickerLauncher.launch(intent)
        },
    )
}
