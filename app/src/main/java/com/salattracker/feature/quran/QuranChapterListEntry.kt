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
    viewModel: QuranChapterListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
        onTabSelected = viewModel::onTabSelected,
        onJumpDialogOpen = viewModel::onJumpDialogOpen,
        onJumpDialogDismiss = viewModel::onJumpDialogDismiss,
        onJumpTypeSelected = viewModel::onJumpTypeSelected,
        onJumpConfirm = viewModel::onJumpConfirm,
    )
}
