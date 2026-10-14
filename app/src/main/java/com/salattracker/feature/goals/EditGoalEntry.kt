package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Edit Goal form.
 * Wire into MainActivity NavHost:
 *   composable("edit_goal/{goalId}") { EditGoalEntry(onSaved = { navController.popBackStack() }) }
 */
@Composable
fun EditGoalEntry(
    onSaved: () -> Unit,
    viewModel: EditGoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // No templates when editing an existing goal.
    val actions = remember(viewModel) {
        GoalFormActions(
            onNameChanged            = viewModel::onNameChanged,
            onCategoryChanged        = viewModel::onCategoryChanged,
            onFrequencyChanged       = viewModel::onFrequencyChanged,
            onTargetAmountChanged    = viewModel::onTargetAmountChanged,
            onUnitChanged            = viewModel::onUnitChanged,
            onDayToggled             = viewModel::onDayToggled,
            onEveryDaySelected       = viewModel::onEveryDaySelected,
            onReminderEnabledChanged = viewModel::onReminderEnabledChanged,
            onReminderTimeChanged    = viewModel::onReminderTimeChanged,
            onEndsAfterChanged       = viewModel::onEndsAfterChanged,
            onDurationDaysChanged    = viewModel::onDurationDaysChanged,
        )
    }

    EditGoalScreen(
        uiState            = uiState,
        actions            = actions,
        onSaveClick        = viewModel::onSaveClick,
        onErrorDismiss     = viewModel::clearError,
        onClearSaveSuccess = { viewModel.clearSaveSuccess(); onSaved() },
    )
}
