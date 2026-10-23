package com.salattracker.feature.tasbih

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TasbihEntry(viewModel: TasbihViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    TasbihScreen(
        uiState = uiState,
        onIncrement = viewModel::increment,
        onUndo = viewModel::undo,
        onReset = viewModel::reset,
        onNewRound = viewModel::newRound,
        onConfigurationChanged = viewModel::updateConfiguration,
        onNoticeDismiss = viewModel::consumeNotice,
    )
}
