package com.salattracker.feature.qaza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the full "Missed Prayers" list.
 * Wire into MainActivity NavHost:
 *   composable("missed_prayers") { MissedPrayersEntry(onBackToDashboard = { navController.popBackStack() }) }
 */
@Composable
fun MissedPrayersEntry(onBackToDashboard: () -> Unit, viewModel: MissedPrayersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    MissedPrayersScreen(
        uiState               = uiState,
        onFilterSelected      = viewModel::onFilterSelected,
        onToggleSortOrder     = viewModel::onToggleSortOrder,
        onSelectMultipleClick = viewModel::onSelectMultipleClick,
        onCancelSelection     = viewModel::onCancelSelection,
        onToggleEntrySelected = viewModel::onToggleEntrySelected,
        onMarkCompleted       = viewModel::onMarkCompleted,
        onCompleteSelected    = viewModel::onCompleteSelected,
        onBackToDashboard     = onBackToDashboard,
        onErrorDismiss        = viewModel::clearError,
    )
}
