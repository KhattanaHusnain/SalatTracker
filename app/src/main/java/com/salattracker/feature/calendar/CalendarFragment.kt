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
        uiState            = uiState,
        onPreviousMonth    = viewModel::onPreviousMonth,
        onNextMonth        = viewModel::onNextMonth,
        onDaySelected      = viewModel::onDaySelected,
        onPrayerRowClick   = viewModel::onPrayerRowClick,
        onSheetDismiss     = viewModel::onSheetDismiss,
        onDayDetailDismiss = viewModel::onDayDetailDismiss,
        onStatusSave       = viewModel::onStatusSave,
    )
}