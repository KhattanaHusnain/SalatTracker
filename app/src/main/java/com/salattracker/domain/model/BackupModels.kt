package com.salattracker.domain.model

/**
 * Why a backup run is blocked, or why the user needs to take action.
 * NONE means everything is fine (or backup is simply off).
 */
enum class BackupAttention {
    NONE,
    /** Google needs the user to sign in / re-consent before we can get a token. */
    SIGN_IN_REQUIRED,
    /** A remote backup exists that this install has never seen — could be a second device. */
    REMOTE_CHANGED,
    /** The device has less data than the remote backup — could be a destructive DB wipe. */
    LOCAL_DATA_MISSING,
    /** Drive reported the user's storage quota is full. */
    STORAGE_FULL,
}

/** What the remote backup file's metadata says, read with a single Drive files.list call. */
data class RemoteBackupInfo(
    val revisionId: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val prayerEntryCount: Int,
    val oldestPrayerDate: String?,   // ISO yyyy-MM-dd, null if there were no prayer entries
    val deviceModel: String,
    val contentSha256: String = "",
)

/** What the Backup screen and the drawer's attention dot observe. */
data class BackupStatus(
    val connected: Boolean = false,
    val accountEmail: String? = null,
    val lastBackupAt: Long? = null,
    val lastBackupBytes: Long? = null,
    val attention: BackupAttention = BackupAttention.NONE,
    val remote: RemoteBackupInfo? = null,
    /** Whether a notification has already been posted for the current [attention] episode. */
    val attentionNotified: Boolean = false,
)

sealed interface ConnectResult {
    /** No backup exists yet for this account. */
    data object NoBackup : ConnectResult
    /** A backup exists and this install already owns its lineage (safe to keep backing up). */
    data class OwnBackupFound(val info: RemoteBackupInfo) : ConnectResult
    /** A backup exists but this install has never seen it — user must choose Restore or Replace. */
    data class ForeignBackupFound(val info: RemoteBackupInfo) : ConnectResult
    data class Failed(val message: String) : ConnectResult
}

sealed interface BackupResult {
    data object Disabled : BackupResult
    data object UpToDate : BackupResult
    data class Uploaded(val bytes: Long) : BackupResult
    data class Blocked(val attention: BackupAttention, val remote: RemoteBackupInfo?) : BackupResult
    data object Retryable : BackupResult
    data class Failed(val message: String) : BackupResult
}

sealed interface RestoreResult {
    data class Restored(val language: AppLanguage, val replacedGoalIds: List<Long>) : RestoreResult
    data object NoBackup : RestoreResult
    data class Failed(val message: String) : RestoreResult
}
