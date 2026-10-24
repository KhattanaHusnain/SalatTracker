package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.domain.model.QuranReadHistoryEntry

@Composable
fun QuranChapterListEntry(
    onChapterClick: (Int) -> Unit,
    onJuzClick: (juzNumber: Int, pageNumber: Int) -> Unit,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLastReadChanged: (QuranReadHistoryEntry?) -> Unit,
    searchRequest: Int,
    onSearchRequestConsumed: () -> Unit,
    viewModel: QuranChapterListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.continueReading) {
        onLastReadChanged(uiState.continueReading)
    }

    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) {
            viewModel.onSearchDialogOpen()
            onSearchRequestConsumed()
        }
    }

    QuranChapterListScreen(
        uiState = uiState,
        onChapterClick = onChapterClick,
        onJuzClick = onJuzClick,
        onBookmarksClick = onBookmarksClick,
        onHistoryClick = onHistoryClick,
        onTabSelected = viewModel::onTabSelected,
        onSearchDialogDismiss = viewModel::onSearchDialogDismiss,
    )
}
