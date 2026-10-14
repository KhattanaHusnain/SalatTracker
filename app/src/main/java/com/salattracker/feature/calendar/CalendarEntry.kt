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
 * icon and Month/Week/Year dropdown (see MainActivity's TopBar), so both need to observe and
 * act on one ViewModel, not two independent copies.
 *
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Calendar.route) { CalendarEntry(calendarViewModel) }
 */
@Composable
fun CalendarEntry(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarScreen(
        uiState              = uiState,
        onPreviousPeriod     = viewModel::onPreviousPeriod,
        onNextPeriod         = viewModel::onNextPeriod,
        onDatePicked         = viewModel::onDatePicked,
        onDaySelected        = viewModel::onDaySelected,
        onMonthTileClick     = viewModel::onMonthTileClick,
        onBackFromYearDrillDown = viewModel::onBackFromMonthDrillDown,
        onPrayerClick        = viewModel::onPrayerClick,
        onStatusSheetDismiss = viewModel::onStatusSheetDismiss,
        onStatusSave         = viewModel::onStatusSave,
        onErrorDismiss       = viewModel::clearError,
    )
}
