package com.salattracker.domain.repository

import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.TafsirEntry
import com.salattracker.domain.model.TranslatedVerseText
import com.salattracker.domain.model.Verse
import com.salattracker.domain.model.VerseLocation
import com.salattracker.domain.model.WordSegment
import com.salattracker.domain.model.WordTranslation
import kotlinx.coroutines.flow.Flow

/**
 * The Quran text itself: chapters, base Arabic verse text, and word-by-word segmentation — all
 * bundled in the app — plus whatever a user's downloaded content packs unlock (translations,
 * tafsirs, word-by-word glosses). Which packs are downloaded, and where their files live, is
 * [QuranUserDataRepository]'s concern; this repository just reads from them once they exist.
 */
interface QuranContentRepository {
    fun observeChapters(): Flow<List<Chapter>>
    suspend fun getChapter(number: Int): Chapter?
    suspend fun getVerses(chapterNumber: Int): List<Verse>
    suspend fun getVerse(chapterNumber: Int, verseNumber: Int): Verse?
    suspend fun getWordSegments(chapterNumber: Int, verseNumber: Int): List<WordSegment>
    suspend fun getWordSegmentsForChapter(chapterNumber: Int): List<WordSegment>

    suspend fun firstVerseOfJuz(juzNumber: Int): VerseLocation?
    suspend fun chapterNumbersForJuz(juzNumber: Int): List<Int>
    suspend fun firstVerseOfHizb(hizbNumber: Int): VerseLocation?
    suspend fun firstVerseOfPage(pageNumber: Int): VerseLocation?

    /** Empty if [packId] isn't downloaded, or isn't a translation pack. */
    suspend fun getTranslationText(packId: String, chapterNumber: Int): List<TranslatedVerseText>

    /** Empty if [packId] isn't downloaded, or isn't a tafsir pack. */
    suspend fun getTafsirEntries(packId: String, chapterNumber: Int): List<TafsirEntry>

    /** Empty if [packId] isn't downloaded, or isn't a word-by-word pack. */
    suspend fun getWordByWordTranslations(packId: String, chapterNumber: Int, verseNumber: Int): List<WordTranslation>

    /** Same as [getWordByWordTranslations], for every verse in [chapterNumber] in one call — the
     * reader needs this per-chapter, not per-verse, to avoid a query per verse on long chapters. */
    suspend fun getWordByWordTranslationsForChapter(packId: String, chapterNumber: Int): List<WordTranslation>
}
