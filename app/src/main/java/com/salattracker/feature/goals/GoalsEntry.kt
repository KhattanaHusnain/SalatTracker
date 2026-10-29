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
    onAddGoalClick: (GoalTemplate?) -> Unit,
    onDayClick: (Long, java.time.LocalDate) -> Unit,
    onTasbihClick: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-derive "today" each time the tab is entered, so the strip rolls over after midnight.
    LaunchedEffect(Unit) { viewModel.refresh() }

    GoalsScreen(
        uiState             = uiState,
        onGoalClick         = onGoalClick,
        onAddGoalClick      = onAddGoalClick,
        onDayClick          = onDayClick,
        onQuickAdd          = viewModel::onQuickAdd,
        onTasbihClick        = onTasbihClick,
        onErrorDismiss      = viewModel::clearError,
    )
}
