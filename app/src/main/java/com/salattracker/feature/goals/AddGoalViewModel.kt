package com.salattracker.feature.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class AddGoalUiState(
    val form: GoalFormState = GoalFormState(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorRes: Int? = null,
)

@HiltViewModel
class AddGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val reminderScheduler: GoalReminderScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddGoalUiState())
    val uiState: StateFlow<AddGoalUiState> = _uiState.asStateFlow()

    private fun updateForm(transform: (GoalFormState) -> GoalFormState) =
        _uiState.update { it.copy(form = transform(it.form)) }

    fun onNameChanged(name: String) = updateForm { it.copy(name = name) }
    fun onCategoryChanged(category: GoalCategory) = updateForm { it.copy(category = category) }
    fun onFrequencyChanged(frequency: GoalFrequency) = updateForm { it.copy(frequency = frequency) }
    fun onTargetAmountChanged(value: String) = updateForm { it.withTargetAmount(value) }
    fun onUnitChanged(unit: String) = updateForm { it.copy(unit = unit) }
    fun onUsesTasbihCounterChanged(enabled: Boolean) = updateForm { it.copy(usesTasbihCounter = enabled) }
    fun onDayToggled(day: DayOfWeek) = updateForm { it.withDayToggled(day) }
    fun onEveryDaySelected() = updateForm { it.copy(daysOfWeek = emptySet()) }
    fun onReminderEnabledChanged(enabled: Boolean) = updateForm { it.copy(reminderEnabled = enabled) }
    fun onReminderTimeChanged(time: LocalTime) = updateForm { it.copy(reminderTime = time) }
    fun onEndsAfterChanged(ends: Boolean) = updateForm { it.copy(endsAfterDuration = ends) }
    fun onDurationDaysChanged(value: String) = updateForm { it.withDurationDays(value) }
    fun onTemplateSelected(template: GoalTemplate, localizedName: String) =
        updateForm { it.withTemplate(template, localizedName) }

    fun onCreateClick() {
        val state = _uiState.value
        if (state.isSaving || state.saveSuccess) return
        val form = state.form
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_goal_name)) }
            return
        }

        // Set synchronously (not inside the coroutine) so a rapid double-tap can't slip through
        // before the button's disabled state has a chance to recompose.
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runCatching {
                goalRepository.createGoal(
                    name              = form.name.trim(),
                    category          = form.category,
                    frequency         = form.frequency,
                    targetAmount      = form.amount,
                    unit              = form.unit,
                    reminderEnabled   = form.reminderEnabled,
                    reminderTime      = if (form.reminderEnabled) form.reminderTime else null,
                    durationDays      = form.duration,
                    daysOfWeek        = form.effectiveDays,
                    endsAfterDuration = form.endsAfterDuration,
                    usesTasbihCounter = form.usesTasbihCounter && form.category == GoalCategory.DHIKR && form.unit == "Time(s)",
                )
            }.onSuccess {
                // A new goal invalidates whatever the periodic worker last scheduled — reschedule now.
                reminderScheduler.rescheduleAll()
                // isSaving stays true (button stays disabled) until the success snackbar's OK closes the screen.
                _uiState.update { it.copy(saveSuccess = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
    fun clearSaveSuccess() = _uiState.update { it.copy(isSaving = false, saveSuccess = false) }
}
