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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

    // ── Analytics ─────────────────────────────────────────────────────────────

    override suspend fun getStatsForPeriod(from: LocalDate, to: LocalDate): PeriodStats {
        val row   = prayerDao.getAggregateStats(userId, from, to)
        val total = row.total.takeIf { it > 0 } ?: 1
        return PeriodStats(
            congregationPct = (row.congregation * 100) / total,
            individualPct   = (row.individual   * 100) / total,
            missedPct       = (row.missed        * 100) / total,
        )
    }

    override suspend fun getStatusTrendPoints(from: LocalDate, to: LocalDate, days: Int): List<StatusTrendPoint> {
        val byDate = prayerDao.getDailyStatusCounts(userId, from, to).associateBy { it.date }
        // (date, congregation, individual, qaza) for every day in range — missing days count as zero.
        val dailyPoints = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { date -> byDate[date] ?: DailyStatusCountRow(date, 0, 0, 0) }
            .toList()

        // Long ranges (> 120 days: 6-month, year): one point per month.
        if (days > 120) {
            val fmt = DateTimeFormatter.ofPattern("MMM")
            return dailyPoints
                .groupBy { YearMonth.from(it.date) }
                .toSortedMap()
                .map { (month, entries) -> entries.toTrendPoint(month.atDay(1).format(fmt)) }
        }

        // Medium ranges (41-120 days: 90-day): 7-day buckets counted backward from [to], so the
        // most recent bucket ends exactly on [to] (the oldest may be a partial week).
        if (days > 40) {
            val fmt = DateTimeFormatter.ofPattern("d MMM")
            return dailyPoints
                .groupBy { ChronoUnit.DAYS.between(it.date, to) / 7 }  // 0 = most recent week
                .toList()
                .sortedByDescending { (bucket, _) -> bucket }          // oldest bucket first
                .map { (_, entries) -> entries.toTrendPoint(entries.minOf { it.date }.format(fmt)) }
        }

        // Short ranges (≤ 40 days): one point per day. Two-line label — "d/M" over the weekday
        // abbreviation — matching the Daily Goals bar chart.
        val dateFmt = DateTimeFormatter.ofPattern("d/M")
        val nameFmt = DateTimeFormatter.ofPattern("EEE")
        return dailyPoints.map { listOf(it).toTrendPoint("${it.date.format(dateFmt)}\n${it.date.format(nameFmt)}") }
    }

    /** Sums the status counts of the bucket and converts them to percentages of recorded prayers. */
    private fun List<DailyStatusCountRow>.toTrendPoint(label: String): StatusTrendPoint {
        val cong = sumOf { it.congregation }
        val indiv = sumOf { it.individual }
        val qaza = sumOf { it.qaza }
        val total = (cong + indiv + qaza).takeIf { it > 0 } ?: 1
        return StatusTrendPoint(
            label           = label,
            congregationPct = cong * 100 / total,
            individualPct   = indiv * 100 / total,
            qazaPct         = qaza * 100 / total,
        )
    }

    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> =
        prayerDao.getPrayerBreakdown(userId, from, to).map { row ->
            PrayerBreakdown(
                prayerType     = row.prayerType,
                completionRate = if (row.total > 0) row.done.toFloat() / row.total else 0f,
            )
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
