package com.salattracker.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.DayLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalLogDayUiState(
    val isLoading: Boolean = true,
    val goalName: String = "",
    val date: LocalDate = LocalDate.MIN,
    val amount: Int = 0,
    val target: Int = 1,
    val unit: String = "Time(s)",
    val skipped: Boolean = false,
    val canSkip: Boolean = false,
    val isSaving: Boolean = false,
    val errorRes: Int? = null,
)

private data class PendingGoalDay(val amount: Int, val skipped: Boolean)

@HiltViewModel
class GoalLogDayViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle["goalId"])
    private val date: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle["date"] as String?))
    private val writes = Channel<PendingGoalDay>(Channel.UNLIMITED)
    private var pendingWrites = 0

    private val _uiState = MutableStateFlow(GoalLogDayUiState(date = date))
    val uiState: StateFlow<GoalLogDayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            goalRepository.observeGoalHistory(goalId).collect { history ->
                if (history == null) {
                    _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_goal_not_found) }
                } else {
                    val log = DayLog(history.checkIns)
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            goalName = history.goal.name,
                            amount = if (pendingWrites == 0) log.amountOn(date) else current.amount,
                            target = history.goal.targetAmount,
                            unit = history.goal.unit,
                            skipped = if (pendingWrites == 0) log.isSkipped(date) else current.skipped,
                            canSkip = history.goal.frequency == GoalFrequency.DAILY,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            for (pending in writes) {
                runCatching { goalRepository.setDayLog(goalId, date, pending.amount, pending.skipped) }
                    .onFailure { _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
                pendingWrites = (pendingWrites - 1).coerceAtLeast(0)
                _uiState.update { it.copy(isSaving = pendingWrites > 0) }
            }
        }
    }

    fun decrement() {
        val state = _uiState.value
        if (state.isLoading || state.skipped || state.amount <= 0) return
        queue(state.amount - 1, false)
    }

    fun increment() {
        val state = _uiState.value
        if (state.isLoading || state.skipped || state.amount >= state.target) return
        queue(state.amount + 1, false)
    }

    fun markDone() {
        val state = _uiState.value
        if (!state.isLoading) queue(state.target, false)
    }

    fun setSkipped(skipped: Boolean) {
        val state = _uiState.value
        if (!state.canSkip) return
        queue(if (skipped) 0 else state.amount, skipped)
    }

    fun clear() = queue(0, false)

    private fun queue(amount: Int, skipped: Boolean) {
        pendingWrites++
        _uiState.update { it.copy(amount = amount, skipped = skipped, isSaving = true) }
        writes.trySend(PendingGoalDay(amount, skipped))
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}
