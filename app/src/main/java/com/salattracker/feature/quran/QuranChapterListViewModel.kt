package com.salattracker.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.MUSHAF_TOTAL_PAGES
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuranBrowseTab { SURAH, JUZ }

/** What a page/hizb jump number is being resolved against. */
enum class QuranJumpType { PAGE, HIZB }

/** A juz's first printed page and its compact structural summary. */
data class QuranJuzUi(
    val juzNumber: Int,
    val pageNumber: Int,
    val pageCount: Int,
    val ayahCount: Int,
    val title: JuzTitle,
)

data class QuranChapterListUiState(
    val isLoading: Boolean = true,
    val chapters: List<Chapter> = emptyList(),
    val continueReading: QuranReadHistoryEntry? = null,
    val selectedTab: QuranBrowseTab = QuranBrowseTab.SURAH,
    val juzEntries: List<QuranJuzUi> = emptyList(),
    val showJumpDialog: Boolean = false,
    val jumpType: QuranJumpType = QuranJumpType.PAGE,
    val jumpError: Boolean = false,
    /** Set once a jump resolves to a chapter; the screen navigates on it and calls
     * [QuranChapterListViewModel.onNavigationConsumed] to reset it. */
    val navigateToChapter: Int? = null,
)

@HiltViewModel
class QuranChapterListViewModel @Inject constructor(
    private val contentRepository: QuranContentRepository,
    private val userDataRepository: QuranUserDataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranChapterListUiState())
    val uiState: StateFlow<QuranChapterListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentRepository.observeChapters().collect { chapters ->
                _uiState.update { it.copy(isLoading = false, chapters = chapters) }
            }
        }
        viewModelScope.launch {
            userDataRepository.observeLastRead().collect { last ->
                _uiState.update { it.copy(continueReading = last) }
            }
        }
        viewModelScope.launch {
            val entries = (1..QURAN_JUZ_COUNT).mapNotNull { juzNumber ->
                val location = contentRepository.firstVerseOfJuz(juzNumber) ?: return@mapNotNull null
                val firstVerse = contentRepository.getVerse(location.chapterNumber, location.verseNumber)
                    ?: return@mapNotNull null
                val nextJuzFirstPage = contentRepository.firstVerseOfJuz(juzNumber + 1)
                    ?.let { contentRepository.getVerse(it.chapterNumber, it.verseNumber)?.pageNumber }
                    ?: MUSHAF_TOTAL_PAGES + 1
                QuranJuzUi(
                    juzNumber = juzNumber,
                    pageNumber = firstVerse.pageNumber,
                    pageCount = nextJuzFirstPage - firstVerse.pageNumber,
                    ayahCount = contentRepository.verseCountForJuz(juzNumber),
                    title = checkNotNull(JUZ_TITLES[juzNumber]),
                )
            }
            _uiState.update { it.copy(juzEntries = entries) }
        }
    }

    fun onTabSelected(tab: QuranBrowseTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onJumpDialogOpen() {
        _uiState.update { it.copy(showJumpDialog = true, jumpError = false) }
    }

    fun onJumpDialogDismiss() {
        _uiState.update { it.copy(showJumpDialog = false, jumpError = false) }
    }

    fun onJumpTypeSelected(type: QuranJumpType) {
        _uiState.update { it.copy(jumpType = type, jumpError = false) }
    }

    fun onJumpConfirm(input: String) {
        val number = input.toIntOrNull()
        if (number == null) {
            _uiState.update { it.copy(jumpError = true) }
            return
        }
        viewModelScope.launch {
            val location = when (_uiState.value.jumpType) {
                QuranJumpType.PAGE -> contentRepository.firstVerseOfPage(number)
                QuranJumpType.HIZB -> contentRepository.firstVerseOfHizb(number)
            }
            if (location != null) {
                _uiState.update { it.copy(showJumpDialog = false, jumpError = false, navigateToChapter = location.chapterNumber) }
            } else {
                _uiState.update { it.copy(jumpError = true) }
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigateToChapter = null) }
    }

    private companion object {
        const val QURAN_JUZ_COUNT = 30
    }
}
