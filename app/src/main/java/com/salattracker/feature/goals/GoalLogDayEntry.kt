package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GoalLogDayEntry(
    onDone: () -> Unit,
    viewModel: GoalLogDayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    GoalLogDayScreen(
        state = state,
        onDecrement = viewModel::decrement,
        onIncrement = viewModel::increment,
        onMarkDone = viewModel::markDone,
        onSkippedChanged = viewModel::setSkipped,
        onClear = viewModel::clear,
        onDone = onDone,
        onErrorDismiss = viewModel::clearError,
    )
}
