package com.salattracker.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.VerseRepository
import com.salattracker.domain.usecase.isPrayerMarkable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class DotColor { GREEN, YELLOW, RED, PURPLE, GREY }

data class DayCell(
    val date: LocalDate,
    val dots: List<DotColor>,
    val completionPct: Int,
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
    val isMarkable: Boolean,
)

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val days: List<DayCell> = emptyList(),
    val selectedDay: DayDetail? = null,
    val isLoadingMonth: Boolean = true,
    val isLoadingDay: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val verse: VerseInfo? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val verseRepository: VerseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
        loadVerse()
    }

    private fun loadVerse() {
        viewModelScope.launch {
            _uiState.update { it.copy(verse = verseRepository.getRandomVerse()) }
        }
    }

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

    fun onDaySelected(date: LocalDate) {
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
                        isMarkable = isPrayerMarkable(date, times?.get(type)),
                    )
                },
            )
            _uiState.update { it.copy(isLoadingDay = false, selectedDay = detail) }
        }
    }

    fun onDayDetailDismiss() = _uiState.update { it.copy(selectedDay = null) }

    fun onStatusSave(prayerType: PrayerType, newStatus: PrayerStatus) {
        val day = _uiState.value.selectedDay ?: return
        val prayerDetail = day.prayers.find { it.type == prayerType } ?: return
        if (!prayerDetail.isMarkable) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                prayerRepository.upsertPrayerStatus(day.date, prayerType, newStatus)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onDaySelected(day.date)
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

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
}