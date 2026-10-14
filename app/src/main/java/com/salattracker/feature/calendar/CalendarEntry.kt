package com.salattracker.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point composable for the Calendar tab.
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Calendar.route) { CalendarEntry() }
 */
@Composable
fun CalendarEntry(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarScreen(
        uiState              = uiState,
        onViewChanged        = viewModel::onViewChanged,
        onPreviousPeriod     = viewModel::onPreviousPeriod,
        onNextPeriod         = viewModel::onNextPeriod,
        onTodayClick         = viewModel::onTodayClick,
        onDatePicked         = viewModel::onDatePicked,
        onDaySelected        = viewModel::onDaySelected,
        onGridDayClick       = viewModel::onGridDayClick,
        onDaySheetDismiss    = viewModel::onDaySheetDismiss,
        onPrayerClick        = viewModel::onPrayerClick,
        onStatusSheetDismiss = viewModel::onStatusSheetDismiss,
        onStatusSave         = viewModel::onStatusSave,
        onErrorDismiss       = viewModel::clearError,
    )
}
