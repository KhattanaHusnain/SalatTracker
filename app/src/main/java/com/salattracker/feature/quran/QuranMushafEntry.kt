package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranMushafEntry(
    onPageControlsChanged: (QuranPageControls) -> Unit,
    viewModel: QuranMushafViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.pageNumber) {
        onPageControlsChanged(
            QuranPageControls(
                pageNumber = uiState.pageNumber,
                hasPreviousPage = viewModel.hasPreviousPage,
                hasNextPage = viewModel.hasNextPage,
                onPreviousPage = viewModel::onPreviousPage,
                onNextPage = viewModel::onNextPage,
            )
        )
    }
    QuranMushafScreen(uiState = uiState)
}
