package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.MUSHAF_TOTAL_PAGES
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.model.VerseLocation
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.MushafRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FIRST_MUSHAF_PAGE = 1

data class QuranMushafUiState(
    val isLoading: Boolean = true,
    val pageNumber: Int = FIRST_MUSHAF_PAGE,
    val firstPageNumber: Int = FIRST_MUSHAF_PAGE,
    val lastPageNumber: Int = MUSHAF_TOTAL_PAGES,
    val page: MushafPage? = null,
    /** Opening ayah of every juz, used to set off its first printed line in the Mushaf. */
    val juzStartVerses: Set<VerseLocation> = emptySet(),
    /** Bounds the selected juz when the first or last printed page is shared with another juz. */
    val juzStartVerse: VerseLocation? = null,
    val nextJuzStartVerse: VerseLocation? = null,
    /** Urdu slices aligned with each printed line, using the same chunking as the surah reader. */
    val translationChunks: Map<Int, List<ReaderTranslationChunk>> = emptyMap(),
    /** A juz opens in translated reading mode; the toolbar Mushaf action switches to Arabic-only. */
    val showTranslations: Boolean = false,
    val errorRes: Int? = null,
)

@HiltViewModel
class QuranMushafViewModel @Inject constructor(
    private val mushafRepository: MushafRepository,
    private val contentRepository: QuranContentRepository,
    private val userDataRepository: QuranUserDataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedJuzNumber: Int? = savedStateHandle.get<Int>("juzNumber")
        ?.takeIf { it in 1..TOTAL_JUZ }

    private val _uiState = MutableStateFlow(
        QuranMushafUiState(
            pageNumber = checkNotNull(savedStateHandle["pageNumber"]),
            showTranslations = selectedJuzNumber != null,
        )
    )
    val uiState: StateFlow<QuranMushafUiState> = _uiState.asStateFlow()

    val hasPreviousPage: Boolean get() = _uiState.value.pageNumber > _uiState.value.firstPageNumber
    val hasNextPage: Boolean get() = _uiState.value.pageNumber < _uiState.value.lastPageNumber

    init {
        val requestedPage = _uiState.value.pageNumber
        loadJuzStartVerses()
        if (selectedJuzNumber == null) {
            loadPage(requestedPage)
        } else {
            loadJuz(selectedJuzNumber, requestedPage)
        }
    }

    fun onNextPage() {
        val next = _uiState.value.pageNumber + 1
        if (next <= _uiState.value.lastPageNumber) loadPage(next)
    }

    fun onPreviousPage() {
        val previous = _uiState.value.pageNumber - 1
        if (previous >= _uiState.value.firstPageNumber) loadPage(previous)
    }

    fun showMushafOnly() {
        _uiState.update { it.copy(showTranslations = false) }
    }

    private fun loadJuzStartVerses() {
        viewModelScope.launch {
            val starts = (1..TOTAL_JUZ).mapNotNull { juzNumber ->
                contentRepository.firstVerseOfJuz(juzNumber)
            }.toSet()
            _uiState.update { it.copy(juzStartVerses = starts) }
        }
    }

    private fun loadJuz(juzNumber: Int, requestedPage: Int) {
        viewModelScope.launch {
            val firstLocation = contentRepository.firstVerseOfJuz(juzNumber)
                ?: return@launch loadPage(requestedPage)
            val firstPage = contentRepository.getVerse(firstLocation.chapterNumber, firstLocation.verseNumber)
                ?.pageNumber
                ?: return@launch loadPage(requestedPage)
            val nextFirstLocation = contentRepository.firstVerseOfJuz(juzNumber + 1)
            val nextFirstPage = nextFirstLocation?.let { location ->
                contentRepository.getVerse(location.chapterNumber, location.verseNumber)?.pageNumber
            }
            val lastPage = nextFirstPage?.let { nextPageNumber ->
                val nextPage = mushafRepository.getPage(nextPageNumber)
                if (nextPage.lines.indexOfFirst { line -> line.startsAt(nextFirstLocation) } == 0) {
                    nextPageNumber - 1
                } else {
                    nextPageNumber
                }
            } ?: MUSHAF_TOTAL_PAGES
            val page = requestedPage.coerceIn(firstPage, lastPage)
            _uiState.update {
                it.copy(
                    pageNumber = page,
                    firstPageNumber = firstPage,
                    lastPageNumber = lastPage,
                    juzStartVerse = firstLocation,
                    nextJuzStartVerse = nextFirstLocation,
                )
            }
            loadPage(page)
        }
    }

    private fun loadPage(pageNumber: Int) {
        _uiState.update {
            it.copy(isLoading = it.page == null, pageNumber = pageNumber, translationChunks = emptyMap(), errorRes = null)
        }
        viewModelScope.launch {
            try {
                val page = mushafRepository.getPage(pageNumber)
                val translationChunks = if (_uiState.value.showTranslations) {
                    val chapterNumbers = page.lines
                        .flatMap { it.words }
                        .map { it.chapterNumber }
                        .distinct()
                    val readerLines = buildList {
                        for (chapterNumber in chapterNumbers) {
                            val translations = contentRepository
                                .getTranslationText(DEFAULT_URDU_TRANSLATION_PACK_ID, chapterNumber)
                                .associate { it.verseNumber to it.text }
                            addAll(mushafRepository.getLinesForChapter(chapterNumber).toReaderLines(translations))
                        }
                    }
                    readerLines
                        .filter { it.pageNumber == page.pageNumber }
                        .groupBy { it.words.first().lineNumber }
                        .mapValues { (_, lines) -> lines.flatMap { it.translationChunks } }
                } else {
                    emptyMap()
                }
                _uiState.update { it.copy(isLoading = false, page = page, translationChunks = translationChunks) }
                userDataRepository.recordRead(
                    chapterNumber = page.firstDisplayedVerse(_uiState.value.juzStartVerse).chapterNumber,
                    verseNumber = page.firstDisplayedVerse(_uiState.value.juzStartVerse).verseNumber,
                    juzNumber = selectedJuzNumber,
                    pageNumber = page.pageNumber,
                )
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) }
            }
        }
    }

    private companion object {
        const val TOTAL_JUZ = 30
    }
}

private fun MushafPage.firstDisplayedVerse(juzStartVerse: VerseLocation?): VerseLocation =
    if (juzStartVerse == null) firstVerse
    else lines.firstOrNull { line -> line.startsAt(juzStartVerse) }
        ?.words?.firstOrNull { word ->
            word.chapterNumber == juzStartVerse.chapterNumber && word.verseNumber == juzStartVerse.verseNumber
        }
        ?.let { word -> VerseLocation(word.chapterNumber, word.verseNumber) }
        ?: firstVerse

private fun com.salattracker.domain.model.MushafLine.startsAt(location: VerseLocation): Boolean =
    words.any { word ->
        word.position == 1 && word.chapterNumber == location.chapterNumber && word.verseNumber == location.verseNumber
    }
