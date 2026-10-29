package com.salattracker.feature.qaza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/** Nav entry-point for the five-page Qaza v10 flow. */
@Composable
fun QazaEntry(
    viewModel: QazaV10ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    QazaV10Screen(
        state = state,
        onOpen = viewModel::open,
        onBack = viewModel::back,
        onDailyGoal = viewModel::setDailyGoal,
        onOldestFirst = viewModel::setOldestFirst,
        onSound = viewModel::setSound,
        onHideBadge = viewModel::setHideBadge,
        onReducedMotion = viewModel::setReducedMotion,
        onMakeUp = viewModel::makeUp,
        onUndoLast = viewModel::undoLast,
        onUndoActivity = viewModel::undoActivity,
        onAddMode = viewModel::setAddMode,
        onAddDates = viewModel::setAddDates,
        onTogglePrayer = viewModel::togglePrayer,
        onAddCount = viewModel::setAddCount,
        onReason = viewModel::setReason,
        onPreviewAdd = viewModel::previewAdd,
        onConfirmAdd = viewModel::confirmAdd,
        onLogCount = viewModel::setLogCount,
        onConfirmLog = viewModel::confirmLog,
        onOlderYears = viewModel::setOlderYears,
        onExcusedDays = viewModel::setExcusedDays,
        onPreviewOlder = viewModel::previewOlder,
        onConfirmOlder = viewModel::confirmOlder,
        onClearMessage = viewModel::clearMessage,
    )
}
