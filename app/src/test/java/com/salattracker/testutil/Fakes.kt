package com.salattracker.testutil

import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.DayStatusCounts
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.repository.VerseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * In-memory [PrayerRepository] exercising only what CalendarViewModel/StatsViewModel actually
 * call. Everything else throws [NotImplementedError], so a future ViewModel change that starts
 * relying on an unfaked method fails its test loudly instead of silently no-op'ing.
 */
class FakePrayerRepository : PrayerRepository {

    private val entries = MutableStateFlow<Map<LocalDate, Map<PrayerType, PrayerStatus>>>(emptyMap())
    private val updatedAtMap = mutableMapOf<Pair<LocalDate, PrayerType>, Long>()
    private val cachedTimes = mutableMapOf<LocalDate, Map<PrayerType, LocalTime>>()
    private val streaksFlow = MutableStateFlow<Map<PrayerType, StreakInfo>>(emptyMap())

    /** Fake "now" used to stamp updatedAt on writes made through [markQazaCompleted] /
     * [bulkUpsertPrayerStatus] — tests control it directly to exercise pace-window filtering. */
    var now: Long = 0L

    var observeMonthEntriesCallCount = 0
        private set
    val upserts = mutableListOf<Triple<LocalDate, PrayerType, PrayerStatus>>()
    val bulkUpserts = mutableListOf<Pair<List<Pair<LocalDate, PrayerType>>, PrayerStatus>>()

    // ── Stats seeding ───────────────────────────────────────────────────────
    var dailyCounts: Map<LocalDate, StatusCounts> = emptyMap()
    var prayerBreakdown: List<PrayerBreakdown> = emptyList()

    val dailyStatusCountsCalls = mutableListOf<Pair<LocalDate, LocalDate>>()
    val prayerBreakdownCalls = mutableListOf<Pair<LocalDate, LocalDate>>()

    fun seedStatus(date: LocalDate, type: PrayerType, status: PrayerStatus, updatedAt: Long = 0L) {
        entries.value = entries.value.toMutableMap().apply {
            this[date] = (this[date].orEmpty()) + (type to status)
        }
        updatedAtMap[date to type] = updatedAt
    }

    private fun setStatus(date: LocalDate, type: PrayerType, status: PrayerStatus) {
        seedStatus(date, type, status, updatedAt = now)
    }

    private fun allEntriesWithStatus(status: PrayerStatus): List<PrayerEntry> =
        entries.value.flatMap { (date, byType) ->
            byType.filter { it.value == status }.map { (type, _) ->
                PrayerEntry(id = 0, userId = "local", date = date, prayerType = type, status = status, updatedAt = updatedAtMap[date to type] ?: 0L)
            }
        }

    fun seedTimes(date: LocalDate, times: Map<PrayerType, LocalTime>) {
        cachedTimes[date] = times
    }

    fun seedStreaks(streaks: Map<PrayerType, StreakInfo>) {
        streaksFlow.value = streaks
    }

    /** Seeds one date's [StatusCounts] — only dates with something recorded should be seeded,
     * matching [PrayerRepository.getDailyStatusCounts]'s real contract. */
    fun seedDailyCounts(date: LocalDate, counts: StatusCounts) {
        dailyCounts = dailyCounts + (date to counts)
    }

    override fun observeMonthEntries(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<PrayerEntry>>> {
        observeMonthEntriesCallCount++
        return entries.map { byDate ->
            byDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) }
                .mapValues { (date, statuses) ->
                    statuses.map { (type, status) ->
                        PrayerEntry(id = 0, userId = "local", date = date, prayerType = type, status = status, updatedAt = updatedAtMap[date to type] ?: 0L)
                    }
                }
        }
    }

    override suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>? = cachedTimes[date]

    override suspend fun upsertPrayerStatus(date: LocalDate, prayerType: PrayerType, status: PrayerStatus) {
        upserts += Triple(date, prayerType, status)
        setStatus(date, prayerType, status)
    }

    override fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>> = streaksFlow

    override suspend fun getDailyStatusCounts(from: LocalDate, to: LocalDate): List<DayStatusCounts> {
        dailyStatusCountsCalls += from to to
        return dailyCounts
            .filterKeys { !it.isBefore(from) && !it.isAfter(to) }
            .toSortedMap()
            .map { (date, counts) -> DayStatusCounts(date, counts) }
    }

    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> {
        prayerBreakdownCalls += from to to
        return prayerBreakdown
    }

    override suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry> = notUsed()

    override suspend fun bulkUpsertPrayerStatus(entries: List<Pair<LocalDate, PrayerType>>, status: PrayerStatus) {
        bulkUpserts += entries to status
        entries.forEach { (date, type) -> setStatus(date, type, status) }
    }

    override suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo> = notUsed()

    override fun observeQazaBalance(): Flow<Map<PrayerType, Int>> = entries.map { byDate ->
        val counts = byDate.values.flatMap { it.entries }
            .filter { it.value == PrayerStatus.QAZA_PENDING }
            .groupingBy { it.key }.eachCount()
        PrayerType.values().associateWith { counts[it] ?: 0 }
    }

    override fun observePendingQazaEntries(): Flow<List<PrayerEntry>> =
        entries.map { allEntriesWithStatus(PrayerStatus.QAZA_PENDING) }

    override suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType) {
        upserts += Triple(date, prayerType, PrayerStatus.QAZA_COMPLETED)
        setStatus(date, prayerType, PrayerStatus.QAZA_COMPLETED)
    }

    override fun observeQazaMadeUpCount(): Flow<Int> =
        entries.map { allEntriesWithStatus(PrayerStatus.QAZA_COMPLETED).size }

    override fun observeQazaMadeUpSince(since: LocalDate): Flow<List<PrayerEntry>> {
        val sinceMillis = since.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return entries.map {
            allEntriesWithStatus(PrayerStatus.QAZA_COMPLETED)
                .filter { it.updatedAt >= sinceMillis }
                .sortedByDescending { it.updatedAt }
        }
    }

    override fun observeRecentQazaMadeUp(limit: Int): Flow<List<PrayerEntry>> =
        entries.map { allEntriesWithStatus(PrayerStatus.QAZA_COMPLETED).sortedByDescending { it.updatedAt }.take(limit) }

    override suspend fun recalculateStreaks() = notUsed()
    override fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>> = notUsed()

    private fun notUsed(): Nothing = throw NotImplementedError("not used by this test")
}

class FakeVerseRepository(private val verse: VerseInfo = VerseInfo("test verse", "Test 1:1")) : VerseRepository {
    override suspend fun getRandomVerse(): VerseInfo = verse
    override suspend fun refreshCache() = throw NotImplementedError("not used by this test")
}

/** In-memory [GoalRepository] exercising only what the Stats/Goals ViewModels actually call. */
class FakeGoalRepository : GoalRepository {

    private val historiesFlow = MutableStateFlow<List<GoalHistory>>(emptyList())

    var periodStats: List<GoalPeriodStat> = emptyList()

    val periodStatsCalls = mutableListOf<Pair<LocalDate, LocalDate>>()
    val checkIns = mutableListOf<Triple<Long, Int, LocalDate>>()
    val dayLogs = mutableListOf<DayLogCall>()
    val archiveCalls = mutableListOf<Pair<Long, Boolean>>()
    val deletedGoalIds = mutableListOf<Long>()

    data class DayLogCall(val goalId: Long, val date: LocalDate, val amount: Int, val skipped: Boolean)

    fun seedHistories(histories: List<GoalHistory>) {
        historiesFlow.value = histories
    }

    override fun observeGoalHistories(): Flow<List<GoalHistory>> = historiesFlow

    override fun observeGoalHistory(goalId: Long): Flow<GoalHistory?> =
        historiesFlow.map { list -> list.firstOrNull { it.goal.id == goalId } }

    override suspend fun getGoalPeriodStats(from: LocalDate, to: LocalDate): List<GoalPeriodStat> {
        periodStatsCalls += from to to
        return periodStats
    }

    override suspend fun checkIn(goalId: Long, amount: Int, date: LocalDate) {
        checkIns += Triple(goalId, amount, date)
    }

    override suspend fun setDayLog(goalId: Long, date: LocalDate, amount: Int, skipped: Boolean) {
        dayLogs += DayLogCall(goalId, date, amount, skipped)
    }

    override suspend fun setArchived(goalId: Long, archived: Boolean) {
        archiveCalls += goalId to archived
    }

    override suspend fun deleteGoal(goalId: Long) {
        deletedGoalIds += goalId
    }

    override suspend fun createGoal(
        name: String, category: com.salattracker.domain.model.GoalCategory, frequency: com.salattracker.domain.model.GoalFrequency,
        targetAmount: Int, unit: String, reminderEnabled: Boolean, reminderTime: LocalTime?, durationDays: Int,
        daysOfWeek: Set<DayOfWeek>, endsAfterDuration: Boolean,
    ): Long = notUsed()
    override suspend fun getAllActiveGoals() = notUsed()
    override suspend fun getGoal(goalId: Long) = notUsed()
    override suspend fun updateGoal(
        goalId: Long, name: String, category: com.salattracker.domain.model.GoalCategory, frequency: com.salattracker.domain.model.GoalFrequency,
        targetAmount: Int, unit: String, reminderEnabled: Boolean, reminderTime: LocalTime?, durationDays: Int,
        daysOfWeek: Set<DayOfWeek>, endsAfterDuration: Boolean,
    ) = notUsed()

    private fun notUsed(): Nothing = throw NotImplementedError("not used by this test")
}

class FakeGoalReminderScheduler : GoalReminderScheduler {
    val cancelled = mutableListOf<Long>()
    var rescheduleCount = 0
        private set

    override fun cancel(goalId: Long) { cancelled += goalId }
    override fun rescheduleAll() { rescheduleCount++ }
}
