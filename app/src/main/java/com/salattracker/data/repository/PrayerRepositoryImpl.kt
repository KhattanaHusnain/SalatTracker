package com.salattracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.salattracker.data.local.dao.*
import com.salattracker.data.local.entity.*
import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.data.remote.api.AlAdhanService
import com.salattracker.data.remote.dto.*
import com.salattracker.data.remote.firestore.FirestoreDataSource
import com.salattracker.data.sync.SyncOrchestrator
import com.salattracker.domain.model.*
import com.salattracker.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val prayerDao: PrayerDao,
    private val streakDao: StreakDao,
    private val prayerTimesDao: PrayerTimesDao,
    private val syncQueueDao: SyncQueueDao,
    private val firestoreDs: FirestoreDataSource,
    private val alAdhanService: AlAdhanService,
    private val settingsRepo: DataStoreSettingsRepository,
    private val auth: FirebaseAuth,
    private val syncOrchestrator: SyncOrchestrator,
) : PrayerRepository {

    // ── Auth helper ───────────────────────────────────────────────────────────

    private val userId: String
        get() = auth.currentUser?.email
            ?: throw IllegalStateException("User not signed in")

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
            syncState  = SyncState.PENDING,
        )
        prayerDao.upsertEntry(entity)

        val dto = entity.toDto()
        syncQueueDao.enqueue(
            SyncQueueEntity(
                opType         = "UPSERT",
                collectionPath = "users/$userId/prayers",
                documentId     = dto.documentId,
                payloadJson    = dto.toJson(),
            )
        )
        syncOrchestrator.triggerOneTimeSync()
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

    // ── Hijri ─────────────────────────────────────────────────────────────────

    override suspend fun getHijriLabel(date: LocalDate): String =
        "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.year} AH (approx)"

    // ── Streak ────────────────────────────────────────────────────────────────

    override suspend fun getStreakSnapshot(): Pair<Int, Int> {
        val snap = streakDao.getSnapshot(userId)
        return (snap?.currentStreak ?: 0) to (snap?.highestStreak ?: 0)
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

    // FIXED: parameter renamed from StatsPeriod enum to String to match domain interface
    override suspend fun getStreakPoints(from: LocalDate, to: LocalDate, periodLabel: String): List<StreakPoint> {
        val rows = prayerDao.getDailyCompletionPoints(userId, from, to)
        val fmt  = when (periodLabel) {
            "week"  -> DateTimeFormatter.ofPattern("EEE")
            "year"  -> DateTimeFormatter.ofPattern("MMM")
            else    -> DateTimeFormatter.ofPattern("d")    // month
        }
        val byDate = rows.associateBy { it.date }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { date ->
                StreakPoint(
                    label          = date.format(fmt),
                    completedCount = byDate[date]?.completedCount ?: 0,
                )
            }
            .toList()
    }

    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> =
        prayerDao.getPrayerBreakdown(userId, from, to).map { row ->
            PrayerBreakdown(
                prayerType     = row.prayerType,
                completionRate = if (row.total > 0) row.done.toFloat() / row.total else 0f,
            )
        }

    // ── Initial Firestore pull on login ───────────────────────────────────────

    suspend fun pullFromFirestoreOnLogin() {
        val prefs   = settingsRepo.getCurrentPrefs()
        val sinceMs = prefs.lastSyncAt
        val entries = if (sinceMs == 0L)
            firestoreDs.fetchAllEntries(userId)
        else
            firestoreDs.fetchEntriesUpdatedSince(userId, sinceMs)

        if (entries.isNotEmpty()) {
            prayerDao.upsertEntries(entries.map { it.toEntity() })
        }

        firestoreDs.fetchStreak(userId)?.let { dto ->
            streakDao.upsertSnapshot(
                StreakSnapshotEntity(
                    userId           = userId,
                    currentStreak    = dto.currentStreak,
                    highestStreak    = dto.highestStreak,
                    lastCalculatedAt = dto.updatedAt,
                )
            )
        }

        settingsRepo.updateLastSyncAt(System.currentTimeMillis())
    }

    // ── Streak recalculation ──────────────────────────────────────────────────

    private suspend fun recalculateAndCacheStreak() {
        val today    = LocalDate.now()
        var date     = today
        var current  = 0
        val existing = streakDao.getSnapshot(userId)
        var highest  = existing?.highestStreak ?: 0

        while (true) {
            val entries  = prayerDao.getEntriesForDate(userId, date)
            if (entries.isEmpty()) break
            val statuses = PrayerType.values().map { type ->
                entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED
            }
            when {
                statuses.all { it == PrayerStatus.MENSTRUATION } -> { /* neutral */ }
                statuses.all { it == PrayerStatus.CONGREGATION || it == PrayerStatus.INDIVIDUAL } -> {
                    current++
                    if (current > highest) highest = current
                }
                else -> break
            }
            date = date.minusDays(1)
        }

        streakDao.upsertSnapshot(
            StreakSnapshotEntity(
                userId           = userId,
                currentStreak    = current,
                highestStreak    = highest,
                lastCalculatedAt = System.currentTimeMillis(),
            )
        )
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

private fun PrayerEntryDto.toJson(): String =
    """{"userId":"$userId","date":"$date","prayerType":"$prayerType","status":"$status","updatedAt":$updatedAt}"""