package com.salattracker.testutil

import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueModeState
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.model.RingerMode
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.DayStatusCounts
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.LocaleApplier
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.MosqueGeofencer
import com.salattracker.domain.repository.MosqueModeNotifier
import com.salattracker.domain.repository.MosqueModeStore
import com.salattracker.domain.repository.MosqueModeSyncScheduler
import com.salattracker.domain.repository.MosqueRepository
import com.salattracker.domain.repository.RingerController
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.domain.repository.VerseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    override suspend fun getPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>? = cachedTimes[date]

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

/** In-memory [LocationProvider] — tests set [nextLocation] before calling the code under test. */
class FakeLocationProvider : LocationProvider {
    var nextLocation: Pair<Double, Double>? = null
    var callCount = 0
        private set
    var lastHighAccuracy: Boolean? = null
        private set

    override suspend fun getCurrentLocation(highAccuracy: Boolean): Pair<Double, Double>? {
        callCount++
        lastHighAccuracy = highAccuracy
        return nextLocation
    }
}

class FakePrayerReminderRescheduler : PrayerReminderRescheduler {
    var rescheduleCount = 0
        private set

    override fun rescheduleAll() { rescheduleCount++ }
}

class FakeLocaleApplier : LocaleApplier {
    val applied = mutableListOf<AppLanguage>()

    override fun apply(language: AppLanguage) { applied += language }
}

/** In-memory [SettingsRepository] exercising only what SettingsViewModel actually calls. */
class FakeSettingsRepository(initial: UserPrefs = UserPrefs()) : SettingsRepository {

    private val prefs = MutableStateFlow(initial)

    val useDeviceLocationCalls = mutableListOf<Boolean>()
    val locationCalls = mutableListOf<Pair<Double, Double>>()
    val calculationMethodIdCalls = mutableListOf<Int>()
    val use24HourFormatCalls = mutableListOf<Boolean>()
    val languageCalls = mutableListOf<AppLanguage>()
    val themeModeCalls = mutableListOf<ThemeMode>()
    val madhabCalls = mutableListOf<Madhab>()
    val highLatitudeRuleCalls = mutableListOf<HighLatitudeRule>()
    val prayerTimeAdjustCalls = mutableListOf<Pair<PrayerType, Int>>()
    val jumuahEnabledCalls = mutableListOf<Boolean>()
    val jumuahReminderLeadCalls = mutableListOf<Int>()

    override fun observeSettings(): Flow<UserPrefs> = prefs

    override suspend fun getCurrentPrefs(): UserPrefs = prefs.value

    override suspend fun saveSettings(
        useDeviceLocation: Boolean, latitude: Double, longitude: Double, calculationMethodId: Int,
        reminderEnabled: Map<PrayerType, Boolean>, reminderOffsetMinutes: Map<PrayerType, Int>,
        use24HourFormat: Boolean, language: AppLanguage, themeMode: ThemeMode,
        madhab: Madhab, highLatitudeRule: HighLatitudeRule, prayerTimeAdjustMinutes: Map<PrayerType, Int>,
        madhabChosen: Boolean, jumuahEnabled: Boolean, jumuahReminderLeadMinutes: Int,
    ) = notUsed()

    override suspend fun setUseDeviceLocation(enabled: Boolean) {
        useDeviceLocationCalls += enabled
        prefs.update { it.copy(useDeviceLocation = enabled) }
    }

    override suspend fun setLocation(latitude: Double, longitude: Double) {
        locationCalls += latitude to longitude
        prefs.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    override suspend fun setCalculationMethodId(id: Int) {
        calculationMethodIdCalls += id
        prefs.update { it.copy(calculationMethodId = id) }
    }

    override suspend fun setUse24HourFormat(enabled: Boolean) {
        use24HourFormatCalls += enabled
        prefs.update { it.copy(use24HourFormat = enabled) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languageCalls += language
        prefs.update { it.copy(language = language) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        themeModeCalls += themeMode
        prefs.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setAllRemindersEnabled(enabled: Boolean) = notUsed()
    override suspend fun setOnboardingCompleted(completed: Boolean) = notUsed()
    override suspend fun setReminderEnabled(type: PrayerType, enabled: Boolean) = notUsed()
    override suspend fun setReminderOffsetMinutes(type: PrayerType, minutes: Int) = notUsed()
    override suspend fun setReminderSnoozeMinutes(type: PrayerType, minutes: Int) = notUsed()
    override suspend fun setReminderFullScreenAlert(type: PrayerType, enabled: Boolean) = notUsed()
    override suspend fun setReminderVibrationEnabled(type: PrayerType, enabled: Boolean) = notUsed()
    override suspend fun setReminderSoundUri(type: PrayerType, uri: String?) = notUsed()

    override suspend fun setMadhab(madhab: Madhab) {
        madhabCalls += madhab
        prefs.update { it.copy(madhab = madhab, madhabChosen = true) }
    }

    override suspend fun setHighLatitudeRule(rule: HighLatitudeRule) {
        highLatitudeRuleCalls += rule
        prefs.update { it.copy(highLatitudeRule = rule) }
    }

    override suspend fun setPrayerTimeAdjustMinutes(type: PrayerType, minutes: Int) {
        prayerTimeAdjustCalls += type to minutes
        prefs.update { it.copy(prayerTimeAdjustMinutes = it.prayerTimeAdjustMinutes + (type to minutes)) }
    }

    override suspend fun setJumuahEnabled(enabled: Boolean) {
        jumuahEnabledCalls += enabled
        prefs.update { it.copy(jumuahEnabled = enabled) }
    }

    override suspend fun setJumuahReminderLeadMinutes(minutes: Int) {
        jumuahReminderLeadCalls += minutes
        prefs.update { it.copy(jumuahReminderLeadMinutes = minutes) }
    }

    private fun notUsed(): Nothing = throw NotImplementedError("not used by this test")
}

// ── Mosque Mode ────────────────────────────────────────────────────────────────

class FakeMosqueRepository(initial: List<Mosque> = emptyList()) : MosqueRepository {
    private val mosques = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    var nearby: List<NearbyMosque> = emptyList()
    var nearbyError: Throwable? = null
    var failWrites = false
    var findNearbyCalls = 0
        private set

    fun getMosquesBlocking(): List<Mosque> = mosques.value

    fun addForTest(mosque: Mosque) { mosques.update { it + mosque } }

    override fun observeMosques(): Flow<List<Mosque>> = mosques

    override suspend fun getMosques(): List<Mosque> = mosques.value

    override suspend fun addMosque(mosque: Mosque): Long {
        if (failWrites) throw java.io.IOException("disk full")
        val id = nextId++
        mosques.update { it + mosque.copy(id = id) }
        return id
    }

    override suspend fun updateMosque(mosque: Mosque) {
        if (failWrites) throw java.io.IOException("disk full")
        mosques.update { list -> list.map { if (it.id == mosque.id) mosque else it } }
    }

    override suspend fun deleteMosque(id: Long) {
        mosques.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyMosque> {
        findNearbyCalls++
        nearbyError?.let { throw it }
        return nearby
    }
}

class FakeMosqueModeStore(initial: MosqueModeState = MosqueModeState()) : MosqueModeStore {
    private val state = MutableStateFlow(initial)

    fun setStateForTest(newState: MosqueModeState) { state.value = newState }

    override fun observeState(): Flow<MosqueModeState> = state

    override suspend fun getState(): MosqueModeState = state.value

    override suspend fun setEnabled(enabled: Boolean) {
        state.update { it.copy(enabled = enabled) }
    }

    override suspend fun setVisit(insideIds: Set<Long>, restoreTo: RingerMode?, activeMosqueName: String?) {
        state.update { it.copy(insideIds = insideIds, restoreTo = restoreTo, activeMosqueName = activeMosqueName) }
    }
}

class FakeMosqueGeofencer : MosqueGeofencer {
    var result = GeofenceSync.OK
    val registered = mutableListOf<List<Mosque>>()
    var clearCount = 0
        private set

    override suspend fun register(mosques: List<Mosque>): GeofenceSync {
        registered += mosques
        return result
    }

    override suspend fun clear() { clearCount++ }
}

class FakeRingerController(var mode: RingerMode = RingerMode.NORMAL) : RingerController {
    override var canVibrate: Boolean = true
    var acceptChanges = true
    val setCalls = mutableListOf<RingerMode>()

    override fun current(): RingerMode = mode

    override fun set(mode: RingerMode): Boolean {
        setCalls += mode
        if (!acceptChanges) return false
        this.mode = mode
        return true
    }
}

class FakeMosqueModeNotifier : MosqueModeNotifier {
    val shown = mutableListOf<String>()
    var dismissCount = 0
        private set

    override fun showVibrateOn(mosqueName: String) { shown += mosqueName }
    override fun dismiss() { dismissCount++ }
}

class FakeMosqueModeSyncScheduler : MosqueModeSyncScheduler {
    var requestCount = 0
        private set

    override fun requestSync() { requestCount++ }
}
