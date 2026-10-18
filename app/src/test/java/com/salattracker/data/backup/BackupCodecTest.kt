package com.salattracker.data.backup

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.UserPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupCodecTest {

    private fun sampleSnapshot() = BackupSnapshot(
        prayerEntries = listOf(
            BackupPrayerEntry(LocalDate.of(2026, 1, 1), PrayerType.FAJR, PrayerStatus.CONGREGATION, 1000L),
            BackupPrayerEntry(LocalDate.of(2026, 1, 1), PrayerType.DHUHR, PrayerStatus.INDIVIDUAL, 1001L),
        ),
        goals = listOf(
            BackupGoal(
                id = 5L, name = "Tahajjud", category = GoalCategory.SALAT, frequency = GoalFrequency.DAILY,
                targetAmount = 1, unit = "Time(s)", reminderEnabled = true, reminderTime = null,
                startDate = LocalDate.of(2026, 1, 1), durationDays = 30, isActive = true,
            ),
        ),
        checkIns = listOf(
            BackupCheckIn(goalId = 5L, date = LocalDate.of(2026, 1, 1), amount = 1),
            // Orphan check-in referring to a goal not present in [goals] — must be dropped on decode.
            BackupCheckIn(goalId = 999L, date = LocalDate.of(2026, 1, 1), amount = 1),
        ),
        highestStreaks = mapOf(PrayerType.FAJR to 12, PrayerType.DHUHR to 5),
        settings = UserPrefs(),
    )

    private fun sampleMeta(snapshot: BackupSnapshot) = BackupMeta(
        formatVersion    = BACKUP_FORMAT_VERSION,
        revisionId       = "rev-1",
        createdAt        = 123456789L,
        appVersionCode   = 1,
        deviceModel      = "Pixel 9",
        prayerEntryCount = snapshot.prayerEntries.size,
        oldestPrayerDate = snapshot.prayerEntries.minOfOrNull { it.date },
        contentSha256    = BackupCodec.contentSha256(snapshot),
    )

    @Test
    fun `round trip preserves prayer entries, goals, streaks and settings`() {
        val snapshot = sampleSnapshot()
        val meta = sampleMeta(snapshot)
        val bytes = BackupCodec.encode(snapshot, meta)

        val (decodedMeta, decodedSnapshot) = BackupCodec.decode(bytes)

        assertEquals(meta.revisionId, decodedMeta.revisionId)
        assertEquals(snapshot.prayerEntries, decodedSnapshot.prayerEntries)
        assertEquals(snapshot.goals, decodedSnapshot.goals)
        assertEquals(mapOf(PrayerType.FAJR to 12, PrayerType.DHUHR to 5, PrayerType.ASR to 0, PrayerType.MAGHRIB to 0, PrayerType.ISHA to 0), decodedSnapshot.highestStreaks)
        assertEquals(snapshot.settings, decodedSnapshot.settings)
    }

    @Test
    fun `round trip preserves weekdays, end-after and excused days`() {
        val base = sampleSnapshot()
        val snapshot = base.copy(
            goals = listOf(base.goals.single().copy(daysOfWeek = 0b0010010, endsAfterDuration = true)),
            checkIns = listOf(BackupCheckIn(goalId = 5L, date = LocalDate.of(2026, 1, 2), amount = 0, isSkip = true)),
        )
        val (_, decoded) = BackupCodec.decode(BackupCodec.encode(snapshot, sampleMeta(snapshot)))

        assertEquals(0b0010010, decoded.goals.single().daysOfWeek)
        assertTrue(decoded.goals.single().endsAfterDuration)
        assertTrue(decoded.checkIns.single().isSkip)
    }

    @Test
    fun `a backup written before these fields existed decodes to every-day ongoing goals`() {
        val json = """{"meta":{"formatVersion":1,"revisionId":"r","createdAt":1,"appVersionCode":1,"deviceModel":"x",
            "prayerEntryCount":0},"payload":{"prayerEntries":[],"goals":[{"id":1,"name":"G","category":"QURAN",
            "frequency":"DAILY","targetAmount":1,"unit":"Pages","reminderEnabled":false,"startDate":"2026-01-01",
            "durationDays":30,"isActive":true}],"checkIns":[{"goalId":1,"date":"2026-01-01","amount":2}],
            "streaks":[],"settings":{}}}""".trimIndent()
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        }.toByteArray()

        val (_, decoded) = BackupCodec.decode(bytes)

        assertEquals(0, decoded.goals.single().daysOfWeek)
        assertFalse(decoded.goals.single().endsAfterDuration)
        assertFalse(decoded.checkIns.single().isSkip)
    }

    @Test
    fun `a backup whose settings JSON still has dailyTipsEnabled decodes fine, field ignored`() {
        val json = """{"meta":{"formatVersion":1,"revisionId":"r","createdAt":1,"appVersionCode":1,"deviceModel":"x",
            "prayerEntryCount":0},"payload":{"prayerEntries":[],"goals":[],"checkIns":[],"streaks":[],
            "settings":{"useDeviceLocation":true,"dailyTipsEnabled":true}}}""".trimIndent()
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        }.toByteArray()

        val (_, decoded) = BackupCodec.decode(bytes)

        assertTrue(decoded.settings.useDeviceLocation)
    }

    @Test
    fun `round trip preserves saved mosques`() {
        val snapshot = sampleSnapshot().copy(
            mosques = listOf(
                Mosque(3, "Masjid Quba", 24.4393, 39.6175, 150),
                Mosque(4, "Al-Noor", 24.0, 39.0, 100),
            ),
        )
        val (_, decoded) = BackupCodec.decode(BackupCodec.encode(snapshot, sampleMeta(snapshot)))

        assertEquals(snapshot.mosques, decoded.mosques)
    }

    @Test
    fun `a backup written before Mosque Mode existed restores with no mosques`() {
        val json = """{"meta":{"formatVersion":1,"revisionId":"r","createdAt":1,"appVersionCode":1,"deviceModel":"x",
            "prayerEntryCount":0},"payload":{"prayerEntries":[],"goals":[],"checkIns":[],"streaks":[],
            "settings":{}}}""".trimIndent()
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        }.toByteArray()

        val (_, decoded) = BackupCodec.decode(bytes)

        assertTrue(decoded.mosques.isEmpty())
    }

    @Test
    fun `a mosque without coordinates is rejected rather than restored at 0,0`() {
        val json = """{"meta":{"formatVersion":1,"revisionId":"r","createdAt":1,"appVersionCode":1,"deviceModel":"x",
            "prayerEntryCount":0},"payload":{"prayerEntries":[],"goals":[],"checkIns":[],"streaks":[],
            "settings":{},"mosques":[{"id":1,"name":"No Place"}]}}""".trimIndent()
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        }.toByteArray()

        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(bytes) }
    }

    @Test
    fun `decode drops check-ins whose goal is not present in the same backup`() {
        val snapshot = sampleSnapshot()
        val bytes = BackupCodec.encode(snapshot, sampleMeta(snapshot))

        val (_, decoded) = BackupCodec.decode(bytes)

        assertEquals(1, decoded.checkIns.size)
        assertEquals(5L, decoded.checkIns.single().goalId)
    }

    @Test
    fun `content hash is deterministic and independent of meta`() {
        val snapshot = sampleSnapshot()
        val hash1 = BackupCodec.contentSha256(snapshot)
        val hash2 = BackupCodec.contentSha256(snapshot.copy()) // structurally identical, new instance

        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotBlank())
    }

    @Test
    fun `content hash changes when data changes`() {
        val snapshot = sampleSnapshot()
        val changed = snapshot.copy(
            prayerEntries = snapshot.prayerEntries + BackupPrayerEntry(LocalDate.of(2026, 1, 2), PrayerType.ASR, PrayerStatus.CONGREGATION, 1002L),
        )

        assertFalse(BackupCodec.contentSha256(snapshot) == BackupCodec.contentSha256(changed))
    }

    @Test
    fun `decode rejects a format version newer than this app supports`() {
        val snapshot = sampleSnapshot()
        val futureMeta = sampleMeta(snapshot).copy(formatVersion = BACKUP_FORMAT_VERSION + 1)
        val bytes = BackupCodec.encode(snapshot, futureMeta)

        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(bytes) }
    }

    @Test
    fun `decode rejects bytes that are not valid gzip`() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("not gzip".toByteArray()) }
    }

    @Test
    fun `decode rejects a tampered payload whose hash no longer matches`() {
        val snapshot = sampleSnapshot()
        val bytes = BackupCodec.encode(snapshot, sampleMeta(snapshot))
        // Flip a byte well inside the gzip stream to corrupt the decompressed JSON without
        // breaking gzip framing outright (which is covered by the "not valid gzip" case above).
        val tampered = bytes.copyOf()
        tampered[tampered.size - 5] = (tampered[tampered.size - 5] + 1).toByte()

        // Either the gzip/JSON itself breaks, or it parses but the hash check catches it —
        // both surface as BackupFormatException, never a silent bad decode.
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(tampered) }
    }

    @Test
    fun `empty snapshot round trips with no entries`() {
        val empty = BackupSnapshot(
            prayerEntries = emptyList(), goals = emptyList(), checkIns = emptyList(),
            highestStreaks = emptyMap(), settings = UserPrefs(),
        )
        val bytes = BackupCodec.encode(empty, sampleMeta(empty))

        val (meta, decoded) = BackupCodec.decode(bytes)

        assertTrue(decoded.prayerEntries.isEmpty())
        assertNull(meta.oldestPrayerDate)
    }
}
