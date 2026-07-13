package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Edit Goal form.
 * Wire into MainActivity NavHost:
 *   composable("edit_goal/{goalId}") { backStackEntry ->
 *       EditGoalEntry(onSaved = { navController.popBackStack() }, onDeleted = { navController.popBackStack() })
 *   }
 */
@Composable
fun EditGoalEntry(
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditGoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }

    EditGoalScreen(
        uiState                  = uiState,
        onNameChanged            = viewModel::onNameChanged,
        onCategoryChanged        = viewModel::onCategoryChanged,
        onFrequencyChanged       = viewModel::onFrequencyChanged,
        onTargetAmountChanged    = viewModel::onTargetAmountChanged,
        onUnitChanged            = viewModel::onUnitChanged,
        onReminderEnabledChanged = viewModel::onReminderEnabledChanged,
        onReminderTimeChanged    = viewModel::onReminderTimeChanged,
        onDurationDaysChanged    = viewModel::onDurationDaysChanged,
        onSaveClick              = viewModel::onSaveClick,
        onDeleteClick            = viewModel::onDeleteClick,
        onConfirmDelete          = viewModel::onConfirmDelete,
        onDismissDeleteConfirm   = viewModel::onDismissDeleteConfirm,
        onErrorDismiss           = viewModel::clearError,
        onClearSaveSuccess       = { viewModel.clearSaveSuccess(); onSaved() },
    )
}
