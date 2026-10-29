package com.salattracker.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.DayLog
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.domain.usecase.GoalStreak
import com.salattracker.domain.usecase.GoalTotals
import com.salattracker.domain.usecase.dayIndex
import com.salattracker.domain.usecase.goalDayState
import com.salattracker.domain.usecase.goalStreak
import com.salattracker.domain.usecase.goalTotals
import com.salattracker.domain.usecase.statusOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class GoalDetailsUiState(
    val isLoading: Boolean = true,
    val goal: SpiritualGoal? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val streak: GoalStreak = GoalStreak(0, 0),
    val totals: GoalTotals = GoalTotals(0, 0, 0, 0),
    val dayIndex: Int? = null,
    val visibleMonth: YearMonth = YearMonth.now(),
    val canGoPrevMonth: Boolean = false,
    val canGoNextMonth: Boolean = false,
    val cells: List<GoalDayUi> = emptyList(),
    val showDeleteConfirm: Boolean = false,
    val deleted: Boolean = false,
    val errorRes: Int? = null,
)

/** 42 dates covering [month], aligned to the Monday-first Goals v2 calendar. */
internal fun goalMonthGridDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.minusDays(first.dayOfWeek.value - 1L)
    return (0 until 42).map { start.plusDays(it.toLong()) }
}

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val reminderScheduler: GoalReminderScheduler,
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle["goalId"])
    private val visibleMonth = MutableStateFlow(YearMonth.now(clock))
    private val _uiState = MutableStateFlow(GoalDetailsUiState(visibleMonth = YearMonth.now(clock)))
    val uiState: StateFlow<GoalDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(goalRepository.observeGoalHistory(goalId), visibleMonth) { history, month ->
                history to month
            }.collect { (history, month) -> render(history, month) }
        }
    }

    private fun render(history: GoalHistory?, month: YearMonth) {
        if (history == null) {
            _uiState.update { it.copy(isLoading = false, goal = null) }
            return
        }
        val goal = history.goal
        val today = LocalDate.now(clock)
        val log = DayLog(history.checkIns)
        val cells = goalMonthGridDates(month).map { date ->
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

    fun onArchiveToggle() {
        val archive = _uiState.value.goal?.isActive ?: return
        viewModelScope.launch {
            runCatching {
                goalRepository.setArchived(goalId, archive)
                if (archive) reminderScheduler.cancel(goalId) else reminderScheduler.rescheduleAll()
            }.onFailure { _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
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
