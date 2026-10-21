package com.salattracker.feature.bulkupdate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.usecase.isPrayerMarkable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class DurationOption(val labelRes: Int) {
    DAY(R.string.bulk_dur_day),
    WEEK(R.string.bulk_dur_week),
    MONTH(R.string.bulk_dur_month),
    CUSTOM(R.string.bulk_dur_custom),
}

/** Sunday-start week containing [date], matching the Sun–Sat grid used elsewhere in the app. */
private fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

private fun defaultRangeFor(duration: DurationOption, today: LocalDate): Pair<LocalDate, LocalDate> = when (duration) {
    DurationOption.DAY    -> today to today
    DurationOption.WEEK   -> weekStartOf(today).let { it to it.plusDays(6) }
    DurationOption.MONTH  -> YearMonth.from(today).let { it.atDay(1) to it.atEndOfMonth() }
    DurationOption.CUSTOM -> today.minusDays(6) to today
}

data class BulkUpdateUiState(
    val duration: DurationOption = DurationOption.WEEK,
    val rangeFrom: LocalDate = weekStartOf(LocalDate.now()),
    val rangeTo: LocalDate = weekStartOf(LocalDate.now()).plusDays(6),
    val selectedPrayers: Set<PrayerType> = PrayerType.values().toSet(),
    val selectedStatus: PrayerStatus = PrayerStatus.CONGREGATION,
    val showConfirmDialog: Boolean = false,
    val isApplying: Boolean = false,
    val applySuccess: Boolean = false,
    val skippedFutureCount: Int = 0,
    val error: String? = null,
) {
    /**
     * Week/Month are anchored to full calendar periods (Sun–Sat, 1st-to-last-day), so the
     * *current* period's true end can fall after today (e.g. today is Wednesday but the week
     * runs through Saturday). [rangeTo] must stay the true period boundary so Previous/Next
     * arithmetic keeps working once you navigate away from the current period — this is the
     * capped value everything display-facing (labels, the mini calendar, the day count) should
     * use instead, since prayers after today are never actually written regardless.
     */
    val effectiveRangeTo: LocalDate get() = minOf(rangeTo, LocalDate.now())

    val dayCount: Int get() = java.time.temporal.ChronoUnit.DAYS.between(rangeFrom, effectiveRangeTo).toInt() + 1

    /** Day/Week/Month can't step forward once the range already reaches today — that would only ever move into the future. */
    val canGoNext: Boolean get() = duration != DurationOption.CUSTOM && rangeTo.isBefore(LocalDate.now())
}

@HiltViewModel
class BulkUpdateViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The Qaza tab's "Add missed prayers" shortcut arrives here with `status = QAZA_PENDING`
     * preselected (via [com.salattracker.feature.main.Routes.bulkUpdate]); any other entry point
     * (the drawer, Calendar's top bar) has no `status` arg and keeps the Congregation default. A
     * blank or unrecognised value falls back to the default rather than crashing. */
    private val initialStatus: PrayerStatus = savedStateHandle.get<String>("status")
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { PrayerStatus.valueOf(it) }.getOrNull() }
        ?: PrayerStatus.CONGREGATION

    private val _uiState = MutableStateFlow(BulkUpdateUiState(selectedStatus = initialStatus))
    val uiState: StateFlow<BulkUpdateUiState> = _uiState.asStateFlow()

    fun onDurationChanged(duration: DurationOption) {
        _uiState.update { state ->
            val (from, to) = if (duration == DurationOption.CUSTOM) {
                state.rangeFrom to state.rangeTo
            } else {
                defaultRangeFor(duration, LocalDate.now())
            }
            state.copy(duration = duration, rangeFrom = from, rangeTo = to)
        }
    }

    /** Steps the current Day/Week/Month range back by one unit — a no-op for Custom (use the date pickers instead). */
    fun onPreviousRange() = shiftRange(-1)

    /** Steps the current Day/Week/Month range forward by one unit — a no-op for Custom (use the date pickers instead). */
    fun onNextRange() = shiftRange(1)

    private fun shiftRange(direction: Int) {
        _uiState.update { state ->
            if (direction > 0 && !state.canGoNext) return@update state
            val (from, to) = when (state.duration) {
                DurationOption.DAY   -> state.rangeFrom.plusDays(direction.toLong()).let { it to it }
                DurationOption.WEEK  -> state.rangeFrom.plusDays(7L * direction) to state.rangeTo.plusDays(7L * direction)
                DurationOption.MONTH -> YearMonth.from(state.rangeFrom).plusMonths(direction.toLong())
                    .let { it.atDay(1) to it.atEndOfMonth() }
                DurationOption.CUSTOM -> return@update state
            }
            state.copy(rangeFrom = from, rangeTo = to)
        }
    }

    fun onCustomRangeChanged(from: LocalDate, to: LocalDate) {
        val today = LocalDate.now()
        val (lo, hi) = if (from.isAfter(to)) to to from else from to to
        _uiState.update { it.copy(rangeFrom = minOf(lo, today), rangeTo = minOf(hi, today)) }
    }

    /** Tapping a date in the mini calendar — only meaningful in Day mode, and never for a future date. */
    fun onDaySelected(date: LocalDate) {
        _uiState.update { state ->
            if (state.duration == DurationOption.DAY && !date.isAfter(LocalDate.now())) {
                state.copy(rangeFrom = date, rangeTo = date)
            } else {
                state
            }
        }
    }

    fun onTogglePrayer(type: PrayerType) {
        _uiState.update { state ->
            val updated = if (type in state.selectedPrayers) state.selectedPrayers - type else state.selectedPrayers + type
            state.copy(selectedPrayers = updated)
        }
    }

    fun onSelectAllToggle() {
        _uiState.update { state ->
            val allSelected = state.selectedPrayers.size == PrayerType.values().size
            state.copy(selectedPrayers = if (allSelected) emptySet() else PrayerType.values().toSet())
        }
    }

    fun onStatusChanged(status: PrayerStatus) = _uiState.update { it.copy(selectedStatus = status) }

    fun onApplyClick() {
        if (_uiState.value.selectedPrayers.isEmpty()) return
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun onDismissConfirm() = _uiState.update { it.copy(showConfirmDialog = false) }

    fun onConfirmApply() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            runCatching {
                val today = LocalDate.now()
                val todayTimes = prayerRepository.getPrayerTimes(today)
                val dates = generateSequence(state.rangeFrom) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(state.rangeTo) }
                    .toList()
                val allPairs = dates.flatMap { date -> state.selectedPrayers.map { type -> date to type } }
                val (markable, skipped) = allPairs.partition { (date, type) ->
                    isPrayerMarkable(date, todayTimes?.get(type), today)
                }
                prayerRepository.bulkUpsertPrayerStatus(markable, state.selectedStatus)
                skipped.size
            }.onSuccess { skippedCount ->
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        showConfirmDialog = false,
                        applySuccess = true,
                        skippedFutureCount = skippedCount,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isApplying = false, showConfirmDialog = false, error = e.message) }
            }
        }
    }

    fun clearApplySuccess() = _uiState.update { it.copy(applySuccess = false, skippedFutureCount = 0) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
