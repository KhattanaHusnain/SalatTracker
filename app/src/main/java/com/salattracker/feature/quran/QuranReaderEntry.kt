package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the reader, reached by tapping a chapter.
 * In MainActivity NavHost:
 *   composable(Routes.QURAN_READER) { QuranReaderEntry() }
 */
@Composable
fun QuranReaderEntry(
    onChapterInfoClick: (Int) -> Unit,
    /** Fires whenever this chapter's first page becomes known, so MainActivity's top bar
     * Mushaf-mode button (a route-level action outside this screen) knows which page to open —
     * see MainScaffold's `quranReaderPageNumber`. */
    onPageLoaded: (Int) -> Unit,
    viewModel: QuranReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.currentPageIndex, uiState.pages) {
        val pageNumber = uiState.pages.getOrNull(uiState.currentPageIndex)?.pageNumber
            ?: uiState.verses.firstOrNull()?.pageNumber
            ?: return@LaunchedEffect
        onPageLoaded(pageNumber)
    }
    QuranReaderScreen(
        uiState = uiState,
        onChapterInfoClick = onChapterInfoClick,
        onPreviousPage = viewModel::onPreviousPage,
        onNextPage = viewModel::onNextPage,
    )
}
