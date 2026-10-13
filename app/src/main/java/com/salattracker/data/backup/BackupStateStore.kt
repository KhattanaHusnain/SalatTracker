package com.salattracker.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.RemoteBackupInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Local bookkeeping for the backup feature only — never shares a file with [androidx.datastore.preferences.preferencesDataStore] "salat_user_prefs". */
private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_state")

data class BackupLocalState(
    val connected: Boolean = false,
    val accountEmail: String? = null,
    /** The revision id this install last successfully uploaded or restored — its "lineage". */
    val lastKnownRevisionId: String? = null,
    /** Set right before an upload starts, so a lost response can still be reconciled with the remote head. */
    val pendingRevisionId: String? = null,
    val lastSuccessAt: Long? = null,
    val lastBackupBytes: Long? = null,
    val attention: BackupAttention = BackupAttention.NONE,
    /** Whether a notification has already been posted for the current [attention] episode. */
    val attentionNotified: Boolean = false,
    /** Cached copy of the remote file's metadata, refreshed on every connect()/backupNow() — lets
     * the UI show "backup found on another device" without an extra network round trip. */
    val remote: RemoteBackupInfo? = null,
)

interface BackupStateStore {
    fun observe(): Flow<BackupLocalState>
    suspend fun current(): BackupLocalState
    suspend fun update(transform: (BackupLocalState) -> BackupLocalState)
    suspend fun clear()
}

@Singleton
class DataStoreBackupStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupStateStore {

    private object Keys {
        val CONNECTED = booleanPreferencesKey("connected")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val LAST_KNOWN_REVISION_ID = stringPreferencesKey("last_known_revision_id")
        val PENDING_REVISION_ID = stringPreferencesKey("pending_revision_id")
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_BACKUP_BYTES = longPreferencesKey("last_backup_bytes")
        val ATTENTION = stringPreferencesKey("attention")
        val ATTENTION_NOTIFIED = booleanPreferencesKey("attention_notified")
        val REMOTE_REVISION_ID = stringPreferencesKey("remote_revision_id")
        val REMOTE_CREATED_AT = longPreferencesKey("remote_created_at")
        val REMOTE_SIZE_BYTES = longPreferencesKey("remote_size_bytes")
        val REMOTE_PRAYER_COUNT = intPreferencesKey("remote_prayer_count")
        val REMOTE_OLDEST_DATE = stringPreferencesKey("remote_oldest_date")
        val REMOTE_DEVICE_MODEL = stringPreferencesKey("remote_device_model")
    }

    override fun observe(): Flow<BackupLocalState> = context.backupDataStore.data.map { it.toState() }

    override suspend fun current(): BackupLocalState = context.backupDataStore.data.first().toState()

    override suspend fun update(transform: (BackupLocalState) -> BackupLocalState) {
        context.backupDataStore.edit { prefs ->
            val next = transform(prefs.toState())
            prefs[Keys.CONNECTED] = next.connected
            if (next.accountEmail != null) prefs[Keys.ACCOUNT_EMAIL] = next.accountEmail else prefs.remove(Keys.ACCOUNT_EMAIL)
            if (next.lastKnownRevisionId != null) prefs[Keys.LAST_KNOWN_REVISION_ID] = next.lastKnownRevisionId else prefs.remove(Keys.LAST_KNOWN_REVISION_ID)
            if (next.pendingRevisionId != null) prefs[Keys.PENDING_REVISION_ID] = next.pendingRevisionId else prefs.remove(Keys.PENDING_REVISION_ID)
            if (next.lastSuccessAt != null) prefs[Keys.LAST_SUCCESS_AT] = next.lastSuccessAt else prefs.remove(Keys.LAST_SUCCESS_AT)
            if (next.lastBackupBytes != null) prefs[Keys.LAST_BACKUP_BYTES] = next.lastBackupBytes else prefs.remove(Keys.LAST_BACKUP_BYTES)
            prefs[Keys.ATTENTION] = next.attention.name
            prefs[Keys.ATTENTION_NOTIFIED] = next.attentionNotified
            val remote = next.remote
            if (remote != null) {
                prefs[Keys.REMOTE_REVISION_ID] = remote.revisionId
                prefs[Keys.REMOTE_CREATED_AT] = remote.createdAt
                prefs[Keys.REMOTE_SIZE_BYTES] = remote.sizeBytes
                prefs[Keys.REMOTE_PRAYER_COUNT] = remote.prayerEntryCount
                if (remote.oldestPrayerDate != null) prefs[Keys.REMOTE_OLDEST_DATE] = remote.oldestPrayerDate else prefs.remove(Keys.REMOTE_OLDEST_DATE)
                prefs[Keys.REMOTE_DEVICE_MODEL] = remote.deviceModel
            } else {
                prefs.remove(Keys.REMOTE_REVISION_ID)
                prefs.remove(Keys.REMOTE_CREATED_AT)
                prefs.remove(Keys.REMOTE_SIZE_BYTES)
                prefs.remove(Keys.REMOTE_PRAYER_COUNT)
                prefs.remove(Keys.REMOTE_OLDEST_DATE)
                prefs.remove(Keys.REMOTE_DEVICE_MODEL)
            }
        }
    }

    override suspend fun clear() {
        context.backupDataStore.edit { it.clear() }
    }

    private fun Preferences.toState() = BackupLocalState(
        connected            = this[Keys.CONNECTED] ?: false,
        accountEmail         = this[Keys.ACCOUNT_EMAIL],
        lastKnownRevisionId  = this[Keys.LAST_KNOWN_REVISION_ID],
        pendingRevisionId    = this[Keys.PENDING_REVISION_ID],
        lastSuccessAt        = this[Keys.LAST_SUCCESS_AT],
        lastBackupBytes      = this[Keys.LAST_BACKUP_BYTES],
        attention            = this[Keys.ATTENTION]?.let { runCatching { BackupAttention.valueOf(it) }.getOrNull() } ?: BackupAttention.NONE,
        attentionNotified    = this[Keys.ATTENTION_NOTIFIED] ?: false,
        remote               = this[Keys.REMOTE_REVISION_ID]?.let { revisionId ->
            RemoteBackupInfo(
                revisionId       = revisionId,
                createdAt        = this[Keys.REMOTE_CREATED_AT] ?: 0L,
                sizeBytes        = this[Keys.REMOTE_SIZE_BYTES] ?: 0L,
                prayerEntryCount = this[Keys.REMOTE_PRAYER_COUNT] ?: 0,
                oldestPrayerDate = this[Keys.REMOTE_OLDEST_DATE],
                deviceModel      = this[Keys.REMOTE_DEVICE_MODEL] ?: "",
            )
        },
    )
}
