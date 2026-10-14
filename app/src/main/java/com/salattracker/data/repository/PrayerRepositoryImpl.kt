package com.salattracker.data.repository

import com.salattracker.data.local.dao.*
import com.salattracker.data.local.entity.*
import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.data.remote.api.AlAdhanService
import com.salattracker.domain.model.*
import com.salattracker.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Single-device, no-login app — every prayer entry belongs to this constant local user. */
const val LOCAL_USER_ID = "local"

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val prayerDao: PrayerDao,
    private val streakDao: StreakDao,
    private val prayerTimesDao: PrayerTimesDao,
    private val alAdhanService: AlAdhanService,
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

    override suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>? {
        val prefs  = settingsRepo.getCurrentPrefs()
        val cached = prayerTimesDao.getCached(date, prefs.latitude, prefs.longitude, prefs.calculationMethodId)
        if (cached != null) return cached.toTimesMap()

        return try {
            val response = alAdhanService.getTimings(date.toString(), prefs.latitude, prefs.longitude, prefs.calculationMethodId)
            val timings  = response.data?.timings ?: return null
            val entity   = PrayerTimeCacheEntity(
                date      = date,
                latitude  = prefs.latitude,
                longitude = prefs.longitude,
                method    = prefs.calculationMethodId,
                fajr      = timings["Fajr"]    ?: "",
                dhuhr     = timings["Dhuhr"]   ?: "",
                asr       = timings["Asr"]     ?: "",
                maghrib   = timings["Maghrib"] ?: "",
                isha      = timings["Isha"]    ?: "",
            )
            prayerTimesDao.insertCache(entity)
            entity.toTimesMap()
        } catch (e: Exception) {
            null
        }
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

    // ── Analytics ──────────────────────────────────────────────────────────────

    override suspend fun getDailyStatusCounts(from: LocalDate, to: LocalDate): List<DayStatusCounts> =
        prayerDao.getDailyStatusCounts(userId, from, to).map { row ->
            DayStatusCounts(date = row.date, counts = row.toCounts())
        }

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

    private class StreakTrack(var highest: Int) {
        var current: Int = 0
        var active: Boolean = true
    }

    private suspend fun recalculateAndCacheStreak() {
        val today = LocalDate.now()
        val existing = streakDao.getAllSnapshots(userId).associateBy { it.prayerType }
        val tracks = PrayerType.values().associateWith { type ->
            StreakTrack(highest = existing[type]?.highestStreak ?: 0)
        }

        var date = today
        var isToday = true
        while (tracks.values.any { it.active }) {
            val entries = prayerDao.getEntriesForDate(userId, date)

            PrayerType.values().forEach { type ->
                val track = tracks.getValue(type)
                if (!track.active) return@forEach

                when (entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED) {
                    PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL -> {
                        track.current++
                        if (track.current > track.highest) track.highest = track.current
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
                    highestStreak    = track.highest,
                    lastCalculatedAt = System.currentTimeMillis(),
                )
            )
        }
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

private fun PrayerTimeCacheEntity.toTimesMap(): Map<PrayerType, LocalTime> {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    fun parse(s: String): LocalTime? = try { LocalTime.parse(s.take(5), fmt) } catch (e: Exception) { null }
    return buildMap {
        parse(fajr)?.let    { put(PrayerType.FAJR,    it) }
        parse(dhuhr)?.let   { put(PrayerType.DHUHR,   it) }
        parse(asr)?.let     { put(PrayerType.ASR,     it) }
        parse(maghrib)?.let { put(PrayerType.MAGHRIB, it) }
        parse(isha)?.let    { put(PrayerType.ISHA,    it) }
    }
}
