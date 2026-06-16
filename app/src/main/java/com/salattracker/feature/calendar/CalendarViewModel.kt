package com.salattracker.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

// ── UI models ─────────────────────────────────────────────────────────────────

/**
 * The dot colour category shown under each day cell.
 * Maps to five dots (one per prayer) with distinct colours.
 */
enum class DotColor {
    GREEN,   // CONGREGATION or INDIVIDUAL (on time)
    YELLOW,  // INDIVIDUAL (secondary)
    RED,     // QAZA_PENDING
    PURPLE,  // QAZA_COMPLETED
    GREY,    // NOT_RECORDED / MENSTRUATION
}

data class DayCell(
    val date: LocalDate,
    val dots: List<DotColor>,          // exactly 5, one per prayer in order
    val completionPct: Int,            // 0-100
    val isToday: Boolean,
    val isSelected: Boolean,
)

data class DayDetail(
    val date: LocalDate,
    val hijriLabel: String,
    val completionPct: Int,
    val prayers: List<PrayerDetailUi>,
)

data class PrayerDetailUi(
    val type: PrayerType,
    val time: String,
    val status: PrayerStatus,
)

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val days: List<DayCell> = emptyList(),
    val selectedDay: DayDetail? = null,
    val isLoadingMonth: Boolean = true,
    val isLoadingDay: Boolean = false,
    // Bottom sheet
    val sheetArgs: PrayerStatusSheetArgs? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
        onDaySelected(LocalDate.now())
    }

    // ── Month navigation ──────────────────────────────────────────────────────

    fun onPreviousMonth() {
        val prev = _uiState.value.visibleMonth.minusMonths(1)
        _uiState.update { it.copy(visibleMonth = prev, days = emptyList(), isLoadingMonth = true) }
        loadMonth(prev)
    }

    fun onNextMonth() {
        val next = _uiState.value.visibleMonth.plusMonths(1)
        _uiState.update { it.copy(visibleMonth = next, days = emptyList(), isLoadingMonth = true) }
        loadMonth(next)
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            val today    = LocalDate.now()
            val selected = _uiState.value.selectedDay?.date
            val first    = month.atDay(1)
            val last     = month.atEndOfMonth()

            prayerRepository.observeMonthEntries(first, last)
                .catch { e -> _uiState.update { it.copy(isLoadingMonth = false, error = e.message) } }
                .collect { entriesByDate ->
                    val days = (1..month.lengthOfMonth()).map { d ->
                        val date    = month.atDay(d)
                        val entries = entriesByDate[date] ?: emptyList()
                        DayCell(
                            date          = date,
                            dots          = PrayerType.values().map { type ->
                                entries.find { it.prayerType == type }?.status.toDotColor()
                            },
                            completionPct = calcCompletion(entries.map { it.status }),
                            isToday       = date == today,
                            isSelected    = date == selected,
                        )
                    }
                    _uiState.update { it.copy(isLoadingMonth = false, days = days) }
                }
        }
    }

    // ── Day selection ─────────────────────────────────────────────────────────

    fun onDaySelected(date: LocalDate) {
        // Update selection highlight immediately
        _uiState.update { state ->
            state.copy(
                isLoadingDay = true,
                days = state.days.map { it.copy(isSelected = it.date == date) },
            )
        }
        viewModelScope.launch {
            val entries = prayerRepository.getPrayerEntriesForDate(date)
            val times   = prayerRepository.getCachedPrayerTimes(date)
            val detail  = DayDetail(
                date          = date,
                hijriLabel    = prayerRepository.getHijriLabel(date),
                completionPct = calcCompletion(entries.map { it.status }),
                prayers       = PrayerType.values().map { type ->
                    PrayerDetailUi(
                        type   = type,
                        time   = times?.get(type)?.toString() ?: "--:--",
                        status = entries.find { it.prayerType == type }?.status
                            ?: PrayerStatus.NOT_RECORDED,
                    )
                },
            )
            _uiState.update { it.copy(isLoadingDay = false, selectedDay = detail) }
        }
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────

    fun onPrayerRowClick(type: PrayerType) {
        val day     = _uiState.value.selectedDay ?: return
        val current = day.prayers.find { it.type == type }?.status ?: PrayerStatus.NOT_RECORDED
        _uiState.update {
            it.copy(
                sheetArgs = PrayerStatusSheetArgs(
                    prayerType    = type,
                    date          = day.date,
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
                _uiState.update { it.copy(isSaving = false, sheetArgs = null) }
                // Refresh day detail and month dots
                onDaySelected(args.date)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun PrayerStatus?.toDotColor() = when (this) {
        PrayerStatus.CONGREGATION   -> DotColor.GREEN
        PrayerStatus.INDIVIDUAL     -> DotColor.YELLOW
        PrayerStatus.QAZA_PENDING   -> DotColor.RED
        PrayerStatus.QAZA_COMPLETED -> DotColor.PURPLE
        else                        -> DotColor.GREY
    }

    private fun calcCompletion(statuses: List<PrayerStatus>): Int {
        if (statuses.isEmpty()) return 0
        val done = statuses.count {
            it == PrayerStatus.CONGREGATION || it == PrayerStatus.INDIVIDUAL
        }
        return (done * 100) / 5
    }

    // Close the day detail sheet (sets selectedDay to null)
    fun onDayDetailDismiss() = _uiState.update { it.copy(selectedDay = null) }
}

