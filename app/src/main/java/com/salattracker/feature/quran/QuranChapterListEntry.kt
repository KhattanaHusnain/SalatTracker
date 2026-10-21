package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuranChapterListEntry(
    onChapterClick: (Int) -> Unit,
    onContinueReadingClick: (chapterNumber: Int, verseNumber: Int) -> Unit,
    onJuzClick: (chapterNumber: Int, verseNumber: Int) -> Unit,
    searchRequest: Int,
    onSearchRequestConsumed: () -> Unit,
    viewModel: QuranChapterListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) {
            viewModel.onJumpDialogOpen()
            onSearchRequestConsumed()
        }
    }

    LaunchedEffect(uiState.navigateToChapter) {
        uiState.navigateToChapter?.let { chapterNumber ->
            onChapterClick(chapterNumber)
            viewModel.onNavigationConsumed()
        }
    }

    QuranChapterListScreen(
        uiState = uiState,
        onChapterClick = onChapterClick,
        onContinueReadingClick = onContinueReadingClick,
        onJuzClick = onJuzClick,
        onTabSelected = viewModel::onTabSelected,
        onJumpDialogDismiss = viewModel::onJumpDialogDismiss,
        onJumpTypeSelected = viewModel::onJumpTypeSelected,
        onJumpConfirm = viewModel::onJumpConfirm,
    )
}
