package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the goal details screen.
 *   composable("goal_details/{goalId}") { GoalDetailsEntry(onEdit = { navController.push(Routes.editGoal(it)) }, onDeleted = { navController.popBackStack() }) }
 */
@Composable
fun GoalDetailsEntry(
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: GoalDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Also leave when the goal no longer exists (e.g. it was deleted from the Edit screen above us).
    val gone = uiState.deleted || (!uiState.isLoading && uiState.goal == null)
    LaunchedEffect(gone) {
        if (gone) onDeleted()
    }

    GoalDetailsScreen(
        uiState                = uiState,
        onPrevMonth            = viewModel::onPrevMonth,
        onNextMonth            = viewModel::onNextMonth,
        onDayClick             = viewModel::onDayClick,
        onEditClick            = { uiState.goal?.let { onEdit(it.id) } },
        onArchiveToggle        = viewModel::onArchiveToggle,
        onDeleteClick          = viewModel::onDeleteClick,
        onConfirmDelete        = viewModel::onConfirmDelete,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onLogAmountChanged     = viewModel::onLogAmountChanged,
        onLogSkippedChanged    = viewModel::onLogSkippedChanged,
        onLogMarkDone          = viewModel::onLogMarkDone,
        onLogSave              = viewModel::onLogSave,
        onLogClear             = viewModel::onLogClear,
        onLogDismiss           = viewModel::onLogDismiss,
        onErrorDismiss         = viewModel::clearError,
    )
}
