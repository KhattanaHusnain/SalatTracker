package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.repository.QuranContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranChapterInfoUiState(
    val isLoading: Boolean = true,
    val chapter: Chapter? = null,
    val firstJuz: Int = 0,
    val lastJuz: Int = 0,
    val firstPage: Int = 0,
    val lastPage: Int = 0,
)

/**
 * Structural chapter facts only (names, revelation place/order, verse count, juz/page range) —
 * all derived from already-cleared, already-seeded data (see QURAN_CONTENT_SOURCES.md). A
 * narrative "what this surah is about" description is deliberately left out: the one source
 * found for that (Quran.com's `/chapters/{id}/info` endpoint) carries the same redistribution
 * restriction that the Arabic text had to be re-sourced away from once already — not repeating
 * that mistake for a single description screen. Add it once a cleared source for that specific
 * content exists.
 */
@HiltViewModel
class QuranChapterInfoViewModel @Inject constructor(
    private val contentRepository: QuranContentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chapterNumber: Int = checkNotNull(savedStateHandle["chapterNumber"])

    private val _uiState = MutableStateFlow(QuranChapterInfoUiState())
    val uiState: StateFlow<QuranChapterInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val chapter = contentRepository.getChapter(chapterNumber)
            val verses = contentRepository.getVerses(chapterNumber)
            val juzNumbers = verses.map { it.juzNumber }
            val pageNumbers = verses.map { it.pageNumber }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    chapter = chapter,
                    firstJuz = juzNumbers.minOrNull() ?: 0,
                    lastJuz = juzNumbers.maxOrNull() ?: 0,
                    firstPage = pageNumbers.minOrNull() ?: 0,
                    lastPage = pageNumbers.maxOrNull() ?: 0,
                )
            }
        }
    }
}
