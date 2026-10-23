package com.salattracker.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.GoalCheckIn
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.DayLog
import com.salattracker.domain.usecase.GoalDayState
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.domain.usecase.GoalStreak
import com.salattracker.domain.usecase.GoalTotals
import com.salattracker.domain.usecase.dayIndex
import com.salattracker.domain.usecase.goalDayState
import com.salattracker.domain.usecase.goalStreak
import com.salattracker.domain.usecase.goalTotals
import com.salattracker.domain.usecase.monthGridDates
import com.salattracker.domain.usecase.statusOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class GoalDetailsUiState(
    val isLoading: Boolean = true,
    val goal: SpiritualGoal? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val streak: GoalStreak = GoalStreak(0, 0),
    val totals: GoalTotals = GoalTotals(0, 0, 0, 0),
    /** 1-based "Day N" within an end-dated goal, null when ongoing. */
    val dayIndex: Int? = null,
    val visibleMonth: YearMonth = YearMonth.now(),
    val canGoPrevMonth: Boolean = false,
    val canGoNextMonth: Boolean = false,
    val cells: List<GoalDayUi> = emptyList(),
    val logSheet: LogSheetUi? = null,
    val showDeleteConfirm: Boolean = false,
    /** Set once the goal is deleted — the Entry navigates back on it. */
    val deleted: Boolean = false,
    val errorRes: Int? = null,
)

/** Builds the log sheet for [date], pre-filled with whatever is already logged that day. */
internal fun SpiritualGoal.logSheetFor(date: LocalDate, checkIns: List<GoalCheckIn>): LogSheetUi {
    val log = DayLog(checkIns)
    return LogSheetUi(
        goalId = id, goalName = name, date = date,
        amount = log.amountOn(date), skipped = log.isSkipped(date),
        target = targetAmount, unit = unit,
        canSkip = frequency == GoalFrequency.DAILY,
    )
}

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val reminderScheduler: GoalReminderScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle["goalId"])

    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(GoalDetailsUiState())
    val uiState: StateFlow<GoalDetailsUiState> = _uiState.asStateFlow()

    private var history: GoalHistory? = null

    init {
        viewModelScope.launch {
            combine(goalRepository.observeGoalHistory(goalId), visibleMonth) { h, month -> h to month }
                .collect { (h, month) ->
                    history = h
                    render(h, month)
                }
        }
    }

    private fun render(history: GoalHistory?, month: YearMonth) {
        if (history == null) {
            _uiState.update { it.copy(isLoading = false, goal = null) }
            return
        }
        val goal = history.goal
        val today = LocalDate.now()
        val log = DayLog(history.checkIns)
        val cells = monthGridDates(month).map { date ->
            val amount = log.amountOn(date)
            GoalDayUi(
                date = date,
                state = goalDayState(goal, date, amount, log.isSkipped(date), today),
                amount = amount,
                isToday = date == today,
                inMonth = YearMonth.from(date) == month,
            )
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                goal = goal,
                status = goal.statusOn(today),
                streak = goalStreak(goal, history.checkIns, today),
                totals = goalTotals(goal, history.checkIns, today),
                dayIndex = goal.dayIndex(today),
                visibleMonth = month,
                canGoPrevMonth = month > YearMonth.from(goal.startDate),
                canGoNextMonth = month < YearMonth.from(today),
                cells = cells,
            )
        }
    }

    fun onPrevMonth() {
        if (_uiState.value.canGoPrevMonth) visibleMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        if (_uiState.value.canGoNextMonth) visibleMonth.update { it.plusMonths(1) }
    }

    fun onDayClick(date: LocalDate) {
        val h = history ?: return
        val cell = _uiState.value.cells.firstOrNull { it.date == date } ?: return
        if (cell.state == GoalDayState.LOCKED) return
        _uiState.update { it.copy(logSheet = h.goal.logSheetFor(date, h.checkIns)) }
    }

    // ── Log sheet ────────────────────────────────────────────────────────────

    fun onLogAmountChanged(amount: Int) = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(amount = amount.coerceIn(0, MAX_LOG_AMOUNT)))
    }

    fun onLogSkippedChanged(skipped: Boolean) = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(skipped = skipped))
    }

    fun onLogMarkDone() = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(amount = s.logSheet.target, skipped = false))
    }

    fun onLogSave() {
        val sheet = _uiState.value.logSheet ?: return
        saveDay(sheet.date, sheet.amount, sheet.skipped)
    }

    fun onLogClear() {
        val sheet = _uiState.value.logSheet ?: return
        saveDay(sheet.date, 0, false)
    }

    fun onLogDismiss() = _uiState.update { it.copy(logSheet = null) }

    private fun saveDay(date: LocalDate, amount: Int, skipped: Boolean) {
        _uiState.update { it.copy(logSheet = null) }
        viewModelScope.launch {
            runCatching { goalRepository.setDayLog(goalId, date, amount, skipped) }
                .onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    // ── Archive / delete ─────────────────────────────────────────────────────

    fun onArchiveToggle() {
        val archive = _uiState.value.goal?.isActive ?: return
        viewModelScope.launch {
            runCatching {
                goalRepository.setArchived(goalId, archive)
                // Archiving stops reminders immediately; restoring re-evaluates every goal's next alarm.
                if (archive) reminderScheduler.cancel(goalId) else reminderScheduler.rescheduleAll()
            }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun onDismissDeleteConfirm() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun onConfirmDelete() {
        viewModelScope.launch {
            runCatching {
                goalRepository.deleteGoal(goalId)
                reminderScheduler.cancel(goalId)
            }.onSuccess {
                _uiState.update { it.copy(showDeleteConfirm = false, deleted = true) }
            }.onFailure {
                _uiState.update { it.copy(showDeleteConfirm = false, errorRes = R.string.error_save_generic) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}
