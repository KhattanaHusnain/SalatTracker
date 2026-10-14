package com.salattracker.feature.calendar

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
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
 * In-memory [PrayerRepository] exercising only what [CalendarViewModel] actually calls.
 * Everything else throws [NotImplementedError], so a future ViewModel change that starts
 * relying on an unfaked method fails its test loudly instead of silently no-op'ing.
 */
class FakePrayerRepository : PrayerRepository {

    private val entries = MutableStateFlow<Map<LocalDate, Map<PrayerType, PrayerStatus>>>(emptyMap())
    private val cachedTimes = mutableMapOf<LocalDate, Map<PrayerType, LocalTime>>()

    var observeMonthEntriesCallCount = 0
        private set
    val upserts = mutableListOf<Triple<LocalDate, PrayerType, PrayerStatus>>()

    fun seedStatus(date: LocalDate, type: PrayerType, status: PrayerStatus) {
        entries.value = entries.value.toMutableMap().apply {
            this[date] = (this[date].orEmpty()) + (type to status)
        }
    }

    fun seedTimes(date: LocalDate, times: Map<PrayerType, LocalTime>) {
        cachedTimes[date] = times
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

    override fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>> = notUsed()
    override suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry> = notUsed()
    override suspend fun bulkUpsertPrayerStatus(entries: List<Pair<LocalDate, PrayerType>>, status: PrayerStatus) = notUsed()
    override suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo> = notUsed()
    override fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>> = notUsed()
    override fun observeQazaBalance(): Flow<Map<PrayerType, Int>> = notUsed()
    override fun observePendingQazaEntries(): Flow<List<PrayerEntry>> = notUsed()
    override suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType) = notUsed()
    override suspend fun getStatsForPeriod(from: LocalDate, to: LocalDate): PeriodStats = notUsed()
    override suspend fun getStatusTrendPoints(from: LocalDate, to: LocalDate, days: Int): List<StatusTrendPoint> = notUsed()
    override suspend fun getPrayerBreakdown(from: LocalDate, to: LocalDate): List<PrayerBreakdown> = notUsed()
    override suspend fun recalculateStreaks() = notUsed()

    private fun notUsed(): Nothing = throw NotImplementedError("not used by CalendarViewModelTest")
}

class FakeVerseRepository(private val verse: VerseInfo = VerseInfo("test verse", "Test 1:1")) : VerseRepository {
    override suspend fun getRandomVerse(): VerseInfo = verse
    override suspend fun refreshCache() = throw NotImplementedError("not used by CalendarViewModelTest")
}
