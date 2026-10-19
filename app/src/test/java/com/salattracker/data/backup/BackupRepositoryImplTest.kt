package com.salattracker.data.backup

import com.salattracker.data.repository.BackupRepositoryImpl
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.model.BackupResult
import com.salattracker.domain.model.RestoreResult
import com.salattracker.domain.repository.UserPrefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class BackupRepositoryImplTest {

    private fun emptySnapshot() = BackupSnapshot(
        prayerEntries = emptyList(), goals = emptyList(), checkIns = emptyList(),
        highestStreaks = emptyMap(), settings = UserPrefs(),
    )

    private fun snapshotWithEntries(count: Int, from: LocalDate = LocalDate.of(2026, 1, 1)) = BackupSnapshot(
        prayerEntries = (0 until count).map {
            BackupPrayerEntry(from.plusDays(it.toLong()), com.salattracker.domain.model.PrayerType.FAJR, com.salattracker.domain.model.PrayerStatus.CONGREGATION, 1000L + it)
        },
        goals = emptyList(), checkIns = emptyList(), highestStreaks = emptyMap(), settings = UserPrefs(),
    )

    private lateinit var localSource: FakeBackupLocalSource
    private lateinit var remoteStore: FakeRemoteBackupStore
    private lateinit var stateStore: FakeBackupStateStore
    private lateinit var authorizer: FakeDriveAuthorizer
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        localSource = FakeBackupLocalSource(snapshotWithEntries(3))
        remoteStore = FakeRemoteBackupStore()
        stateStore = FakeBackupStateStore(BackupLocalState(connected = true, accountEmail = "test@example.com"))
        authorizer = FakeDriveAuthorizer()
        repository = BackupRepositoryImpl(authorizer, remoteStore, localSource, stateStore)
    }

    @Test
    fun `disabled when not connected`() = runTest {
        stateStore.update { it.copy(connected = false) }

        val result = repository.backupNow()

        assertEquals(BackupResult.Disabled, result)
        assertEquals(0, remoteStore.putCount)
    }

    @Test
    fun `first backup uploads and records lineage`() = runTest {
        val result = repository.backupNow()

        assertTrue(result is BackupResult.Uploaded)
        assertEquals(1, remoteStore.putCount)
        val state = stateStore.current()
        assertEquals(remoteStore.currentHead()!!.info.revisionId, state.lastKnownRevisionId)
        assertNull(state.pendingRevisionId)
        assertEquals(BackupAttention.NONE, state.attention)
    }

    @Test
    fun `unchanged data does not upload a second time`() = runTest {
        repository.backupNow() // first backup, uploads
        assertEquals(1, remoteStore.putCount)

        val second = repository.backupNow()

        assertEquals(BackupResult.UpToDate, second)
        assertEquals(1, remoteStore.putCount) // no new upload
    }

    @Test
    fun `lineage guard blocks an unseen remote revision, and an explicit replace overrides it`() = runTest {
        remoteStore.seedRemote(revisionId = "from-other-device", prayerEntryCount = 0)

        val blocked = repository.backupNow(overwriteRemote = false)
        assertTrue(blocked is BackupResult.Blocked)
        assertEquals(BackupAttention.REMOTE_CHANGED, (blocked as BackupResult.Blocked).attention)
        assertEquals(0, remoteStore.putCount)

        val replaced = repository.backupNow(overwriteRemote = true)
        assertTrue(replaced is BackupResult.Uploaded)
        assertEquals(1, remoteStore.putCount)
        assertEquals(remoteStore.currentHead()!!.info.revisionId, stateStore.current().lastKnownRevisionId)
    }

    @Test
    fun `shrink guard blocks when local has fewer prayer entries than the known remote backup`() = runTest {
        // Establish lineage first so the lineage guard itself does not fire.
        repository.backupNow()
        val knownRevision = stateStore.current().lastKnownRevisionId!!
        // Simulate the remote having more data than this device now has (e.g. after a local wipe).
        remoteStore.seedRemote(revisionId = knownRevision, prayerEntryCount = 999)
        localSource.snapshot = snapshotWithEntries(1)

        val result = repository.backupNow()

        assertTrue(result is BackupResult.Blocked)
        assertEquals(BackupAttention.LOCAL_DATA_MISSING, (result as BackupResult.Blocked).attention)
    }

    @Test
    fun `a pending revision whose upload response was lost is adopted without re-uploading`() = runTest {
        val snapshot = localSource.snapshot
        val hash = BackupCodec.contentSha256(snapshot)
        stateStore.update { it.copy(pendingRevisionId = "lost-response-revision") }
        remoteStore.seedRemote(revisionId = "lost-response-revision", prayerEntryCount = snapshot.prayerEntries.size, contentSha256 = hash)

        val result = repository.backupNow()

        assertEquals(BackupResult.UpToDate, result)
        assertEquals(0, remoteStore.putCount) // reconciled, never re-uploaded
        val state = stateStore.current()
        assertEquals("lost-response-revision", state.lastKnownRevisionId)
        assertNull(state.pendingRevisionId)
    }

    @Test
    fun `a failed upload leaves the remote and lineage untouched and is retryable`() = runTest {
        remoteStore.failNextWith = RemoteBackupException(RemoteError.ServerError(500), "boom")

        val result = repository.backupNow()

        assertEquals(BackupResult.Retryable, result)
        assertEquals(0, remoteStore.putCount)
        assertNull(stateStore.current().lastKnownRevisionId)
    }

    @Test
    fun `restore applies the downloaded snapshot and reports the goal ids it replaced`() = runTest {
        val existingGoal = com.salattracker.domain.model.GoalCategory.SALAT
        localSource.snapshot = localSource.snapshot.copy(
            goals = listOf(
                BackupGoal(
                    id = 3L, name = "Old goal", category = existingGoal, frequency = com.salattracker.domain.model.GoalFrequency.DAILY,
                    targetAmount = 1, unit = "Time(s)", reminderEnabled = false, reminderTime = null,
                    startDate = LocalDate.of(2025, 1, 1), durationDays = 10, isActive = true,
                )
            ),
        )
        val remoteSnapshot = emptySnapshot().copy(
            prayerEntries = listOf(BackupPrayerEntry(LocalDate.of(2026, 2, 1), com.salattracker.domain.model.PrayerType.ISHA, com.salattracker.domain.model.PrayerStatus.INDIVIDUAL, 2000L)),
        )
        val meta = BackupMeta(
            formatVersion = BACKUP_FORMAT_VERSION, revisionId = "remote-rev", createdAt = 1L, appVersionCode = 1,
            deviceModel = "Other Device", prayerEntryCount = 1, oldestPrayerDate = LocalDate.of(2026, 2, 1),
            contentSha256 = BackupCodec.contentSha256(remoteSnapshot),
        )
        remoteStore.put("setup-token", previous = null, bytes = BackupCodec.encode(remoteSnapshot, meta), meta = meta)

        val result = repository.restore()

        assertTrue(result is RestoreResult.Restored)
        assertEquals(listOf(3L), (result as RestoreResult.Restored).replacedGoalIds)
        assertEquals(remoteSnapshot.prayerEntries, localSource.snapshot.prayerEntries)
        assertEquals("remote-rev", stateStore.current().lastKnownRevisionId)
    }

    @Test
    fun `restore reports NoBackup when the account has nothing to restore`() = runTest {
        val result = repository.restore()

        assertEquals(RestoreResult.NoBackup, result)
    }

    @Test
    fun `restore catches corrupt backup exception gracefully without crashing`() = runTest {
        val remoteSnapshot = emptySnapshot()
        val meta = BackupMeta(
            formatVersion = BACKUP_FORMAT_VERSION, revisionId = "remote-rev", createdAt = 1L, appVersionCode = 1,
            deviceModel = "Other Device", prayerEntryCount = 0, oldestPrayerDate = null,
            contentSha256 = "invalid_hash_that_does_not_match",
        )
        remoteStore.put("setup-token", previous = null, bytes = BackupCodec.encode(remoteSnapshot, meta), meta = meta)

        val result = repository.restore()

        assertTrue(result is RestoreResult.Failed)
        assertTrue((result as RestoreResult.Failed).message.contains("corrupt"))
    }
}
