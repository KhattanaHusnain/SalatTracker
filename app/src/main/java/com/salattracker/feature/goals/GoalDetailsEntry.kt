package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.domain.usecase.GoalStatus
import java.time.LocalDate

data class GoalDetailsTopBarControls(
    val title: String,
    val status: GoalStatus,
    val onEdit: () -> Unit,
    val onArchiveToggle: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun GoalDetailsEntry(
    onEdit: (Long) -> Unit,
    onLogDay: (Long, LocalDate) -> Unit,
    onDeleted: () -> Unit,
    onTopBarControls: (GoalDetailsTopBarControls?) -> Unit,
    viewModel: GoalDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val gone = uiState.deleted || (!uiState.isLoading && uiState.goal == null)
    LaunchedEffect(gone) { if (gone) onDeleted() }

    val controls = uiState.goal?.let { goal ->
        remember(goal.id, goal.name, uiState.status) {
            GoalDetailsTopBarControls(
                title = goal.name,
                status = uiState.status,
                onEdit = { onEdit(goal.id) },
                onArchiveToggle = viewModel::onArchiveToggle,
                onDelete = viewModel::onDeleteClick,
            )
        }
    }
    LaunchedEffect(controls) { onTopBarControls(controls) }
    DisposableEffect(Unit) { onDispose { onTopBarControls(null) } }

    GoalDetailsScreen(
        uiState = uiState,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onDayClick = { date -> uiState.goal?.let { onLogDay(it.id, date) } },
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onErrorDismiss = viewModel::clearError,
    )
}
