package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranChapterInfoEntry(
    onReadClick: (Int) -> Unit,
    viewModel: QuranChapterInfoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    QuranChapterInfoScreen(uiState = uiState, onReadClick = onReadClick)
}
