package com.salattracker.feature.bulkupdate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Bulk Update screen.
 * Wire into MainActivity NavHost:
 *   composable("bulk_update") { BulkUpdateEntry() }
 */
@Composable
fun BulkUpdateEntry(viewModel: BulkUpdateViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    BulkUpdateScreen(
        uiState              = uiState,
        onDurationChanged    = viewModel::onDurationChanged,
        onPreviousRange      = viewModel::onPreviousRange,
        onNextRange          = viewModel::onNextRange,
        onDaySelected        = viewModel::onDaySelected,
        onCustomRangeChanged = viewModel::onCustomRangeChanged,
        onTogglePrayer       = viewModel::onTogglePrayer,
        onSelectAllToggle    = viewModel::onSelectAllToggle,
        onStatusChanged      = viewModel::onStatusChanged,
        onApplyClick         = viewModel::onApplyClick,
        onConfirmApply       = viewModel::onConfirmApply,
        onDismissConfirm     = viewModel::onDismissConfirm,
        onClearApplySuccess  = viewModel::clearApplySuccess,
        onErrorDismiss       = viewModel::clearError,
    )
}
