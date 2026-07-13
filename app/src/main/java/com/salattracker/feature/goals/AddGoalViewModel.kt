package com.salattracker.feature.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.R
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.notification.GoalReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

val GOAL_UNITS = listOf("Time(s)", "Minutes", "Pages")

data class AddGoalUiState(
    val name: String = "",
    val category: GoalCategory = GoalCategory.SALAT,
    val frequency: GoalFrequency = GoalFrequency.DAILY,
    val targetAmount: String = "1",
    val unit: String = GOAL_UNITS.first(),
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(4, 30),
    val durationDays: String = "30",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddGoalUiState())
    val uiState: StateFlow<AddGoalUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }
    fun onCategoryChanged(category: GoalCategory) = _uiState.update { it.copy(category = category) }
    fun onFrequencyChanged(frequency: GoalFrequency) = _uiState.update { it.copy(frequency = frequency) }

    fun onTargetAmountChanged(value: String) {
        if (value.isEmpty() || value.all(Char::isDigit)) _uiState.update { it.copy(targetAmount = value) }
    }

    fun onUnitChanged(unit: String) = _uiState.update { it.copy(unit = unit) }
    fun onReminderEnabledChanged(enabled: Boolean) = _uiState.update { it.copy(reminderEnabled = enabled) }
    fun onReminderTimeChanged(time: LocalTime) = _uiState.update { it.copy(reminderTime = time) }

    fun onDurationDaysChanged(value: String) {
        if (value.isEmpty() || value.all(Char::isDigit)) _uiState.update { it.copy(durationDays = value) }
    }

    fun onCreateClick() {
        val state = _uiState.value
        if (state.isSaving || state.saveSuccess) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_goal_name)) }
            return
        }
        val amount = state.targetAmount.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val duration = state.durationDays.toIntOrNull()?.takeIf { it > 0 } ?: 30

        // Set synchronously (not inside the coroutine) so a rapid double-tap can't slip through
        // before the button's disabled state has a chance to recompose.
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runCatching {
                goalRepository.createGoal(
                    name            = state.name.trim(),
                    category        = state.category,
                    frequency       = state.frequency,
                    targetAmount    = amount,
                    unit            = state.unit,
                    reminderEnabled = state.reminderEnabled,
                    reminderTime    = if (state.reminderEnabled) state.reminderTime else null,
                    durationDays    = duration,
                )
            }.onSuccess {
                // A new/changed reminder invalidates whatever the periodic worker last scheduled — reschedule now.
                GoalReminderWorker.enqueueOneTime(workManager)
                // isSaving stays true (button stays disabled) until the success snackbar's OK closes the screen.
                _uiState.update { it.copy(saveSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSaveSuccess() = _uiState.update { it.copy(isSaving = false, saveSuccess = false) }
}
