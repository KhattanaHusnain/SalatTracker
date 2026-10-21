package com.salattracker.data.repository

import com.salattracker.data.local.quran.MushafWordDao
import com.salattracker.data.local.quran.MushafWordEntity
import com.salattracker.domain.model.MushafEdition
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.model.MushafWord
import com.salattracker.domain.model.VerseLocation
import com.salattracker.domain.repository.MushafRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MushafRepositoryImpl @Inject constructor(
    private val mushafWordDao: MushafWordDao,
) : MushafRepository {

    override suspend fun getPage(pageNumber: Int, edition: MushafEdition): MushafPage {
        val entities = mushafWordDao.getWords(pageNumber, edition.apiId)
        check(entities.isNotEmpty()) { "No bundled Mushaf data for page $pageNumber (edition ${edition.apiId})" }

        // entities are already in order_index order (set at seed time to the original API's
        // "verse order, then word order within that verse" sequence) — see QuranSeedWorker.
        val words = entities.map { it.toDomain() }
        val lines = words.groupBy { it.lineNumber }
            .toSortedMap()
            .map { (lineNumber, lineWords) -> MushafLine(lineNumber, lineWords) }

        val firstVerse = words.first().let { VerseLocation(it.chapterNumber, it.verseNumber) }
        val lastVerse = words.last().let { VerseLocation(it.chapterNumber, it.verseNumber) }
        return MushafPage(pageNumber, lines, firstVerse, lastVerse)
    }

    override suspend fun getLinesForChapter(chapterNumber: Int, edition: MushafEdition): List<MushafLine> {
        val words = mushafWordDao.getWordsForChapter(chapterNumber, edition.apiId).map { it.toDomain() }
        // Grouping by (page, line) — not line alone, since every page reuses line numbers 1..~15 —
        // and words are already in order_index order, so the resulting groups come out in correct
        // reading order with no extra sort needed (LinkedHashMap preserves first-seen key order).
        return words.groupBy { it.pageNumber to it.lineNumber }
            .values
            .map { lineWords -> MushafLine(lineNumber = lineWords.first().lineNumber, words = lineWords) }
    }
}

private fun MushafWordEntity.toDomain() = MushafWord(
    chapterNumber = chapterNumber,
    verseNumber = verseNumber,
    position = position,
    pageNumber = pageNumber,
    lineNumber = lineNumber,
    glyphCode = glyphCode,
    isVerseEndMarker = isVerseEndMarker,
)
