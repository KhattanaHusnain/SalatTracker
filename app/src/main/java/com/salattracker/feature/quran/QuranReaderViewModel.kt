package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.MushafWord
import com.salattracker.domain.model.QuranBookmark
import com.salattracker.domain.model.Verse
import com.salattracker.domain.repository.MushafRepository
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** An approximate Urdu translation slice displayed below one Arabic Mushaf line.
 * Translation packs are written per ayah rather than per printed line, so a verse spanning lines
 * is distributed proportionally by word count. Adjacent slices are rendered as one Urdu row. */
data class ReaderTranslationChunk(
    val verseNumber: Int,
    val text: String,
    val isVerseComplete: Boolean,
)

/** One real printed Mushaf line of this chapter's Arabic and its approximate Urdu row. */
data class ReaderLine(
    val pageNumber: Int,
    val words: List<MushafWord>,
    val translationChunks: List<ReaderTranslationChunk>,
)

/** The portion of a surah that belongs to one printed Mushaf page. */
data class ReaderPage(
    val pageNumber: Int,
    val lines: List<ReaderLine>,
)

data class QuranReaderUiState(
    val isLoading: Boolean = true,
    val chapter: Chapter? = null,
    val verses: List<Verse> = emptyList(),
    /** This chapter's Arabic, pre-split into real Mushaf lines — see [ReaderLine]. Empty until
     * the bundled Mushaf data loads. */
    val lines: List<ReaderLine> = emptyList(),
    /** Reader lines grouped by their real printed page number. */
    val pages: List<ReaderPage> = emptyList(),
    val currentPageIndex: Int = 0,
    /** Urdu translation (Fateh Muhammad Jalandhry), keyed by verse number — used only by the
     * robustness fallback below (plain reflowing text, if [lines] is ever empty). The reader's
     * normal line-by-line display uses each line's own [ReaderLine.translationChunks] instead. */
    val translations: Map<Int, String> = emptyMap(),
    /** Single-verse bookmarks only (fromVerse == toVerse == that verse number) in this chapter —
     * the per-verse bookmark toggle's own concern. Range bookmarks aren't editable from here. */
    val bookmarkedVerses: Set<Int> = emptySet(),
)

@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    private val contentRepository: QuranContentRepository,
    private val mushafRepository: MushafRepository,
    private val userDataRepository: QuranUserDataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chapterNumber: Int = checkNotNull(savedStateHandle["chapterNumber"])
    private val resumeVerseNumber: Int = savedStateHandle["verseNumber"] ?: 1

    private val _uiState = MutableStateFlow(QuranReaderUiState())
    val uiState: StateFlow<QuranReaderUiState> = _uiState.asStateFlow()

    /** This chapter's single-verse bookmarks, keyed by verse number, kept around so toggling off
     * a bookmark has its id to delete without a separate lookup query. */
    private var bookmarksByVerse: Map<Int, QuranBookmark> = emptyMap()

    init {
        viewModelScope.launch {
            val chapter = contentRepository.getChapter(chapterNumber)
            val verses = contentRepository.getVerses(chapterNumber)
            val mushafLines = mushafRepository.getLinesForChapter(chapterNumber)
            val translations = contentRepository
                .getTranslationText(DEFAULT_URDU_TRANSLATION_PACK_ID, chapterNumber)
                .associate { it.verseNumber to it.text }
            val lines = mushafLines.toReaderLines(translations)
            val readerPages = lines.groupBy { it.pageNumber }
                .map { (pageNumber, pageLines) -> ReaderPage(pageNumber, pageLines) }
            val initialPageIndex = readerPages.indexOfFirst { page ->
                page.lines.any { line -> line.words.any { it.verseNumber == resumeVerseNumber } }
            }.takeIf { it >= 0 } ?: 0
            _uiState.update {
                it.copy(
                    isLoading = false, chapter = chapter, verses = verses,
                    lines = lines, pages = readerPages, currentPageIndex = initialPageIndex,
                    translations = translations,
                )
            }
            recordReadForPage(readerPages.getOrNull(initialPageIndex))
        }
        viewModelScope.launch {
            userDataRepository.observeBookmarks().collect { bookmarks ->
                bookmarksByVerse = bookmarks
                    .filter { it.chapterNumber == chapterNumber && it.fromVerse == it.toVerse }
                    .associateBy { it.fromVerse }
                _uiState.update { it.copy(bookmarkedVerses = bookmarksByVerse.keys) }
            }
        }
    }

    fun onBookmarkToggle(verseNumber: Int) {
        viewModelScope.launch {
            val existing = bookmarksByVerse[verseNumber]
            if (existing != null) {
                userDataRepository.deleteBookmark(existing.id)
            } else {
                userDataRepository.addBookmark(
                    QuranBookmark(chapterNumber = chapterNumber, fromVerse = verseNumber, toVerse = verseNumber)
                )
            }
        }
    }

    fun onNextPage() {
        _uiState.update { state ->
            if (state.currentPageIndex >= state.pages.lastIndex) state
            else state.copy(currentPageIndex = state.currentPageIndex + 1)
        }
        recordReadForPage(_uiState.value.pages.getOrNull(_uiState.value.currentPageIndex))
    }

    fun onPreviousPage() {
        _uiState.update { state ->
            if (state.currentPageIndex <= 0) state
            else state.copy(currentPageIndex = state.currentPageIndex - 1)
        }
        recordReadForPage(_uiState.value.pages.getOrNull(_uiState.value.currentPageIndex))
    }

    /** Saves the first ayah displayed on the current printed page, which also identifies the
     * exact page to restore when the user selects Continue reading. */
    private fun recordReadForPage(page: ReaderPage?) {
        val verseNumber = page?.lines?.firstOrNull()?.words?.firstOrNull()?.verseNumber ?: return
        viewModelScope.launch { userDataRepository.recordRead(chapterNumber, verseNumber) }
    }
}

// Urdu conventionally uses Extended Arabic-Indic digits (the Persian/Urdu variant — e.g. ۴ ۵ ۶,
// not Arabic's own ٤ ٥ ٦), appended to a translation the same way the Arabic text ends each verse
// with its own ornamental number (see MushafWord.isVerseEndMarker) — same ornate-parenthesis
// style (U+FD3E/FD3F), same "last thing read" position (RTL, so the logical end of the string).
// Not `private`: QuranReaderScreen's robustness fallback (plain-text rendering, if bundled Mushaf
// data is ever missing) formats a whole-verse translation the same way.
val URDU_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
fun Int.toUrduNumeral(): String = toString().map { URDU_DIGITS[it - '0'] }.joinToString("")
fun verseTranslationWithNumber(translation: String, verseNumber: Int): String =
    "$translation ﴿${verseNumber.toUrduNumeral()}﴾"

/** Shared by the surah and juz readers so Urdu follows the same Arabic line chunks in both. */
fun List<MushafLine>.toReaderLines(translations: Map<Int, String>): List<ReaderLine> {
    val totalWordsByVerse = HashMap<Int, Int>()
    forEach { line ->
        line.words.filterNot { it.isVerseEndMarker }.forEach { word ->
            totalWordsByVerse[word.verseNumber] = (totalWordsByVerse[word.verseNumber] ?: 0) + 1
        }
    }

    val urduWordsByVerse = translations.mapValues { (_, text) -> text.trim().split(WHITESPACE).filter { it.isNotEmpty() } }

    // Running totals distribute a verse's translation across the same printed Arabic lines.
    val arabicWordsConsumed = HashMap<Int, Int>()
    val urduWordsEmitted = HashMap<Int, Int>()

    return map { line ->
        val versesOnLine = LinkedHashSet<Int>().apply {
            line.words.filterNot { it.isVerseEndMarker }.forEach { add(it.verseNumber) }
        }
        val arabicWordsOnLineByVerse = line.words
            .filterNot { it.isVerseEndMarker }
            .groupingBy { it.verseNumber }
            .eachCount()

        val chunks = versesOnLine.mapNotNull { verseNumber ->
            val totalArabicWords = totalWordsByVerse[verseNumber] ?: return@mapNotNull null
            val urduWords = urduWordsByVerse[verseNumber]
            if (urduWords.isNullOrEmpty()) return@mapNotNull null

            val consumed = (arabicWordsConsumed[verseNumber] ?: 0) + (arabicWordsOnLineByVerse[verseNumber] ?: 0)
            arabicWordsConsumed[verseNumber] = consumed
            val isVerseComplete = consumed >= totalArabicWords
            val alreadyEmitted = urduWordsEmitted[verseNumber] ?: 0
            val targetEmitted = if (isVerseComplete) {
                urduWords.size
            } else {
                kotlin.math.ceil(urduWords.size.toDouble() * consumed / totalArabicWords)
                    .toInt()
                    .coerceIn(alreadyEmitted, urduWords.size)
            }
            if (targetEmitted <= alreadyEmitted) return@mapNotNull null
            urduWordsEmitted[verseNumber] = targetEmitted

            val text = urduWords.subList(alreadyEmitted, targetEmitted).joinToString(" ")
            ReaderTranslationChunk(
                verseNumber = verseNumber,
                text = if (isVerseComplete) verseTranslationWithNumber(text, verseNumber) else text,
                isVerseComplete = isVerseComplete,
            )
        }

        ReaderLine(
            pageNumber = line.words.first().pageNumber,
            words = line.words,
            translationChunks = chunks,
        )
    }
}

private val WHITESPACE = Regex("\\s+")
