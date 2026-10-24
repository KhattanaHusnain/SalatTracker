package com.salattracker.data.repository

import com.salattracker.data.local.dao.*
import com.salattracker.data.local.entity.*
import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.domain.model.*
import com.salattracker.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Single-device, no-login app — every prayer entry belongs to this constant local user. */
const val LOCAL_USER_ID = "local"

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val prayerDao: PrayerDao,
    private val streakDao: StreakDao,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepo: DataStoreSettingsRepository,
) : PrayerRepository {

    private val userId: String get() = LOCAL_USER_ID

    // ── Today / single day ────────────────────────────────────────────────────

    override fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>> =
        prayerDao.observeEntriesForDate(userId, date).map { it.map { e -> e.toDomain() } }

    override suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry> =
        prayerDao.getEntriesForDate(userId, date).map { it.toDomain() }

    override suspend fun upsertPrayerStatus(
        date: LocalDate,
        prayerType: PrayerType,
        status: PrayerStatus,
    ) {
        val existing = prayerDao.getEntry(userId, date, prayerType)
        val entity = PrayerEntryEntity(
            id         = existing?.id ?: 0,
            userId     = userId,
            date       = date,
            prayerType = prayerType,
            status     = status,
            updatedAt  = System.currentTimeMillis(),
        )
        prayerDao.upsertEntry(entity)
        recalculateAndCacheStreak()
    }

    override suspend fun bulkUpsertPrayerStatus(
        entries: List<Pair<LocalDate, PrayerType>>,
        status: PrayerStatus,
    ) {
        val now = System.currentTimeMillis()
        val entities = entries.map { (date, type) ->
            PrayerEntryEntity(
                userId     = userId,
                date       = date,
                prayerType = type,
                status     = status,
                updatedAt  = now,
            )
        }
        prayerDao.upsertEntries(entities)
        recalculateAndCacheStreak()
    }

    // ── Month range ───────────────────────────────────────────────────────────

    override fun observeMonthEntries(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<PrayerEntry>>> =
        prayerDao.observeEntriesForRange(userId, from, to).map { list ->
            list.map { it.toDomain() }.groupBy { it.date }
        }

    // ── Prayer times ──────────────────────────────────────────────────────────

    override suspend fun getPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>? {
        val prefs = settingsRepo.getCurrentPrefs()
        return prayerTimeCalculator.calculate(date, prefs.latitude, prefs.longitude, prefs)
    }

    override suspend fun getSunriseTime(date: LocalDate): LocalTime? {
        val prefs = settingsRepo.getCurrentPrefs()
        return prayerTimeCalculator.calculateSunrise(date, prefs.latitude, prefs.longitude, prefs)
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    override suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo> {
        val byType = streakDao.getAllSnapshots(userId).associateBy { it.prayerType }
        return PrayerType.values().associateWith { type ->
            val snap = byType[type]
            StreakInfo(currentStreak = snap?.currentStreak ?: 0, highestStreak = snap?.highestStreak ?: 0)
        }
    }

    override fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>> =
        streakDao.observeAllSnapshots(userId).map { snapshots ->
            val byType = snapshots.associateBy { it.prayerType }
            PrayerType.values().associateWith { type ->
                val snap = byType[type]
                StreakInfo(currentStreak = snap?.currentStreak ?: 0, highestStreak = snap?.highestStreak ?: 0)
            }
        }

    // ── Qaza ──────────────────────────────────────────────────────────────────

    override fun observeQazaBalance(): Flow<Map<PrayerType, Int>> =
        prayerDao.observeQazaBalance(userId).map { rows ->
            rows.associate { it.prayerType to it.pendingCount }
        }

    override fun observePendingQazaEntries(): Flow<List<PrayerEntry>> =
        prayerDao.observePendingQaza(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType) =
        upsertPrayerStatus(date, prayerType, PrayerStatus.QAZA_COMPLETED)

    override fun observeQazaMadeUpCount(): Flow<Int> =
        prayerDao.observeQazaMadeUpCount(userId)

    override fun observeQazaMadeUpSince(since: LocalDate): Flow<List<PrayerEntry>> {
        val sinceMillis = since.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return prayerDao.observeQazaMadeUpSince(userId, sinceMillis).map { it.map { e -> e.toDomain() } }
    }

    override fun observeRecentQazaMadeUp(limit: Int): Flow<List<PrayerEntry>> =
        prayerDao.observeRecentQazaMadeUp(userId, limit).map { it.map { e -> e.toDomain() } }

    override suspend fun getQazaHistory(): List<PrayerEntry> =
        prayerDao.getQazaHistory(userId).map { it.toDomain() }

    // ── Analytics ──────────────────────────────────────────────────────────────

    override suspend fun getDailyStatusCounts(from: LocalDate, to: LocalDate): List<DayStatusCounts> =
        prayerDao.getDailyStatusCounts(userId, from, to).map { row ->
            DayStatusCounts(date = row.date, counts = row.toCounts())
        }

    override suspend fun getFirstRecordedPrayerDate(): LocalDate? =
        prayerDao.getFirstRecordedPrayerDate(userId)

    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> {
        val byType = prayerDao.getPrayerBreakdown(userId, from, to).associateBy { it.prayerType }
        // Always 5 rows, in PrayerType declaration order — the DAO's GROUP BY prayer_type has no
        // ORDER BY, so SQLite returns rows sorted alphabetically by key; a prayer with nothing
        // recorded in range also needs a (zero) row rather than being silently dropped.
        return PrayerType.values().map { type ->
            PrayerBreakdown(prayerType = type, counts = byType[type]?.toCounts() ?: StatusCounts())
        }
    }

    // ── Streak recalculation ──────────────────────────────────────────────────
    // Each of the 5 prayer types gets its own independent streak, walked
    // backward day-by-day from today until every type has "locked in" (hit a
    // breaking day). One DAO query per date, shared across all still-active types.

    override suspend fun recalculateStreaks() = recalculateAndCacheStreak()

    private class StreakTrack {
        var current: Int = 0
        var active: Boolean = true
    }

    private suspend fun recalculateAndCacheStreak() {
        val today = LocalDate.now()
        val tracks = PrayerType.values().associateWith { StreakTrack() }
        // One ordered history read serves both current and best streaks. Previously this loop
        // issued one SQL query for every day it walked backwards through the user's history.
        val allEntries = prayerDao.getAllEntries(userId)
        val entriesByDate = allEntries.groupBy { it.date }
        // Never seed a best streak from its old cache. A corrected historic entry (or a
        // restored backup) must be able to lower it as well as raise it.
        val highestByType = calculateHighestStreaks(allEntries, today)

        var date = today
        var isToday = true
        while (tracks.values.any { it.active }) {
            val entries = entriesByDate[date].orEmpty()

            PrayerType.values().forEach { type ->
                val track = tracks.getValue(type)
                if (!track.active) return@forEach

                when (entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED) {
                    PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL -> {
                        track.current++
                    }
                    PrayerStatus.MENSTRUATION -> { /* neutral: doesn't count, doesn't break */ }
                    PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED -> {
                        track.active = false
                    }
                    PrayerStatus.NOT_RECORDED -> {
                        // Today: not yet resolved (not due yet, or awaiting the auto-Qaza
                        // worker) — leave the streak as-is until it's actually decided.
                        // Any earlier day: no record ever landed here, so it's a break.
                        if (!isToday) track.active = false
                    }
                }
            }

            date = date.minusDays(1)
            isToday = false
        }

        tracks.forEach { (type, track) ->
            streakDao.upsertSnapshot(
                StreakSnapshotEntity(
                    userId           = userId,
                    prayerType       = type,
                    currentStreak    = track.current,
                    highestStreak    = highestByType.getValue(type),
                    lastCalculatedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}

/**
 * Calculates each prayer's longest contiguous prayed run from the persisted history.
 * Menstruation is neutral, matching the current-streak rules: it neither extends nor breaks a
 * run. Missing rows and Qaza statuses break it. Keeping this independent of the snapshot cache
 * is what lets a correction reduce an inflated historical best.
 */
internal fun calculateHighestStreaks(
    entries: List<PrayerEntryEntity>,
    today: LocalDate,
): Map<PrayerType, Int> {
    val firstDate = entries.minOfOrNull { it.date }
        ?: return PrayerType.values().associateWith { 0 }
    val statusesByDate = entries.groupBy { it.date }.mapValues { (_, entriesForDate) ->
        entriesForDate.associate { it.prayerType to it.status }
    }

    return PrayerType.values().associateWith { type ->
        var current = 0
        var highest = 0
        var date = firstDate
        while (!date.isAfter(today)) {
            when (statusesByDate[date]?.get(type) ?: PrayerStatus.NOT_RECORDED) {
                PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL -> {
                    current++
                    highest = maxOf(highest, current)
                }
                PrayerStatus.MENSTRUATION -> Unit
                PrayerStatus.NOT_RECORDED, PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED -> current = 0
            }
            date = date.plusDays(1)
        }
        highest
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun DailyStatusCountRow.toCounts() = StatusCounts(
    congregation = congregation, individual = individual,
    qazaPending = qazaPending, qazaCompleted = qazaCompleted, excused = excused,
)

private fun PrayerBreakdownRow.toCounts() = StatusCounts(
    congregation = congregation, individual = individual,
    qazaPending = qazaPending, qazaCompleted = qazaCompleted, excused = excused,
)

private fun PrayerEntryEntity.toDomain() = PrayerEntry(
    id = id, userId = userId, date = date,
    prayerType = prayerType, status = status, updatedAt = updatedAt,
)
