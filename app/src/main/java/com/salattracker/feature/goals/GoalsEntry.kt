package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Spiritual Goals dashboard.
 * Wire into MainActivity NavHost:
 *   composable("goals") { GoalsEntry(onGoalClick = { navController.push(Routes.goalDetails(it)) }, onAddGoalClick = { ... }) }
 */
@Composable
fun GoalsEntry(
    onGoalClick: (Long) -> Unit,
    onAddGoalClick: () -> Unit,
    onTasbihClick: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-derive "today" each time the tab is entered, so the strip rolls over after midnight.
    LaunchedEffect(Unit) { viewModel.refresh() }

    GoalsScreen(
        uiState             = uiState,
        onDayTap            = viewModel::onDayTap,
        onDayLongPress      = viewModel::onDayLongPress,
        onGoalClick         = onGoalClick,
        onAddGoalClick      = onAddGoalClick,
        onTasbihClick        = onTasbihClick,
        onLogAmountChanged  = viewModel::onLogAmountChanged,
        onLogSkippedChanged = viewModel::onLogSkippedChanged,
        onLogMarkDone       = viewModel::onLogMarkDone,
        onLogSave           = viewModel::onLogSave,
        onLogClear          = viewModel::onLogClear,
        onLogDismiss        = viewModel::onLogDismiss,
        onErrorDismiss      = viewModel::clearError,
    )
}
