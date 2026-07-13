package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Add Goal form.
 * Wire into MainActivity NavHost:
 *   composable("add_goal") { AddGoalEntry(onSaved = { navController.popBackStack() }) }
 */
@Composable
fun AddGoalEntry(onSaved: () -> Unit, viewModel: AddGoalViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    AddGoalScreen(
        uiState                  = uiState,
        onNameChanged            = viewModel::onNameChanged,
        onCategoryChanged        = viewModel::onCategoryChanged,
        onFrequencyChanged       = viewModel::onFrequencyChanged,
        onTargetAmountChanged    = viewModel::onTargetAmountChanged,
        onUnitChanged            = viewModel::onUnitChanged,
        onReminderEnabledChanged = viewModel::onReminderEnabledChanged,
        onReminderTimeChanged    = viewModel::onReminderTimeChanged,
        onDurationDaysChanged    = viewModel::onDurationDaysChanged,
        onCreateClick            = viewModel::onCreateClick,
        onErrorDismiss           = viewModel::clearError,
        onClearSaveSuccess       = { viewModel.clearSaveSuccess(); onSaved() },
    )
}
