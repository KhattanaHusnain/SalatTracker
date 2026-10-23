package com.salattracker.feature.backup

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.SettingsCard
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.core.ui.formatAppTime
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.RemoteBackupInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun BackupScreen(
    uiState: BackupUiState,
    isOnboarding: Boolean,
    onConnectClick: () -> Unit,
    onBackupNowClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onDisconnectConfirm: () -> Unit,
    onDisconnectDismiss: () -> Unit,
    onReplaceRemoteConfirm: () -> Unit,
    onConflictDismiss: () -> Unit,
    onErrorDismiss: () -> Unit,
    onMessageDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(SalatSecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_cloud_upload), null, tint = SalatPrimary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.backup_title), color = SalatPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.backup_subtitle), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
            }

            if (uiState.attention != BackupAttention.NONE && uiState.conflictInfo == null) {
                AttentionBanner(uiState.attention)
            }

            if (!uiState.connected) {
                NotConnectedCard(isWorking = uiState.isWorking, onConnectClick = onConnectClick)
            } else {
                ConnectedCard(
                    uiState           = uiState,
                    isOnboarding      = isOnboarding,
                    onBackupNowClick  = onBackupNowClick,
                    onRestoreClick    = onRestoreClick,
                    onDisconnectClick = onDisconnectClick,
                    onConnectClick    = onConnectClick,
                )
            }
        }

        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = AppUiFontFamily) }
        }

        uiState.message?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onMessageDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) { Text(msg, fontFamily = AppUiFontFamily) }
        }
    }

    if (uiState.showDisconnectConfirm) {
        DisconnectConfirmDialog(onConfirm = onDisconnectConfirm, onDismiss = onDisconnectDismiss)
    }

    uiState.conflictInfo?.let { info ->
        ConflictDialog(
            info                    = info,
            isOnboarding            = isOnboarding,
            onRestore               = onRestoreClick,
            onReplace               = onReplaceRemoteConfirm,
            onDismiss               = onConflictDismiss,
        )
    }
}

// ── Not connected ──────────────────────────────────────────────────────────────

@Composable
private fun NotConnectedCard(isWorking: Boolean, onConnectClick: () -> Unit) {
    SettingsCard {
        Text(
            stringResource(R.string.backup_not_connected_desc),
            color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
        )
        Spacer(Modifier.height(16.dp))
        com.salattracker.core.ui.SalatPrimaryButton(
            onClick  = onConnectClick,
            enabled  = !isWorking,
        ) {
            if (isWorking) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.backup_connect), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Connected ──────────────────────────────────────────────────────────────────

@Composable
private fun ConnectedCard(
    uiState: BackupUiState,
    isOnboarding: Boolean,
    onBackupNowClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onConnectClick: () -> Unit,
) {
    SettingsCard {
        LabeledRow(stringResource(R.string.backup_connected_account), uiState.accountEmail ?: "—")
        Spacer(Modifier.height(10.dp))
        LabeledRow(
            stringResource(R.string.backup_last_backup),
            formatLastBackup(uiState.lastBackupAt, uiState.lastBackupBytes, stringResource(R.string.backup_last_backup_never), LocalUse24HourFormat.current),
        )

        Spacer(Modifier.height(16.dp))
        com.salattracker.core.ui.SalatPrimaryButton(
            onClick  = onBackupNowClick,
            enabled  = !uiState.isWorking,
        ) {
            if (uiState.isWorking) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.backup_back_up_now), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
        }

        if (!isOnboarding) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick  = onRestoreClick,
                enabled  = !uiState.isWorking,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, SalatOutlineVariant),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SalatPrimary),
            ) { Text(stringResource(R.string.backup_restore), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onConnectClick, enabled = !uiState.isWorking) {
                    Text(stringResource(R.string.backup_switch_account), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily, fontSize = 13.sp)
                }
                TextButton(onClick = onDisconnectClick, enabled = !uiState.isWorking) {
                    Text(stringResource(R.string.backup_disconnect), color = SalatError, fontFamily = AppUiFontFamily, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
        Text(value, color = SalatOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
    }
}

// ── Attention banner ─────────────────────────────────────────────────────────────

@Composable
private fun AttentionBanner(attention: BackupAttention) {
    val text = when (attention) {
        BackupAttention.SIGN_IN_REQUIRED -> stringResource(R.string.backup_attention_sign_in)
        BackupAttention.REMOTE_CHANGED -> stringResource(R.string.backup_attention_remote_changed)
        BackupAttention.LOCAL_DATA_MISSING -> stringResource(R.string.backup_attention_local_missing)
        BackupAttention.STORAGE_FULL -> stringResource(R.string.backup_attention_storage_full)
        BackupAttention.NONE -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = SalatErrorContainer,
    ) {
        Text(
            text, modifier = Modifier.padding(14.dp),
            color = SalatOnErrorContainer, fontSize = 13.sp, fontFamily = AppUiFontFamily,
        )
    }
}

// ── Conflict dialog ───────────────────────────────────────────────────────────────

@Composable
private fun ConflictDialog(
    info: RemoteBackupInfo,
    isOnboarding: Boolean,
    onRestore: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = SalatSurfaceContainerLowest,
        title = {
            Text(stringResource(R.string.backup_conflict_title), color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        },
        text = {
            val deviceLabel = info.deviceModel.ifBlank { stringResource(R.string.backup_unknown_device) }
            Text(
                stringResource(R.string.backup_conflict_desc, deviceLabel, formatEpoch(info.createdAt)),
                color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
            )
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors  = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                shape   = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.backup_conflict_restore), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            if (isOnboarding) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
            } else {
                TextButton(onClick = onReplace) { Text(stringResource(R.string.backup_conflict_replace), color = SalatError, fontFamily = AppUiFontFamily) }
            }
        },
    )
}

// ── Disconnect dialog ──────────────────────────────────────────────────────────────

@Composable
private fun DisconnectConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = SalatSurfaceContainerLowest,
        title = { Text(stringResource(R.string.backup_disconnect_title), color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily) },
        text  = { Text(stringResource(R.string.backup_disconnect_desc), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = SalatError, contentColor = SalatOnPrimary),
                shape   = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.backup_disconnect), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
        },
    )
}

// ── Formatting ─────────────────────────────────────────────────────────────────

private fun formatEpoch(epochMillis: Long, use24HourFormat: Boolean): String =
    if (epochMillis <= 0L) "—" else
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).let {
            "${it.toLocalDate().format(DateTimeFormatter.ofPattern(\"d MMM yyyy\"))}, ${it.toLocalTime().formatAppTime(use24HourFormat)}"
        }

private fun formatLastBackup(epochMillis: Long?, bytes: Long?, neverLabel: String, use24HourFormat: Boolean): String {
    if (epochMillis == null) return neverLabel
    val whenText = formatEpoch(epochMillis, use24HourFormat)
    val size = bytes?.let { " (${formatBytes(it)})" } ?: ""
    return "$whenText$size"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BackupScreenConnectedPreview() {
    BackupScreen(
        uiState = BackupUiState(
            isLoading = false, connected = true, accountEmail = "husnain@gmail.com",
            lastBackupAt = System.currentTimeMillis(), lastBackupBytes = 48_000L,
        ),
        isOnboarding = false,
        onConnectClick = {}, onBackupNowClick = {}, onRestoreClick = {}, onDisconnectClick = {},
        onDisconnectConfirm = {}, onDisconnectDismiss = {}, onReplaceRemoteConfirm = {},
        onConflictDismiss = {}, onErrorDismiss = {}, onMessageDismiss = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BackupScreenNotConnectedPreview() {
    BackupScreen(
        uiState = BackupUiState(isLoading = false, connected = false),
        isOnboarding = false,
        onConnectClick = {}, onBackupNowClick = {}, onRestoreClick = {}, onDisconnectClick = {},
        onDisconnectConfirm = {}, onDisconnectDismiss = {}, onReplaceRemoteConfirm = {},
        onConflictDismiss = {}, onErrorDismiss = {}, onMessageDismiss = {},
    )
}
