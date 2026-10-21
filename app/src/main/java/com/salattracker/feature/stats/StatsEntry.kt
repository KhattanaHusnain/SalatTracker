package com.salattracker.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for Stats, reached from the app drawer.
 * In MainActivity NavHost:
 *   composable(Routes.STATS) { StatsEntry(onAddGoalClick = { ... }) }
 */
@Composable
fun StatsEntry(
    onAddGoalClick: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    StatsScreen(
        uiState         = uiState,
        onPeriodChanged = viewModel::onPeriodChanged,
        onAddGoalClick  = onAddGoalClick,
        onErrorDismiss  = viewModel::clearError,
    )
}
