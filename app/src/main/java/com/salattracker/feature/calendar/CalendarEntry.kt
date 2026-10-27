package com.salattracker.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point composable for the Calendar tab.
 *
 * [viewModel] defaults to a fresh [hiltViewModel] lookup, but MainScaffold passes its own
 * Activity-scoped instance explicitly instead — the same one feeds the shared top bar's Today
 * and edit actions (see MainActivity's TopBar), so both need to observe and
 * act on one ViewModel, not two independent copies.
 *
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Calendar.route) { CalendarEntry(calendarViewModel) }
 */
@Composable
fun CalendarEntry(
    viewModel: CalendarV4ViewModel = hiltViewModel(),
    onOpenQaza: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarV4Screen(
        state = uiState,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onDayTapped = viewModel::onDayTapped,
        onQazaOnly = viewModel::toggleQazaOnly,
        onPrayerFilter = viewModel::setPrayerFilter,
        onEditToggle = viewModel::toggleEditMode,
        onClearSelection = viewModel::clearSelection,
        onSelectRange = viewModel::selectRange,
        onCatchUp = viewModel::selectCatchUp,
        onPrepareBatch = viewModel::prepareBatch,
        onFillEmptyOnly = viewModel::setFillEmptyOnly,
        onDismissPreview = viewModel::dismissPreview,
        onApplyPreview = viewModel::applyPreview,
        onUndo = viewModel::undoBatch,
        onDismissDay = viewModel::dismissDaySheet,
        onSaveDayStatus = viewModel::saveDayStatus,
        onErrorDismiss = viewModel::clearError,
        onOpenQaza = onOpenQaza,
    )
}
