package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranBookmarksEntry(
    onBookmarkClick: (Int) -> Unit,
    viewModel: QuranBookmarksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    QuranBookmarksScreen(uiState = uiState, onBookmarkClick = onBookmarkClick, onDeleteClick = viewModel::onDelete)
}
