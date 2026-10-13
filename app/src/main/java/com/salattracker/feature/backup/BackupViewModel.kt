package com.salattracker.feature.backup

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.data.backup.DriveAuthorizer
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.BackupResult
import com.salattracker.domain.model.ConnectResult
import com.salattracker.domain.model.RemoteBackupInfo
import com.salattracker.domain.model.RestoreResult
import com.salattracker.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = true,
    val connected: Boolean = false,
    val accountEmail: String? = null,
    val lastBackupAt: Long? = null,
    val lastBackupBytes: Long? = null,
    val attention: BackupAttention = BackupAttention.NONE,
    val remote: RemoteBackupInfo? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** A conflict (remote changed / local data missing) the user must resolve explicitly. */
    val conflictInfo: RemoteBackupInfo? = null,
    val showDisconnectConfirm: Boolean = false,
    /** Non-null while the consent sheet needs to be launched by the Entry's ActivityResult launcher. */
    val pendingConsentIntentSender: IntentSender? = null,
    /** One-off signal: a restore just succeeded — onboarding uses this to finish itself. */
    val restoreCompleted: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val driveAuthorizer: DriveAuthorizer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        connected        = status.connected,
                        accountEmail     = status.accountEmail,
                        lastBackupAt     = status.lastBackupAt,
                        lastBackupBytes  = status.lastBackupBytes,
                        attention        = status.attention,
                        remote           = status.remote,
                    )
                }
            }
        }
    }

    // ── Connect / switch account ─────────────────────────────────────────────────

    /** Always forces Google's account chooser — used for both the first "Connect" and "Switch account". */
    fun onConnectClick() {
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = driveAuthorizer.authorize(accountEmail = null, forceAccountChooser = true)) {
                is DriveAuthorizer.Outcome.ConsentRequired ->
                    _uiState.update { it.copy(pendingConsentIntentSender = outcome.intentSender) }
                is DriveAuthorizer.Outcome.Token -> finishConnect(outcome.value)
                is DriveAuthorizer.Outcome.Failed ->
                    _uiState.update { it.copy(isWorking = false, error = outcome.message) }
            }
        }
    }

    /** Called by the Entry once the consent sheet launcher returns. */
    fun onConsentIntentLaunched() {
        _uiState.update { it.copy(pendingConsentIntentSender = null) }
    }

    fun onConsentResult(data: Intent?) {
        when (val outcome = driveAuthorizer.completeConsent(data)) {
            is DriveAuthorizer.Outcome.Token -> finishConnect(outcome.value)
            is DriveAuthorizer.Outcome.ConsentRequired ->
                _uiState.update { it.copy(isWorking = false, error = "Sign-in was not completed") }
            is DriveAuthorizer.Outcome.Failed ->
                _uiState.update { it.copy(isWorking = false, error = outcome.message) }
        }
    }

    private fun finishConnect(accessToken: String) {
        viewModelScope.launch {
            when (val result = backupRepository.connect(accessToken)) {
                is ConnectResult.NoBackup -> {
                    _uiState.update { it.copy(isWorking = false, message = "Connected. Taking your first backup…") }
                    runBackup(overwriteRemote = false)
                }
                is ConnectResult.OwnBackupFound -> {
                    _uiState.update { it.copy(isWorking = false, message = "Connected") }
                }
                is ConnectResult.ForeignBackupFound ->
                    _uiState.update { it.copy(isWorking = false, conflictInfo = result.info) }
                is ConnectResult.Failed ->
                    _uiState.update { it.copy(isWorking = false, error = result.message) }
            }
        }
    }

    // ── Backup / restore ──────────────────────────────────────────────────────────

    fun onBackupNowClick() = runBackup(overwriteRemote = false)

    /** User explicitly chose "Replace it with this device's data" after seeing a conflict. */
    fun onReplaceRemoteConfirm() {
        _uiState.update { it.copy(conflictInfo = null) }
        runBackup(overwriteRemote = true)
    }

    /** User chose "Restore this backup" after seeing a conflict (or from the Backup screen's Restore action). */
    fun onRestoreClick() {
        _uiState.update { it.copy(isWorking = true, error = null, conflictInfo = null) }
        viewModelScope.launch {
            when (val result = backupRepository.restore()) {
                is RestoreResult.Restored ->
                    _uiState.update { it.copy(isWorking = false, message = "Restored from backup", restoreCompleted = true) }
                is RestoreResult.NoBackup ->
                    _uiState.update { it.copy(isWorking = false, error = "No backup found for this account") }
                is RestoreResult.Failed ->
                    _uiState.update { it.copy(isWorking = false, error = result.message) }
            }
        }
    }

    private fun runBackup(overwriteRemote: Boolean) {
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val result = backupRepository.backupNow(overwriteRemote)) {
                is BackupResult.Disabled ->
                    _uiState.update { it.copy(isWorking = false) }
                is BackupResult.UpToDate ->
                    _uiState.update { it.copy(isWorking = false, message = "Already up to date") }
                is BackupResult.Uploaded ->
                    _uiState.update { it.copy(isWorking = false, message = "Backup saved") }
                is BackupResult.Blocked -> when (result.attention) {
                    BackupAttention.REMOTE_CHANGED, BackupAttention.LOCAL_DATA_MISSING ->
                        _uiState.update { it.copy(isWorking = false, conflictInfo = result.remote) }
                    BackupAttention.SIGN_IN_REQUIRED ->
                        _uiState.update { it.copy(isWorking = false, error = "Google sign-in is required — tap Connect to continue") }
                    BackupAttention.STORAGE_FULL ->
                        _uiState.update { it.copy(isWorking = false, error = "Your Google Drive storage is full") }
                    BackupAttention.NONE -> _uiState.update { it.copy(isWorking = false) }
                }
                is BackupResult.Retryable ->
                    _uiState.update { it.copy(isWorking = false, error = "Could not reach Google Drive — will try again later") }
                is BackupResult.Failed ->
                    _uiState.update { it.copy(isWorking = false, error = result.message) }
            }
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────────────

    fun onDisconnectClick() = _uiState.update { it.copy(showDisconnectConfirm = true) }
    fun onDisconnectDismiss() = _uiState.update { it.copy(showDisconnectConfirm = false) }

    fun onDisconnectConfirm() {
        _uiState.update { it.copy(showDisconnectConfirm = false, isWorking = true) }
        viewModelScope.launch {
            backupRepository.disconnect()
            _uiState.update { it.copy(isWorking = false, message = "Disconnected") }
        }
    }

    // ── Dismissals ────────────────────────────────────────────────────────────────

    fun onConflictDismiss() = _uiState.update { it.copy(conflictInfo = null) }
    fun onErrorDismiss() = _uiState.update { it.copy(error = null) }
    fun onMessageDismiss() = _uiState.update { it.copy(message = null) }
    fun onRestoreCompletedConsumed() = _uiState.update { it.copy(restoreCompleted = false) }
}
