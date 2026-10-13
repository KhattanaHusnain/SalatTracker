package com.salattracker.data.backup

import android.content.Intent
import com.salattracker.domain.model.RemoteBackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** In-memory [BackupLocalSource] — no Room, no Android. */
class FakeBackupLocalSource(initial: BackupSnapshot) : BackupLocalSource {

    var snapshot: BackupSnapshot = initial
    val appliedSnapshots = mutableListOf<BackupSnapshot>()

    override suspend fun readSnapshot(): BackupSnapshot = snapshot

    override suspend fun localCounts(): LocalBackupCounts = LocalBackupCounts(
        prayerEntryCount = snapshot.prayerEntries.size,
        oldestPrayerDate = snapshot.prayerEntries.minOfOrNull { it.date },
    )

    override suspend fun applySnapshot(snapshot: BackupSnapshot): List<Long> {
        val previousGoalIds = this.snapshot.goals.map { it.id }
        appliedSnapshots += snapshot
        this.snapshot = snapshot
        return previousGoalIds
    }
}

/** In-memory [RemoteBackupStore] with one backup "slot" and injectable failures. */
class FakeRemoteBackupStore(private val email: String = "test@example.com") : RemoteBackupStore {

    private var head: RemoteHead? = null
    private var bytes: ByteArray = ByteArray(0)
    var putCount = 0
    var failNextWith: RemoteBackupException? = null

    override suspend fun accountEmail(token: String): String = email

    override suspend fun head(token: String): RemoteHead? = head

    override suspend fun download(token: String, fileId: String): ByteArray = bytes

    override suspend fun put(token: String, previous: RemoteHead?, bytes: ByteArray, meta: BackupMeta): RemoteHead {
        failNextWith?.let { failure -> failNextWith = null; throw failure }
        putCount++
        this.bytes = bytes
        val newHead = RemoteHead(
            fileId = previous?.fileId ?: "file-1",
            info = RemoteBackupInfo(
                revisionId = meta.revisionId, createdAt = meta.createdAt, sizeBytes = bytes.size.toLong(),
                prayerEntryCount = meta.prayerEntryCount, oldestPrayerDate = meta.oldestPrayerDate?.toString(),
                deviceModel = meta.deviceModel, contentSha256 = meta.contentSha256,
            ),
            md5Checksum = null,
        )
        head = newHead
        return newHead
    }

    /** Test seam: simulate a backup already sitting on the server, as if from another device/install. */
    fun seedRemote(revisionId: String, prayerEntryCount: Int = 0, oldestPrayerDate: LocalDate? = null, contentSha256: String = "seed-hash") {
        head = RemoteHead(
            fileId = "file-1",
            info = RemoteBackupInfo(
                revisionId = revisionId, createdAt = 0L, sizeBytes = 0L, prayerEntryCount = prayerEntryCount,
                oldestPrayerDate = oldestPrayerDate?.toString(), deviceModel = "Other Device", contentSha256 = contentSha256,
            ),
            md5Checksum = null,
        )
    }

    fun currentHead(): RemoteHead? = head
}

/** In-memory [BackupStateStore]. */
class FakeBackupStateStore(initial: BackupLocalState = BackupLocalState()) : BackupStateStore {
    private val flow = MutableStateFlow(initial)

    override fun observe(): StateFlow<BackupLocalState> = flow
    override suspend fun current(): BackupLocalState = flow.value
    override suspend fun update(transform: (BackupLocalState) -> BackupLocalState) {
        flow.value = transform(flow.value)
    }
    override suspend fun clear() {
        flow.value = BackupLocalState()
    }
}

/** Always authorizes silently with a fixed token — never returns ConsentRequired, since
 * that path needs a real android.content.IntentSender and is exercised by the UI instead. */
class FakeDriveAuthorizer(private val token: String = "fake-token") : DriveAuthorizer {
    var invalidatedTokens = mutableListOf<String>()

    override suspend fun authorize(accountEmail: String?, forceAccountChooser: Boolean): DriveAuthorizer.Outcome =
        DriveAuthorizer.Outcome.Token(token)

    override fun completeConsent(data: Intent?): DriveAuthorizer.Outcome =
        DriveAuthorizer.Outcome.Failed("not used in these tests")

    override suspend fun invalidate(token: String) {
        invalidatedTokens += token
    }
}
