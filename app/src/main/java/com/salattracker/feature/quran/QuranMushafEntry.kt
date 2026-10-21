package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranMushafEntry(viewModel: QuranMushafViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    QuranMushafScreen(
        uiState = uiState,
        onPreviousPage = viewModel::onPreviousPage,
        onNextPage = viewModel::onNextPage,
    )
}
