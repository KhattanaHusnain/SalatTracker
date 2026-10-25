package com.salattracker.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/** Page controls rendered by MainActivity's top app bar while a Quran page is open. */
data class QuranPageControls(
    val pageNumber: Int,
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
    val showMushafButton: Boolean = false,
    val onMushafButtonClick: () -> Unit = {},
)

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
    onPageControlsChanged: (QuranPageControls) -> Unit,
    viewModel: QuranReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.currentPageIndex, uiState.pages) {
        val pageNumber = uiState.pages.getOrNull(uiState.currentPageIndex)?.pageNumber
            ?: uiState.verses.firstOrNull()?.pageNumber
            ?: return@LaunchedEffect
        onPageLoaded(pageNumber)
        onPageControlsChanged(
            QuranPageControls(
                pageNumber = pageNumber,
                hasPreviousPage = uiState.currentPageIndex > 0,
                hasNextPage = uiState.currentPageIndex < uiState.pages.lastIndex,
                onPreviousPage = viewModel::onPreviousPage,
                onNextPage = viewModel::onNextPage,
            )
        )
    }
    QuranReaderScreen(
        uiState = uiState,
        onChapterInfoClick = onChapterInfoClick,
        onBookmarkToggle = viewModel::onBookmarkToggle,
    )
}
