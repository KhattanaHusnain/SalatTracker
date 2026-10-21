package com.salattracker.domain.model

// ── Chapter (surah) ───────────────────────────────────────────────────────────

enum class RevelationPlace { MECCAN, MEDINAN }

data class Chapter(
    val number: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameTranslation: String,
    val versesCount: Int,
    val revelationPlace: RevelationPlace,
    val revelationOrder: Int,
)

// ── Verse ─────────────────────────────────────────────────────────────────────
// Base Arabic text plus the static facts (juz/hizb/rub/page, sajda) every verse carries
// regardless of which translation/tafsir/script the user has picked — those live in
// downloadable content packs instead (see [TranslatedVerseText]/[TafsirEntry]/[WordTranslation]).

data class Verse(
    val chapterNumber: Int,
    val verseNumber: Int,
    val textUthmani: String,
    val juzNumber: Int,
    val hizbNumber: Int,
    val rubNumber: Int,
    val pageNumber: Int,
    val hasSajda: Boolean = false,
)

/** A verse's location, without its content — the result of a juz/hizb/page navigation lookup. */
data class VerseLocation(val chapterNumber: Int, val verseNumber: Int)

/** One Arabic word of a verse, in reading order ([position] is 1-based). The word's meaning in a
 * given language comes from a downloaded word-by-word pack — see [WordTranslation]. */
data class WordSegment(
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val textArabic: String,
)

// ── Downloaded-pack content ───────────────────────────────────────────────────
// Only resolvable once the pack named by [packId] is downloaded — see
// [com.salattracker.domain.repository.QuranUserDataRepository] for the download registry.

data class TranslatedVerseText(
    val packId: String,
    val chapterNumber: Int,
    val verseNumber: Int,
    val text: String,
)

/** A tafsir (commentary) entry. Tafsirs often comment on a group of verses together, so
 * [fromVerse]/[toVerse] may span more than one verse ([fromVerse] == [toVerse] for a single verse). */
data class TafsirEntry(
    val packId: String,
    val chapterNumber: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val text: String,
)

data class WordTranslation(
    val packId: String,
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val text: String,
)
