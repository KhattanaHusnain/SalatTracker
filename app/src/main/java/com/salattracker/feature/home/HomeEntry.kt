package com.salattracker.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate

/**
 * Nav entry-point composable for the Home tab.
 *
 * Follows the usual ViewModel -> Screen -> Entry split (see the android-mvvm-jetpack-compose
 * skill): [HomeScreen] stays stateless so it keeps its @Preview, and [viewModel] is looked up
 * once here via [hiltViewModel] rather than inline in MainActivity's NavHost.
 */
@Composable
fun HomeEntry(
    onQazaClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onMenuClick: () -> Unit = {},
    hostedBelowSharedTopBar: Boolean = false,
    onLocationClick: () -> Unit = {},
    onDonateClick: () -> Unit = {},
    onCatchUpClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        uiState           = uiState,
        onPrayerCardClick = viewModel::onPrayerCardClick,
        onSheetDismiss    = viewModel::onSheetDismiss,
        onStatusSave      = viewModel::onStatusSave,
        onQazaClick       = onQazaClick,
        onDayClick        = onDayClick,
        onQuickStatusSave = viewModel::onQuickStatusSave,
        onPastStatusSave = viewModel::onPastStatusSave,
        onReminderToggle = viewModel::onReminderToggle,
        onAppearanceChanged = viewModel::onAppearanceChanged,
        onErrorDismiss = viewModel::clearError,
        onMenuClick = onMenuClick,
        hostedBelowSharedTopBar = hostedBelowSharedTopBar,
        onLocationClick = onLocationClick,
        onDonateClick = onDonateClick,
        onCatchUpClick = onCatchUpClick,
    )
}
