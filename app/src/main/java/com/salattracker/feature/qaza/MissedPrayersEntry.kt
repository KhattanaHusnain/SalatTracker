package com.salattracker.feature.qaza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the full "Missed Prayers" list.
 * Wire into MainActivity NavHost:
 *   composable(Routes.MISSED_PRAYERS, arguments = ...) {
 *       MissedPrayersEntry(
 *           onBackToDashboard = { navController.popBackStack() },
 *           onOpenDay = { date -> calendarViewModel.onOpenDay(date); navController.navigateToTab(BottomNavRoute.Calendar.route) },
 *       )
 *   }
 */
@Composable
fun MissedPrayersEntry(
    onBackToDashboard: () -> Unit,
    onOpenDay: (java.time.LocalDate) -> Unit,
    viewModel: MissedPrayersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MissedPrayersScreen(
        uiState           = uiState,
        onFilterSelected  = viewModel::onFilterSelected,
        onToggleSortOrder = viewModel::onToggleSortOrder,
        onSelectClick     = viewModel::onSelectClick,
        onSelectAll       = viewModel::onSelectAll,
        onClearSelection  = viewModel::onClearSelection,
        onCancelSelection = viewModel::onCancelSelection,
        onPillClick       = viewModel::onPillClick,
        onPillLongClick   = viewModel::onPillLongClick,
        onMakeUpDay       = viewModel::onMakeUpDay,
        onMakeUpSelected  = viewModel::onMakeUpSelected,
        onOpenDay         = onOpenDay,
        onUndo            = viewModel::onUndo,
        onUndoDismiss     = viewModel::onUndoDismiss,
        onBackToDashboard = onBackToDashboard,
        onErrorDismiss    = viewModel::clearError,
    )
}
