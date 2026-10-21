package com.salattracker.domain.model

// ── Mushaf (paginated, print-accurate reading mode) ──────────────────────────────────────
// Rendering a page exactly as printed needs two things that are separate from the regular
// reader's data: a glyph code per word (a private-use-area character sequence that only renders
// correctly in that word's page-specific QCF font) and a line number grouping words the way the
// print layout does, which often mixes words from more than one verse on one line. Both are
// bundled in the APK (assets/quran/mushaf_words.json, assets/quran/mushaf_fonts/v2/) — see
// MushafRepositoryImpl and QuranSeedWorker — rather than fetched at runtime.

/** Every printed Hafs Mushaf edition has exactly 604 pages — used for the reader's page-number
 * clamping. */
const val MUSHAF_TOTAL_PAGES = 604

/** Which printed Mushaf edition a page/word belongs to. Only Hafs/QCF V2 is wired up today;
 * the others are listed since the API's `mushaf` id parameter is edition-specific. */
enum class MushafEdition(val apiId: Int) {
    QCF_V2(1),
    QCF_V1(2),
    QCF_V4_TAJWEED(19),
}

/** One word on a printed Mushaf page. [glyphCode] is the character sequence to render using
 * that page's own font ([MushafEdition]+[pageNumber] selects which font file); it is not
 * human-readable text and must never be shown with a regular Arabic font. */
data class MushafWord(
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val pageNumber: Int,
    val lineNumber: Int,
    val glyphCode: String,
    val isVerseEndMarker: Boolean = false,
)

/** All words on one printed line of a Mushaf page, in reading (right-to-left) order. A line can
 * — and often does — contain words from more than one verse. */
data class MushafLine(
    val lineNumber: Int,
    val words: List<MushafWord>,
)

/** One printed Mushaf page: its lines in top-to-bottom order, plus the verse range it covers
 * (for jump-to-page / continue-reading bookkeeping shared with the regular reader). */
data class MushafPage(
    val pageNumber: Int,
    val lines: List<MushafLine>,
    val firstVerse: VerseLocation,
    val lastVerse: VerseLocation,
)
