package com.salattracker.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for Backup & Restore. Owns the consent-sheet launcher, since
 * launching an IntentSender needs an Activity-hosted ActivityResult contract that
 * a domain-layer repository can't drive itself (see BackupRepository.connect()).
 *
 * [isOnboarding] hides Switch Account / Disconnect / Replace and is used when this
 * is hosted inside the first-run onboarding flow rather than from Settings.
 */
@Composable
fun BackupEntry(
    isOnboarding: Boolean = false,
    onRestoreCompleted: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConsentResult(result.data) }

    LaunchedEffect(uiState.pendingConsentIntentSender) {
        val sender = uiState.pendingConsentIntentSender ?: return@LaunchedEffect
        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
        viewModel.onConsentIntentLaunched()
    }

    LaunchedEffect(uiState.restoreCompleted) {
        if (uiState.restoreCompleted) {
            onRestoreCompleted()
            viewModel.onRestoreCompletedConsumed()
        }
    }

    BackupScreen(
        uiState                 = uiState,
        isOnboarding             = isOnboarding,
        onConnectClick           = viewModel::onConnectClick,
        onBackupNowClick         = viewModel::onBackupNowClick,
        onRestoreClick           = viewModel::onRestoreClick,
        onDisconnectClick        = viewModel::onDisconnectClick,
        onDisconnectConfirm      = viewModel::onDisconnectConfirm,
        onDisconnectDismiss      = viewModel::onDisconnectDismiss,
        onReplaceRemoteConfirm   = viewModel::onReplaceRemoteConfirm,
        onConflictDismiss        = viewModel::onConflictDismiss,
        onErrorDismiss           = viewModel::onErrorDismiss,
        onMessageDismiss         = viewModel::onMessageDismiss,
    )
}
