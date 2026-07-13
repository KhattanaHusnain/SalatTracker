package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Spiritual Goals dashboard.
 * Wire into MainActivity NavHost:
 *   composable("goals") { GoalsEntry(onEditGoal = { goalId -> navController.navigate("edit_goal/$goalId") }) }
 */
@Composable
fun GoalsEntry(onEditGoal: (Long) -> Unit, viewModel: GoalsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    GoalsScreen(
        uiState                = uiState,
        onCheckIn              = viewModel::onCheckIn,
        onEditClick            = onEditGoal,
        onDeleteClick          = viewModel::onDeleteClick,
        onConfirmDelete        = viewModel::onConfirmDelete,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onErrorDismiss         = viewModel::clearError,
    )
}
