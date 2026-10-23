package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Add Goal form.
 * Wire into MainActivity NavHost:
 *   composable("add_goal") { AddGoalEntry(onSaved = { navController.popBackStack() }) }
 */
@Composable
fun AddGoalEntry(onSaved: () -> Unit, viewModel: AddGoalViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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
            onUsesTasbihCounterChanged = viewModel::onUsesTasbihCounterChanged,
            onTemplateSelected       = viewModel::onTemplateSelected,
        )
    }

    AddGoalScreen(
        uiState            = uiState,
        actions            = actions,
        onCreateClick      = viewModel::onCreateClick,
        onErrorDismiss     = viewModel::clearError,
        onClearSaveSuccess = { viewModel.clearSaveSuccess(); onSaved() },
    )
}
