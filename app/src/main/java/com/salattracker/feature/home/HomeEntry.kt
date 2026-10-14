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
    onGoalsClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    onQazaClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        uiState           = uiState,
        onPrayerCardClick = viewModel::onPrayerCardClick,
        onSheetDismiss    = viewModel::onSheetDismiss,
        onStatusSave      = viewModel::onStatusSave,
        onGoalsClick      = onGoalsClick,
        onAddGoalClick    = onAddGoalClick,
        onQazaClick       = onQazaClick,
        onDayClick        = onDayClick,
    )
}
