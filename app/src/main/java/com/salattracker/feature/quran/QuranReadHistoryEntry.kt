package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranReadHistoryEntry(
    onEntryClick: (Int) -> Unit,
    viewModel: QuranReadHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    QuranReadHistoryScreen(uiState = uiState, onEntryClick = onEntryClick, onClearHistory = viewModel::onClearHistory)
}
