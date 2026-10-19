package com.salattracker.data.repository

import android.os.Build
import com.salattracker.BuildConfig
import com.salattracker.data.backup.BACKUP_FORMAT_VERSION
import com.salattracker.data.backup.BackupCodec
import com.salattracker.data.backup.BackupFormatException
import com.salattracker.data.backup.BackupLocalSource
import com.salattracker.data.backup.BackupLocalState
import com.salattracker.data.backup.BackupMeta
import com.salattracker.data.backup.BackupStateStore
import com.salattracker.data.backup.DriveAuthorizer
import com.salattracker.data.backup.LocalBackupCounts
import com.salattracker.data.backup.RemoteBackupException
import com.salattracker.data.backup.RemoteBackupStore
import com.salattracker.data.backup.RemoteError
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.BackupResult
import com.salattracker.domain.model.BackupStatus
import com.salattracker.domain.model.ConnectResult
import com.salattracker.domain.model.RemoteBackupInfo
import com.salattracker.domain.model.RestoreResult
import com.salattracker.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Internal signal that Google needs the user's attention again — no UI to drive it from here. */
private object ConsentRequiredMarker : Exception()
private class AuthFailedException(message: String) : Exception(message)

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val driveAuthorizer: DriveAuthorizer,
    private val remoteStore: RemoteBackupStore,
    private val localSource: BackupLocalSource,
    private val backupStateStore: BackupStateStore,
) : BackupRepository {

    private val mutex = Mutex()

    override fun observeStatus(): Flow<BackupStatus> = backupStateStore.observe().map { s ->
        BackupStatus(
            connected        = s.connected,
            accountEmail     = s.accountEmail,
            lastBackupAt     = s.lastSuccessAt,
            lastBackupBytes  = s.lastBackupBytes,
            attention        = s.attention,
            remote           = s.remote,
            attentionNotified = s.attentionNotified,
        )
    }

    override suspend fun connect(accessToken: String): ConnectResult = mutex.withLock {
        try {
            val email = remoteStore.accountEmail(accessToken)
            val head = remoteStore.head(accessToken)
            val priorState = backupStateStore.current()

            backupStateStore.update { it.copy(connected = true, accountEmail = email, remote = head?.info) }

            when {
                head == null -> ConnectResult.NoBackup
                priorState.lastKnownRevisionId == head.info.revisionId -> ConnectResult.OwnBackupFound(head.info)
                else -> ConnectResult.ForeignBackupFound(head.info)
            }
        } catch (e: RemoteBackupException) {
            ConnectResult.Failed(e.message ?: "Could not connect to Google Drive")
        } catch (e: Exception) {
            ConnectResult.Failed(e.message ?: "Could not connect to Google Drive")
        }
    }

    override suspend fun backupNow(overwriteRemote: Boolean): BackupResult = mutex.withLock {
        val state = backupStateStore.current()
        if (!state.connected) return@withLock BackupResult.Disabled

        val result = withFreshToken(state.accountEmail) { token -> performBackup(token, state, overwriteRemote) }

        result.fold(
            onSuccess = { it },
            onFailure = { e -> mapFailureToResult(e, state) },
        )
    }

    override suspend fun restore(): RestoreResult = mutex.withLock {
        val state = backupStateStore.current()
        if (!state.connected) return@withLock RestoreResult.Failed("Not connected to a Google account")

        val result = withFreshToken(state.accountEmail) { token -> performRestore(token) }

        result.fold(
            onSuccess = { it },
            onFailure = { e ->
                when (e) {
                    is ConsentRequiredMarker -> {
                        setAttention(BackupAttention.SIGN_IN_REQUIRED)
                        RestoreResult.Failed("Google sign-in is required before restoring")
                    }
                    is AuthFailedException -> RestoreResult.Failed(e.message ?: "Sign-in failed")
                    is BackupFormatException -> RestoreResult.Failed(e.message ?: "Backup file is corrupt")
                    is RemoteBackupException -> RestoreResult.Failed(e.message ?: "Could not download backup")
                    else -> RestoreResult.Failed(e.message ?: "Restore failed")
                }
            },
        )
    }

    override suspend fun disconnect() {
        backupStateStore.clear()
    }

    override suspend fun markAttentionNotified() {
        backupStateStore.update { it.copy(attentionNotified = true) }
    }

    /** Resets [BackupLocalState.attentionNotified] whenever the attention reason actually changes, so the worker notifies once per new episode, not once per reason that happens to repeat. */
    private suspend fun setAttention(newAttention: BackupAttention, remote: RemoteBackupInfo? = null) {
        backupStateStore.update { current ->
            current.copy(
                attention         = newAttention,
                attentionNotified = if (current.attention == newAttention) current.attentionNotified else false,
                remote            = remote ?: current.remote,
            )
        }
    }

    // ── Backup ──────────────────────────────────────────────────────────────────

    private suspend fun performBackup(token: String, stateAtStart: BackupLocalState, overwriteRemote: Boolean): BackupResult {
        var state = stateAtStart
        val head = remoteStore.head(token)

        // Reconcile a pending upload whose response may have been lost (process death,
        // connection drop after Drive committed it) — adopt it as our lineage first.
        if (head != null && state.pendingRevisionId != null && head.info.revisionId == state.pendingRevisionId) {
            backupStateStore.update {
                it.copy(
                    lastKnownRevisionId = head.info.revisionId,
                    pendingRevisionId   = null,
                    lastSuccessAt        = System.currentTimeMillis(),
                    lastBackupBytes      = head.info.sizeBytes,
                    attention            = BackupAttention.NONE,
                    remote               = head.info,
                )
            }
            state = backupStateStore.current()
        }

        if (!overwriteRemote) {
            if (head != null && head.info.revisionId != state.lastKnownRevisionId) {
                setAttention(BackupAttention.REMOTE_CHANGED, head.info)
                return BackupResult.Blocked(BackupAttention.REMOTE_CHANGED, head.info)
            }

            val local = localSource.localCounts()
            if (head != null && isLocalDataMissing(local, head.info)) {
                setAttention(BackupAttention.LOCAL_DATA_MISSING, head.info)
                return BackupResult.Blocked(BackupAttention.LOCAL_DATA_MISSING, head.info)
            }
        }

        val snapshot = localSource.readSnapshot()
        val contentHash = BackupCodec.contentSha256(snapshot)

        if (head != null && head.info.revisionId == state.lastKnownRevisionId && head.info.contentSha256 == contentHash) {
            setAttention(BackupAttention.NONE, head.info)
            return BackupResult.UpToDate
        }

        val revisionId = UUID.randomUUID().toString()
        backupStateStore.update { it.copy(pendingRevisionId = revisionId) }

        val meta = BackupMeta(
            formatVersion    = BACKUP_FORMAT_VERSION,
            revisionId       = revisionId,
            createdAt        = System.currentTimeMillis(),
            appVersionCode   = BuildConfig.VERSION_CODE,
            deviceModel      = Build.MODEL ?: "",
            prayerEntryCount = snapshot.prayerEntries.size,
            oldestPrayerDate = snapshot.prayerEntries.minOfOrNull { it.date },
            contentSha256    = contentHash,
        )
        val bytes = BackupCodec.encode(snapshot, meta)
        val newHead = remoteStore.put(token, previous = head, bytes = bytes, meta = meta)

        backupStateStore.update {
            it.copy(
                lastKnownRevisionId = revisionId,
                pendingRevisionId   = null,
                lastSuccessAt        = System.currentTimeMillis(),
                lastBackupBytes      = bytes.size.toLong(),
                attention            = BackupAttention.NONE,
                remote               = newHead.info,
            )
        }
        return BackupResult.Uploaded(bytes.size.toLong())
    }

    private fun isLocalDataMissing(local: LocalBackupCounts, remote: RemoteBackupInfo): Boolean {
        if (local.prayerEntryCount < remote.prayerEntryCount) return true
        val remoteOldest = remote.oldestPrayerDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val localOldest = local.oldestPrayerDate ?: return remote.prayerEntryCount > 0
        return localOldest.isAfter(remoteOldest)
    }

    private suspend fun mapFailureToResult(e: Throwable, state: BackupLocalState): BackupResult = when (e) {
        is ConsentRequiredMarker -> {
            setAttention(BackupAttention.SIGN_IN_REQUIRED)
            BackupResult.Blocked(BackupAttention.SIGN_IN_REQUIRED, state.remote)
        }
        is AuthFailedException -> BackupResult.Failed(e.message ?: "Sign-in failed")
        is RemoteBackupException -> when (e.error) {
            RemoteError.Unauthorized -> {
                setAttention(BackupAttention.SIGN_IN_REQUIRED)
                BackupResult.Blocked(BackupAttention.SIGN_IN_REQUIRED, state.remote)
            }
            RemoteError.QuotaExceeded -> {
                setAttention(BackupAttention.STORAGE_FULL)
                BackupResult.Blocked(BackupAttention.STORAGE_FULL, state.remote)
            }
            RemoteError.RateLimited -> BackupResult.Retryable
            is RemoteError.ServerError -> BackupResult.Retryable
            is RemoteError.Other -> BackupResult.Failed(e.message ?: "Backup failed")
        }
        else -> BackupResult.Failed(e.message ?: "Backup failed")
    }

    // ── Restore ─────────────────────────────────────────────────────────────────

    private suspend fun performRestore(token: String): RestoreResult {
        val head = remoteStore.head(token) ?: return RestoreResult.NoBackup
        val bytes = remoteStore.download(token, head.fileId)
        val (meta, snapshot) = BackupCodec.decode(bytes)
        val replacedGoalIds = localSource.applySnapshot(snapshot)

        backupStateStore.update {
            it.copy(
                lastKnownRevisionId = meta.revisionId,
                pendingRevisionId   = null,
                lastSuccessAt        = System.currentTimeMillis(),
                lastBackupBytes      = bytes.size.toLong(),
                attention            = BackupAttention.NONE,
                attentionNotified    = false,
                remote               = head.info,
            )
        }
        return RestoreResult.Restored(language = snapshot.settings.language, replacedGoalIds = replacedGoalIds)
    }

    // ── Token helper: one silent authorize, one 401 retry ────────────────────────

    /**
     * Gets a token (silently, pinned to [accountEmail]) and runs [block]. On a 401 from
     * [block], clears the cached token and retries [block] once with a fresh one. When
     * Google needs the user again there is no UI to drive from here (the worker and the
     * repository have none), so that surfaces as [ConsentRequiredMarker] for the caller
     * to turn into [BackupAttention.SIGN_IN_REQUIRED].
     */
    private suspend fun <T> withFreshToken(accountEmail: String?, block: suspend (String) -> T): Result<T> {
        val outcome = driveAuthorizer.authorize(accountEmail)
        val token = when (outcome) {
            is DriveAuthorizer.Outcome.Token -> outcome.value
            is DriveAuthorizer.Outcome.ConsentRequired -> return Result.failure(ConsentRequiredMarker)
            is DriveAuthorizer.Outcome.Failed -> return Result.failure(AuthFailedException(outcome.message))
        }

        return try {
            Result.success(block(token))
        } catch (e: RemoteBackupException) {
            if (e.error == RemoteError.Unauthorized) {
                driveAuthorizer.invalidate(token)
                val retryOutcome = driveAuthorizer.authorize(accountEmail)
                val retryToken = (retryOutcome as? DriveAuthorizer.Outcome.Token)?.value
                    ?: return Result.failure(e)
                runCatching { block(retryToken) }
            } else {
                Result.failure(e)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
