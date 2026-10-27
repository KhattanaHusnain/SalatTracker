package com.salattracker.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBatchUndo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.PrayerSlot
import com.salattracker.domain.repository.ReminderNotifications
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.usecase.isPrayerMarkable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarBatchPreview(
    val status: PrayerStatus,
    val updates: Map<PrayerSlot, PrayerStatus>,
    val overwriteCount: Int,
)

data class CalendarV4UiState(
    val today: LocalDate,
    val visibleMonth: YearMonth,
    val trackingStart: LocalDate,
    val entries: Map<LocalDate, Map<PrayerType, PrayerEntry>> = emptyMap(),
    val selectedTimes: Map<PrayerType, LocalTime>? = null,
    val todayTimes: Map<PrayerType, LocalTime>? = null,
    val gender: Gender? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val qazaOnly: Boolean = false,
    val prayerFilter: PrayerType? = null,
    val daySheetDate: LocalDate? = null,
    val isEditMode: Boolean = false,
    val selectedSlots: Set<PrayerSlot> = emptySet(),
    val fillEmptyOnly: Boolean = false,
    val preview: CalendarBatchPreview? = null,
    val undoCount: Int? = null,
    val errorRes: Int? = null,
) {
    val awayFromToday: Boolean get() = visibleMonth != YearMonth.from(today)
    val canGoPrevious: Boolean get() = visibleMonth > YearMonth.from(trackingStart)
    val canGoNext: Boolean get() = visibleMonth < YearMonth.from(today)
    val dayEntries: Map<PrayerType, PrayerEntry>
        get() = daySheetDate?.let { entries[it].orEmpty() }.orEmpty()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarV4ViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderNotifications: ReminderNotifications,
    private val clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val _uiState = MutableStateFlow(
        CalendarV4UiState(today, YearMonth.from(today), today),
    )
    val uiState: StateFlow<CalendarV4UiState> = _uiState.asStateFlow()
    private var undo: PrayerBatchUndo? = null
    private var undoJob: Job? = null

    init {
        viewModelScope.launch {
            val first = prayerRepository.getFirstRecordedPrayerDate() ?: today
            val times = prayerRepository.getPrayerTimes(today)
            _uiState.update { it.copy(trackingStart = first.coerceAtMost(today), todayTimes = times) }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().map { it.gender }.distinctUntilChanged().collect { gender ->
                _uiState.update { it.copy(gender = gender) }
            }
        }
        observeVisibleEntries()
    }

    private fun observeVisibleEntries() {
        viewModelScope.launch {
            _uiState.map { it.visibleMonth }.distinctUntilChanged()
                .flatMapLatest { month ->
                    val dates = calendarMonthGridDates(month)
                    prayerRepository.observeMonthEntries(dates.first(), dates.last())
                }
                .catch { _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } }
                .collect { byDate ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            entries = byDate.mapValues { (_, rows) -> rows.associateBy { it.prayerType } },
                        )
                    }
                }
        }
    }

    fun onPreviousMonth() = _uiState.update { state ->
        if (!state.canGoPrevious) state else state.copy(visibleMonth = state.visibleMonth.minusMonths(1))
    }

    fun onNextMonth() = _uiState.update { state ->
        if (!state.canGoNext) state else state.copy(visibleMonth = state.visibleMonth.plusMonths(1))
    }

    fun onTodayClick() = _uiState.update { it.copy(visibleMonth = YearMonth.from(it.today)) }

    fun onOpenDay(date: LocalDate) {
        val bounded = date.coerceIn(_uiState.value.trackingStart, today)
        _uiState.update { it.copy(visibleMonth = YearMonth.from(bounded), daySheetDate = bounded) }
        loadDayTimes(bounded)
    }

    fun onDayTapped(date: LocalDate) {
        val state = _uiState.value
        if (date < state.trackingStart || date > state.today) return
        if (state.isEditMode) toggleDay(date) else {
            _uiState.update { it.copy(daySheetDate = date) }
            loadDayTimes(date)
        }
    }

    private fun loadDayTimes(date: LocalDate) = viewModelScope.launch {
        val times = prayerRepository.getPrayerTimes(date)
        _uiState.update { if (it.daySheetDate == date) it.copy(selectedTimes = times) else it }
    }

    fun dismissDaySheet() = _uiState.update { it.copy(daySheetDate = null, selectedTimes = null) }
    fun toggleQazaOnly() = _uiState.update { it.copy(qazaOnly = !it.qazaOnly, selectedSlots = emptySet()) }
    fun setPrayerFilter(type: PrayerType?) = _uiState.update { it.copy(prayerFilter = type, selectedSlots = emptySet()) }

    fun toggleEditMode() = _uiState.update { state ->
        state.copy(
            isEditMode = !state.isEditMode,
            selectedSlots = emptySet(),
            preview = null,
            fillEmptyOnly = false,
            daySheetDate = null,
        )
    }

    private fun toggleDay(date: LocalDate) {
        val state = _uiState.value
        val types = state.prayerFilter?.let(::listOf) ?: PrayerType.values().toList()
        val slots = types.map { PrayerSlot(date, it) }.filter { isSelectableSlot(it, state) }.toSet()
        val remove = slots.isNotEmpty() && slots.all { it in state.selectedSlots }
        _uiState.update { it.copy(selectedSlots = if (remove) it.selectedSlots - slots else it.selectedSlots + slots) }
    }

    fun selectRange(start: LocalDate, end: LocalDate) {
        val state = _uiState.value
        val first = minOf(start, end).coerceAtLeast(state.trackingStart)
        val last = maxOf(start, end).coerceAtMost(state.today)
        if (first > last) return
        viewModelScope.launch {
            val loaded = prayerRepository.observeMonthEntries(first, last).first()
                .mapValues { (_, rows) -> rows.associateBy { it.prayerType } }
            _uiState.update { current ->
                val merged = current.copy(entries = current.entries + loaded)
                val types = merged.prayerFilter?.let(::listOf) ?: PrayerType.values().toList()
                val slots = generateSequence(first) { it.plusDays(1).takeIf { next -> next <= last } }
                    .flatMap { date -> types.asSequence().map { PrayerSlot(date, it) } }
                    .filter { isSelectableSlot(it, merged) }
                    .toSet()
                merged.copy(selectedSlots = merged.selectedSlots + slots)
            }
        }
    }

    fun selectCatchUp() {
        val state = _uiState.value
        val monthDates = (1..state.visibleMonth.lengthOfMonth()).map(state.visibleMonth::atDay)
        val types = state.prayerFilter?.let(::listOf) ?: PrayerType.values().toList()
        val slots = monthDates.flatMap { date -> types.map { PrayerSlot(date, it) } }
            .filter { isSlotEligible(it, state) && statusOf(it, state) == PrayerStatus.NOT_RECORDED }
            .toSet()
        _uiState.update { it.copy(isEditMode = true, selectedSlots = slots) }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedSlots = emptySet()) }
    fun setFillEmptyOnly(value: Boolean) {
        val status = _uiState.value.preview?.status
        _uiState.update { it.copy(fillEmptyOnly = value) }
        if (status != null) prepareBatch(status)
    }

    fun prepareBatch(status: PrayerStatus) {
        val state = _uiState.value
        val updates = state.selectedSlots.mapNotNull { slot ->
            val current = statusOf(slot, state)
            val allowed = current != status && (!state.fillEmptyOnly || current == PrayerStatus.NOT_RECORDED) &&
                (status != PrayerStatus.QAZA_COMPLETED || current == PrayerStatus.QAZA_PENDING)
            if (allowed) slot to status else null
        }.toMap()
        val overwriteCount = updates.keys.count { statusOf(it, state) != PrayerStatus.NOT_RECORDED }
        _uiState.update { it.copy(preview = CalendarBatchPreview(status, updates, overwriteCount)) }
    }

    fun dismissPreview() = _uiState.update { it.copy(preview = null) }

    fun applyPreview() {
        val preview = _uiState.value.preview ?: return
        if (preview.updates.isEmpty()) { dismissPreview(); return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { prayerRepository.applyPrayerBatch(preview.updates) }
                .onSuccess { snapshot ->
                    undo = snapshot
                    undoJob?.cancel()
                    undoJob = viewModelScope.launch {
                        delay(10_000)
                        undo = null
                        _uiState.update { it.copy(undoCount = null) }
                    }
                    _uiState.update {
                        it.copy(isSaving = false, preview = null, selectedSlots = emptySet(), undoCount = preview.updates.size)
                    }
                }
                .onFailure { _uiState.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) } }
        }
    }

    fun undoBatch() {
        val snapshot = undo ?: return
        undo = null
        undoJob?.cancel()
        viewModelScope.launch {
            runCatching { prayerRepository.restorePrayerBatch(snapshot) }
                .onSuccess { _uiState.update { it.copy(undoCount = null) } }
                .onFailure { _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    fun saveDayStatus(type: PrayerType, status: PrayerStatus) {
        val state = _uiState.value
        val date = state.daySheetDate ?: return
        if (!isSlotEligible(PrayerSlot(date, type), state) || state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { prayerRepository.upsertPrayerStatus(date, type, status, loggedLate = true) }
                .onSuccess {
                    if (date == today) reminderNotifications.dismiss(type)
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { _uiState.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

    private fun statusOf(slot: PrayerSlot, state: CalendarV4UiState): PrayerStatus =
        state.entries[slot.date]?.get(slot.prayerType)?.status ?: PrayerStatus.NOT_RECORDED

    private fun isSlotEligible(slot: PrayerSlot, state: CalendarV4UiState): Boolean {
        if (slot.date < state.trackingStart || slot.date > state.today) return false
        if (slot.date < state.today) return true
        return isPrayerMarkable(slot.date, state.todayTimes?.get(slot.prayerType), state.today, LocalTime.now(clock))
    }

    private fun isSelectableSlot(slot: PrayerSlot, state: CalendarV4UiState): Boolean =
        isSlotEligible(slot, state) && (!state.qazaOnly || statusOf(slot, state) in
            listOf(PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED))
}

/** Monday-first six-week grid used by Calendar v4. */
fun calendarMonthGridDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    return (0 until 42).map { start.plusDays(it.toLong()) }
}
