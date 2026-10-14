package com.salattracker.testutil

import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PeriodStats
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StatusTrendPoint
import com.salattracker.domain.repository.VerseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * In-memory [PrayerRepository] exercising only what CalendarViewModel/StatsViewModel actually
 * call. Everything else throws [NotImplementedError], so a future ViewModel change that starts
 * relying on an unfaked method fails its test loudly instead of silently no-op'ing.
 */
class FakePrayerRepository : PrayerRepository {

    private val entries = MutableStateFlow<Map<LocalDate, Map<PrayerType, PrayerStatus>>>(emptyMap())
    private val cachedTimes = mutableMapOf<LocalDate, Map<PrayerType, LocalTime>>()
    private val streaksFlow = MutableStateFlow<Map<PrayerType, StreakInfo>>(emptyMap())

    var observeMonthEntriesCallCount = 0
        private set
    val upserts = mutableListOf<Triple<LocalDate, PrayerType, PrayerStatus>>()

    // ── Stats seeding ───────────────────────────────────────────────────────
    var periodStats: PeriodStats = PeriodStats(congregationPct = 0, individualPct = 0, missedPct = 0, completionPct = 0)
    var statusTrendPoints: List<StatusTrendPoint> = emptyList()
    var prayerBreakdown: List<PrayerBreakdown> = emptyList()

    val statsForPeriodCalls = mutableListOf<Pair<LocalDate, LocalDate>>()
    val statusTrendCalls = mutableListOf<Triple<LocalDate, LocalDate, Int>>()
    val prayerBreakdownCalls = mutableListOf<Pair<LocalDate, LocalDate>>()

    fun seedStatus(date: LocalDate, type: PrayerType, status: PrayerStatus) {
        entries.value = entries.value.toMutableMap().apply {
            this[date] = (this[date].orEmpty()) + (type to status)
        }
    }

    fun seedTimes(date: LocalDate, times: Map<PrayerType, LocalTime>) {
        cachedTimes[date] = times
    }

    fun seedStreaks(streaks: Map<PrayerType, StreakInfo>) {
        streaksFlow.value = streaks
    }

    override fun observeMonthEntries(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<PrayerEntry>>> {
        observeMonthEntriesCallCount++
        return entries.map { byDate ->
            byDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) }
                .mapValues { (date, statuses) ->
                    statuses.map { (type, status) ->
                        PrayerEntry(id = 0, userId = "local", date = date, prayerType = type, status = status, updatedAt = 0L)
                    }
                }
        }
    }

    override suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>? = cachedTimes[date]

    override suspend fun upsertPrayerStatus(date: LocalDate, prayerType: PrayerType, status: PrayerStatus) {
        upserts += Triple(date, prayerType, status)
        seedStatus(date, prayerType, status)
    }

    override fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>> = streaksFlow

    override suspend fun getStatsForPeriod(from: LocalDate, to: LocalDate): PeriodStats {
        statsForPeriodCalls += from to to
        return periodStats
    }

    override suspend fun getStatusTrendPoints(from: LocalDate, to: LocalDate, days: Int): List<StatusTrendPoint> {
        statusTrendCalls += Triple(from, to, days)
        return statusTrendPoints
    }

    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> {
        prayerBreakdownCalls += from to to
        return prayerBreakdown
    }

    override fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>> = notUsed()
    override suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry> = notUsed()
    override suspend fun bulkUpsertPrayerStatus(entries: List<Pair<LocalDate, PrayerType>>, status: PrayerStatus) = notUsed()
    override suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo> = notUsed()
    override fun observeQazaBalance(): Flow<Map<PrayerType, Int>> = notUsed()
    override fun observePendingQazaEntries(): Flow<List<PrayerEntry>> = notUsed()
    override suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType) = notUsed()
    override suspend fun recalculateStreaks() = notUsed()

    private fun notUsed(): Nothing = throw NotImplementedError("not used by this test")
}

class FakeVerseRepository(private val verse: VerseInfo = VerseInfo("test verse", "Test 1:1")) : VerseRepository {
    override suspend fun getRandomVerse(): VerseInfo = verse
    override suspend fun refreshCache() = throw NotImplementedError("not used by this test")
}

/** In-memory [GoalRepository] exercising only what StatsViewModel actually calls. */
class FakeGoalRepository : GoalRepository {

    private val activeGoalsFlow = MutableStateFlow<List<GoalWithProgress>>(emptyList())

    var dailyPoints: List<GoalDailyPoint> = emptyList()
    var completionBreakdown: List<GoalCompletionStat> = emptyList()

    val dailyPointsCalls = mutableListOf<Pair<LocalDate, LocalDate>>()
    val completionBreakdownCalls = mutableListOf<Pair<LocalDate, LocalDate>>()

    fun seedActiveGoals(goals: List<GoalWithProgress>) {
        activeGoalsFlow.value = goals
    }

    override fun observeActiveGoalsWithProgress(): Flow<List<GoalWithProgress>> = activeGoalsFlow

    override suspend fun getDailyGoalCompletionPoints(from: LocalDate, to: LocalDate): List<GoalDailyPoint> {
        dailyPointsCalls += from to to
        return dailyPoints
    }

    override suspend fun getGoalCompletionBreakdown(from: LocalDate, to: LocalDate): List<GoalCompletionStat> {
        completionBreakdownCalls += from to to
        return completionBreakdown
    }

    override suspend fun createGoal(
        name: String, category: com.salattracker.domain.model.GoalCategory, frequency: com.salattracker.domain.model.GoalFrequency,
        targetAmount: Int, unit: String, reminderEnabled: Boolean, reminderTime: LocalTime?, durationDays: Int,
    ): Long = notUsed()
    override suspend fun checkIn(goalId: Long, amount: Int) = notUsed()
    override suspend fun getAllActiveGoals() = notUsed()
    override suspend fun getGoal(goalId: Long) = notUsed()
    override suspend fun updateGoal(
        goalId: Long, name: String, category: com.salattracker.domain.model.GoalCategory, frequency: com.salattracker.domain.model.GoalFrequency,
        targetAmount: Int, unit: String, reminderEnabled: Boolean, reminderTime: LocalTime?, durationDays: Int,
    ) = notUsed()
    override suspend fun deleteGoal(goalId: Long) = notUsed()

    private fun notUsed(): Nothing = throw NotImplementedError("not used by this test")
}
