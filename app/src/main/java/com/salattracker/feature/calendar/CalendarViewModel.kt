package com.salattracker.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.ReminderNotifications
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.VerseRepository
import com.salattracker.domain.usecase.CalendarViewType
import com.salattracker.domain.usecase.coerceToCalendarRange
import com.salattracker.domain.usecase.isAwayFromToday
import com.salattracker.domain.usecase.isPrayerMarkable
import com.salattracker.domain.usecase.observedRange
import com.salattracker.domain.usecase.shiftPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val view: CalendarViewType = CalendarViewType.MONTH,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoadingMarks: Boolean = true,
    /** Only the prayers actually recorded for a date — a type missing from the inner map (or
     * the date missing entirely) means not-yet-recorded, same convention as every other screen. */
    val marks: Map<LocalDate, Map<PrayerType, PrayerStatus>> = emptyMap(),
    val selectedTimes: Map<PrayerType, LocalTime>? = null,
    val statusSheet: PrayerStatusSheetArgs? = null,
    /** Set when the Year view drilled into a month, so Back can return there instead of
     * leaving Calendar entirely; cleared by returning, or by any explicit view switch. */
    val cameFromYear: Int? = null,
    val isSaving: Boolean = false,
    val errorRes: Int? = null,
    val verse: VerseInfo? = null,
    val gender: Gender? = null,
) {
    val awayFromToday: Boolean get() = isAwayFromToday(view, selectedDate, today)
    val selectedDayStatuses: Map<PrayerType, PrayerStatus> get() = marks[selectedDate].orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest / mapLatest below
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val verseRepository: VerseRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderNotifications: ReminderNotifications,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        observeMarks()
        observeSelectedTimes()
        observeGender()
        loadVerse()
    }

    private fun observeGender() {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.gender }
                .distinctUntilChanged()
                .collect { gender -> _uiState.update { it.copy(gender = gender) } }
        }
    }

    private fun loadVerse() {
        viewModelScope.launch {
            _uiState.update { it.copy(verse = verseRepository.getRandomVerse()) }
        }
    }

    /** Re-fetches whenever the observed range actually changes (view switch, or paging to a
     * different period) — [flatMapLatest] drops an in-flight query for a range we've since
     * paged away from, and the Room [Flow] re-emits on its own after every save, so no manual
     * reload is ever needed after [onStatusSave]. */
    private fun observeMarks() {
        viewModelScope.launch {
            _uiState
                .map { observedRange(it.view, it.selectedDate) }
                .distinctUntilChanged()
                .flatMapLatest { (from, to) -> prayerRepository.observeMonthEntries(from, to) }
                .onStart { _uiState.update { it.copy(isLoadingMarks = true) } }
                .catch { _ -> _uiState.update { it.copy(isLoadingMarks = false, errorRes = R.string.error_load_generic) } }
                .collect { entriesByDate ->
                    val marks = entriesByDate.mapValues { (_, entries) ->
                        entries.associate { it.prayerType to it.status }
                    }
                    _uiState.update { it.copy(isLoadingMarks = false, marks = marks) }
                }
        }
    }

    /** Prayer times for the selected day only. [mapLatest] means a fast swipe cancels a lookup
     * for a date we've already left before it resolves; the inner guard drops a result for a
     * date we've since moved on from again, for the same reason. */
    private fun observeSelectedTimes() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedDate }
                .distinctUntilChanged()
                .mapLatest { date -> date to prayerRepository.getPrayerTimes(date) }
                .collect { (date, times) ->
                    _uiState.update { if (it.selectedDate == date) it.copy(selectedTimes = times) else it }
                }
        }
    }

    // ── View & navigation ─────────────────────────────────────────────────────

    fun onViewChanged(view: CalendarViewType) = _uiState.update { it.copy(view = view, cameFromYear = null) }

    fun onPreviousPeriod() = shiftSelected(-1)

    fun onNextPeriod() = shiftSelected(1)

    private fun shiftSelected(direction: Int) = _uiState.update { state ->
        state.copy(selectedDate = shiftPeriod(state.view, state.selectedDate, direction))
    }

    fun onTodayClick() = _uiState.update { it.copy(selectedDate = it.today) }

    fun onDatePicked(date: LocalDate) = _uiState.update { it.copy(selectedDate = date.coerceToCalendarRange()) }

    /** Tapping a day cell in the Month view selects it, loading that day into the inline panel. */
    fun onDaySelected(date: LocalDate) = _uiState.update { it.copy(selectedDate = date) }

    /** Jumps straight to Month view with [date] selected — used by Home's week strip, which
     * lets you tap a day from outside Calendar entirely. */
    fun onOpenDay(date: LocalDate) = _uiState.update {
        it.copy(view = CalendarViewType.MONTH, selectedDate = date.coerceToCalendarRange(), cameFromYear = null)
    }

    /** Year: tapping a month tile drills into it, remembering the year to return Back to. */
    fun onMonthTileClick(month: YearMonth) = _uiState.update { state ->
        state.copy(view = CalendarViewType.MONTH, selectedDate = month.atDay(1), cameFromYear = state.selectedDate.year)
    }

    fun onBackFromMonthDrillDown() = _uiState.update { state ->
        val year = state.cameFromYear ?: return@update state
        state.copy(view = CalendarViewType.YEAR, selectedDate = LocalDate.of(year, state.selectedDate.monthValue, 1), cameFromYear = null)
    }

    // ── Prayer status editing ────────────────────────────────────────────────

    /**
     * Opens the status picker for one (date, prayer) — used by the Month view's inline rows
     * and the Week view's squares. Markability only needs the actual prayer time for
     * *today* (any earlier date is always markable, any later date never is — see
     * [isPrayerMarkable]'s own rules), so this only fetches times when [date] is today, rather
     * than requiring every visible date's times to already be loaded.
     */
    fun onPrayerClick(date: LocalDate, type: PrayerType) {
        viewModelScope.launch {
            val today = _uiState.value.today
            val prayerTime = if (date == today) prayerRepository.getPrayerTimes(date)?.get(type) else null
            if (!isPrayerMarkable(date, prayerTime, today)) return@launch
            val current = _uiState.value.marks[date]?.get(type) ?: PrayerStatus.NOT_RECORDED
            _uiState.update { it.copy(statusSheet = PrayerStatusSheetArgs(type, date, current)) }
        }
    }

    fun onStatusSheetDismiss() = _uiState.update { it.copy(statusSheet = null) }

    fun onStatusSave(newStatus: PrayerStatus) {
        val args = _uiState.value.statusSheet ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                prayerRepository.upsertPrayerStatus(args.date, args.prayerType, newStatus)
            }.onSuccess {
                if (args.date == LocalDate.now()) reminderNotifications.dismiss(args.prayerType)
                _uiState.update { it.copy(isSaving = false, statusSheet = null) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}
