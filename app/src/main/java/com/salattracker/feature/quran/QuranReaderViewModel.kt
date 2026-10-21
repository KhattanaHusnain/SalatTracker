package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.DEFAULT_TAFSIR_PACK_ID
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.MushafWord
import com.salattracker.domain.model.QuranBookmark
import com.salattracker.domain.model.TafsirEntry
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

/** A slice of one verse's Urdu translation shown under a single Arabic Mushaf line.
 *
 * There's no translation data that's actually aligned to where a verse's words fall across print
 * lines — Jalandhry (like every translation we have) is composed as whole, grammatical sentences
 * per verse, not pre-split to match any particular Mushaf edition's line breaks. So a verse that
 * spans several lines has its translation cut by *word-count proportion* instead: if two-thirds of
 * a verse's Arabic words are on this line, roughly the first two-thirds of its Urdu words are
 * shown here. This is a deliberate, visible approximation, not a claim of precise alignment — the
 * cut point can land mid-phrase. [isVerseComplete] marks the slice that reaches the end of the
 * verse, which is the one that gets the trailing ayah-number marker. */
data class ReaderTranslationChunk(
    val verseNumber: Int,
    val text: String,
    val isVerseComplete: Boolean,
)

/** One real printed Mushaf line of this chapter's Arabic, ready for the reader to flow verses
 * continuously exactly as printed — including a verse that runs past the end of one line onto
 * the next — rather than forcing one verse per visual block. [translationChunks] always has one
 * entry per verse touched on this line — see [ReaderTranslationChunk]. */
data class ReaderLine(
    val pageNumber: Int,
    val words: List<MushafWord>,
    val translationChunks: List<ReaderTranslationChunk>,
)

data class QuranReaderUiState(
    val isLoading: Boolean = true,
    val chapter: Chapter? = null,
    val verses: List<Verse> = emptyList(),
    /** This chapter's Arabic, pre-split into real Mushaf lines — see [ReaderLine]. Empty until
     * the bundled Mushaf data loads. */
    val lines: List<ReaderLine> = emptyList(),
    /** Urdu translation (Fateh Muhammad Jalandhry), keyed by verse number — used only by the
     * robustness fallback below (plain reflowing text, if [lines] is ever empty). The reader's
     * normal line-by-line display uses each line's own [ReaderLine.translationChunks] instead. */
    val translations: Map<Int, String> = emptyMap(),
    /** Single-verse bookmarks only (fromVerse == toVerse == that verse number) in this chapter —
     * the per-verse bookmark toggle's own concern. Range bookmarks aren't editable from here. */
    val bookmarkedVerses: Set<Int> = emptySet(),
    /** At-Tafsir Al-Muyassar entries for this chapter. A verse's tafsir may span a range
     * ([TafsirEntry.fromVerse]..[TafsirEntry.toVerse]), so this isn't keyed by a single verse
     * number the way [translations] is — the screen looks up the entry whose range contains
     * the verse being shown. */
    val tafsirs: List<TafsirEntry> = emptyList(),
    /** Verse numbers whose tafsir is currently expanded in the reader. */
    val expandedTafsirVerses: Set<Int> = emptySet(),
)

@HiltViewModel
class QuranReaderViewModel @Inject constructor(
    private val contentRepository: QuranContentRepository,
    private val mushafRepository: MushafRepository,
    private val userDataRepository: QuranUserDataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chapterNumber: Int = checkNotNull(savedStateHandle["chapterNumber"])

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
            val tafsirs = contentRepository.getTafsirEntries(DEFAULT_TAFSIR_PACK_ID, chapterNumber)
            _uiState.update {
                it.copy(
                    isLoading = false, chapter = chapter, verses = verses,
                    lines = lines, translations = translations, tafsirs = tafsirs,
                )
            }
            // A coarse "started reading this chapter" signal for now — per-verse scroll-position
            // tracking (so "continue reading" resumes mid-chapter) is a later refinement, once the
            // reader has real scroll-visibility tracking to drive it honestly.
            userDataRepository.recordRead(chapterNumber, verseNumber = 1)
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

    fun onTafsirToggle(verseNumber: Int) {
        _uiState.update {
            val expanded = it.expandedTafsirVerses
            it.copy(expandedTafsirVerses = if (verseNumber in expanded) expanded - verseNumber else expanded + verseNumber)
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

private val WHITESPACE = Regex("\\s+")

private fun List<MushafLine>.toReaderLines(translations: Map<Int, String>): List<ReaderLine> {
    val totalWordsByVerse = HashMap<Int, Int>()
    forEach { line -> line.words.forEach { totalWordsByVerse[it.verseNumber] = (totalWordsByVerse[it.verseNumber] ?: 0) + 1 } }

    val urduWordsByVerse = translations.mapValues { (_, text) -> text.trim().split(WHITESPACE).filter { it.isNotEmpty() } }

    // Running totals carried across lines, in reading order, so each verse's successive chunks
    // pick up exactly where the last one left off rather than overlapping or skipping words.
    val arabicWordsConsumed = HashMap<Int, Int>()
    val urduWordsEmitted = HashMap<Int, Int>()

    return map { line ->
        val versesOnLine = LinkedHashSet<Int>().apply { line.words.forEach { add(it.verseNumber) } }
        val arabicWordsOnLineByVerse = line.words.groupingBy { it.verseNumber }.eachCount()

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
                ((urduWords.size.toDouble() * consumed / totalArabicWords).toInt()).coerceIn(alreadyEmitted, urduWords.size)
            }
            if (targetEmitted <= alreadyEmitted) return@mapNotNull null
            urduWordsEmitted[verseNumber] = targetEmitted

            val chunkText = urduWords.subList(alreadyEmitted, targetEmitted).joinToString(" ")
            val text = if (isVerseComplete) verseTranslationWithNumber(chunkText, verseNumber) else chunkText
            ReaderTranslationChunk(verseNumber, text, isVerseComplete)
        }

        ReaderLine(
            pageNumber = line.words.first().pageNumber,
            words = line.words,
            translationChunks = chunks,
        )
    }
}
