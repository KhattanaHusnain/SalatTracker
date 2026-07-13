package com.salattracker.feature.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.notification.GoalAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<GoalWithProgress> = emptyList(),
    val overallPercent: Int = 0,
    val deleteConfirmGoal: GoalWithProgress? = null,   // non-null = showing the "Delete Goal?" dialog
    val error: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init { observeGoals() }

    private fun observeGoals() {
        viewModelScope.launch {
            goalRepository.observeActiveGoalsWithProgress()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { goals ->
                    val overall = if (goals.isEmpty()) {
                        0
                    } else {
                        (goals.sumOf { it.progressFraction.toDouble() } / goals.size * 100).roundToInt()
                    }
                    _uiState.update { it.copy(isLoading = false, goals = goals, overallPercent = overall) }
                }
        }
    }

    fun onCheckIn(goalId: Long) {
        viewModelScope.launch {
            runCatching { goalRepository.checkIn(goalId) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun onDeleteClick(goalId: Long) {
        val goal = _uiState.value.goals.find { it.goal.id == goalId } ?: return
        _uiState.update { it.copy(deleteConfirmGoal = goal) }
    }

    fun onDismissDeleteConfirm() = _uiState.update { it.copy(deleteConfirmGoal = null) }

    fun onConfirmDelete() {
        val goalId = _uiState.value.deleteConfirmGoal?.goal?.id ?: return
        viewModelScope.launch {
            runCatching {
                goalRepository.deleteGoal(goalId)
                GoalAlarmScheduler(context).cancelFor(goalId)
            }.onSuccess {
                _uiState.update { it.copy(deleteConfirmGoal = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(deleteConfirmGoal = null, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
