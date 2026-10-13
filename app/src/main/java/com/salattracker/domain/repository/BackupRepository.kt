package com.salattracker.domain.repository

import com.salattracker.domain.model.BackupResult
import com.salattracker.domain.model.BackupStatus
import com.salattracker.domain.model.ConnectResult
import com.salattracker.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for Google Drive backup/restore.
 *
 * File type: Interface — no Android/Play-Services/Drive imports here.
 * The implementation lives in data/repository/BackupRepositoryImpl and is
 * bound via Hilt in di/BackupModule.
 */
interface BackupRepository {

    /** Reactive status for the drawer's attention dot and the Backup screen. */
    fun observeStatus(): Flow<BackupStatus>

    /**
     * Links this account (identified by a Drive access token the caller already
     * obtained — see [com.salattracker.data.backup.DriveAuthorizer], used directly by
     * the Backup/Onboarding ViewModels to drive Google's sign-in UI, which a domain
     * interface can't do) and inspects the remote backup without uploading or
     * downloading anything.
     */
    suspend fun connect(accessToken: String): ConnectResult

    /**
     * Runs one backup pass: guards, hash check, upload if needed. Used by both the
     * daily worker and the "Back up now" button.
     *
     * [overwriteRemote] bypasses the lineage/shrink guards — only set true after the
     * user explicitly confirmed "Replace it with this device's data".
     */
    suspend fun backupNow(overwriteRemote: Boolean = false): BackupResult

    /** Downloads, validates and applies the remote backup, replacing all local data. */
    suspend fun restore(): RestoreResult

    /** Forgets the account locally. Does not revoke Drive access or delete the remote file. */
    suspend fun disconnect()

    /** Marks the current attention episode as already notified, so the daily worker posts at most one notification per episode. */
    suspend fun markAttentionNotified()
}
