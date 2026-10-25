package com.salattracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.R
import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.ReminderNotifications
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.domain.repository.VerseRepository
import com.salattracker.domain.usecase.CalculateStreakUseCase
import com.salattracker.domain.usecase.NextPrayer
import com.salattracker.domain.usecase.currentPrayerOf
import com.salattracker.domain.usecase.isPrayerMarkable
import com.salattracker.domain.usecase.minutesUntil
import com.salattracker.domain.usecase.nextPrayerOf
import com.salattracker.domain.usecase.prayerDayForHome
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val now: LocalDateTime = LocalDateTime.now(),
    val todayDate: LocalDate = LocalDate.now(),
    val prayers: List<PrayerEntryUi> = emptyList(),
    val nextPrayer: NextPrayer? = null,
    val minutesUntilNext: Long = 0,
    val currentPrayer: PrayerType? = null,
    val streaks: Map<PrayerType, StreakInfo> = emptyMap(),
    val lastSevenDays: Map<LocalDate, Map<PrayerType, PrayerStatus>> = emptyMap(),
    val qazaPendingTotal: Int = 0,
    val jumuahEnabled: Boolean = true,
    val madhab: Madhab = Madhab.STANDARD,
    val gender: Gender? = null,
    val verse: VerseInfo? = null,
    val sheetArgs: PrayerStatusSheetArgs? = null,   // null = sheet hidden
    val isSaving: Boolean = false,
    val errorRes: Int? = null,
)

data class PrayerEntryUi(
    val type: PrayerType,
    val time: LocalTime?,
    val reminderTime: LocalTime?,   // prayer time adjusted by the reminder offset; null if no offset/reminder disabled
    val status: PrayerStatus,
    val isMarkable: Boolean,        // false once it's a prayer later today that hasn't happened yet
)

/** Everything the hero card and timeline need for one tick, built off the suspend
 * prayer-time lookups so it can be computed inside a single Flow transform. */
private data class TodaySnapshot(
    val prayerDate: LocalDate,
    val now: LocalDateTime,
    val prayers: List<PrayerEntryUi>,
    val nextPrayer: NextPrayer?,
    val minutesUntilNext: Long,
    val currentPrayer: PrayerType?,
    val jumuahEnabled: Boolean,
    val madhab: Madhab,
    val gender: Gender?,
)

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest below
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val verseRepository: VerseRepository,
    private val workManager: WorkManager,
    private val reminderNotifications: ReminderNotifications,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Emits immediately, then again on every minute boundary — everything in Home that depends
     * on the clock (the countdown, which prayer is "now", a midnight date rollover) is driven off
     * this single source rather than each polling independently. */
    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            val millisIntoThisMinute = now.second * 1000L + now.nano / 1_000_000L
            delay(60_000L - millisIntoThisMinute)
        }
    }

    /** The Home timeline changes at Fajr, not civil midnight, so late Isha remains available. */
    private val todayDate: Flow<LocalDate> = ticker
        .mapLatest { now ->
            prayerDayForHome(
                now = now,
                todayFajr = prayerRepository.getPrayerTimes(now.toLocalDate())?.get(PrayerType.FAJR),
            )
        }
        .distinctUntilChanged()

    init {
        observeToday()
        observeLastSevenDays()
        observeQazaBalance()
        loadStreak()
        loadVerse()
        PrayerReminderWorker.enqueueOneTime(workManager)
    }

    private fun observeToday() {
        viewModelScope.launch {
            todayDate
                .flatMapLatest { date ->
                    combine(
                        prayerRepository.observeTodayPrayers(date),
                        settingsRepository.observeSettings(),
                        ticker,
                    ) { entries, prefs, now -> buildTodaySnapshot(date, entries, prefs, now) }
                }
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { _ -> _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } }
                .collect { snapshot ->
                    _uiState.update {
                        it.copy(
                            isLoading        = false,
                            todayDate        = snapshot.prayerDate,
                            now              = snapshot.now,
                            prayers          = snapshot.prayers,
                            nextPrayer       = snapshot.nextPrayer,
                            minutesUntilNext = snapshot.minutesUntilNext,
                            currentPrayer    = snapshot.currentPrayer,
                            jumuahEnabled    = snapshot.jumuahEnabled,
                            madhab           = snapshot.madhab,
                            gender           = snapshot.gender,
                        )
                    }
                }
        }
    }

    private suspend fun buildTodaySnapshot(
        date: LocalDate,
        entries: List<PrayerEntry>,
        prefs: UserPrefs,
        now: LocalDateTime,
    ): TodaySnapshot {
        val times = prayerRepository.getPrayerTimes(date)
        val tomorrowFajr = prayerRepository.getPrayerTimes(date.plusDays(1))?.get(PrayerType.FAJR)
        val nowTime = now.toLocalTime()

        val prayers = PrayerType.values().map { type ->
            val prayerTime = times?.get(type)
            val offsetMinutes = prefs.reminderOffsetMinutes[type] ?: 0
            val reminderEnabled = prefs.reminderEnabled[type] ?: true
            PrayerEntryUi(
                type = type,
                time = prayerTime,
                reminderTime = if (reminderEnabled && offsetMinutes != 0) prayerTime?.plusMinutes(offsetMinutes.toLong()) else null,
                status = entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED,
                isMarkable = isPrayerMarkable(date, prayerTime, today = date, now = nowTime),
            )
        }

        val nextPrayer = nextPrayerOf(times, tomorrowFajr, nowTime)?.let { next ->
            // When [date] is yesterday during the pre-Fajr period, "tomorrow" is actually
            // the current civil date. Its Fajr is still ahead today, not a day away.
            if (date.isBefore(now.toLocalDate()) && next.type == PrayerType.FAJR && next.isTomorrow) {
                next.copy(isTomorrow = false)
            } else next
        }
        return TodaySnapshot(
            prayerDate       = date,
            now              = now,
            prayers          = prayers,
            nextPrayer       = nextPrayer,
            minutesUntilNext = nextPrayer?.let { minutesUntil(it, nowTime) } ?: 0,
            currentPrayer    = currentPrayerOf(times, nowTime),
            // Women aren't obliged to attend Jumu'ah, so Friday Dhuhr never relabels to
            // "Jumu'ah" for them, regardless of the (possibly stale) stored toggle.
            jumuahEnabled    = prefs.jumuahEnabled && prefs.gender != Gender.FEMALE,
            madhab           = prefs.madhab,
            gender           = prefs.gender,
        )
    }

    private fun observeLastSevenDays() {
        viewModelScope.launch {
            todayDate
                .flatMapLatest { date -> prayerRepository.observeMonthEntries(date.minusDays(6), date) }
                .map { entriesByDate -> entriesByDate.mapValues { (_, entries) -> entries.associate { it.prayerType to it.status } } }
                .collect { marks -> _uiState.update { it.copy(lastSevenDays = marks) } }
        }
    }

    private fun observeQazaBalance() {
        viewModelScope.launch {
            prayerRepository.observeQazaBalance()
                .map { it.values.sum() }
                .collect { total -> _uiState.update { it.copy(qazaPendingTotal = total) } }
        }
    }

    private fun loadVerse() {
        viewModelScope.launch {
            _uiState.update { it.copy(verse = verseRepository.getVerseOfDay()) }
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            calculateStreakUseCase().collect { streaks ->
                _uiState.update { it.copy(streaks = streaks) }
            }
        }
    }

    // ── Sheet ─────────────────────────────────────────────────────────────────

    fun onPrayerCardClick(type: PrayerType) {
        val entry = _uiState.value.prayers.find { it.type == type } ?: return
        if (!entry.isMarkable) return
        val current = entry.status
        _uiState.update {
            it.copy(
                sheetArgs = PrayerStatusSheetArgs(
                    prayerType    = type,
                    date          = it.todayDate,
                    currentStatus = current,
                )
            )
        }
    }

    fun onSheetDismiss() = _uiState.update { it.copy(sheetArgs = null) }

    fun onStatusSave(newStatus: PrayerStatus) {
        val args = _uiState.value.sheetArgs ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                prayerRepository.upsertPrayerStatus(args.date, args.prayerType, newStatus)
            }.onSuccess {
                // Before Fajr, Home intentionally shows yesterday's Isha; logging that row must
                // dismiss its still-visible reminder just like a same-civil-date prayer does.
                if (args.date == _uiState.value.todayDate) reminderNotifications.dismiss(args.prayerType)
                // Streak tiles refresh on their own — loadStreak()'s Flow collector
                // re-emits automatically once upsertPrayerStatus's write commits.
                _uiState.update { it.copy(isSaving = false, sheetArgs = null) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}
