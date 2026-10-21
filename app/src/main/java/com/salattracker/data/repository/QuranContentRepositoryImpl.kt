package com.salattracker.data.repository

import android.content.Context
import com.salattracker.data.local.quran.ChapterDao
import com.salattracker.data.local.quran.ChapterEntity
import com.salattracker.data.local.quran.QuranPackDatabases
import com.salattracker.data.local.quran.QuranTafsirPackDatabase
import com.salattracker.data.local.quran.QuranTranslationPackDatabase
import com.salattracker.data.local.quran.QuranVerseDao
import com.salattracker.data.local.quran.QuranWordByWordPackDatabase
import com.salattracker.data.local.quran.TafsirTextEntity
import com.salattracker.data.local.quran.TranslationTextEntity
import com.salattracker.data.local.quran.VerseEntity
import com.salattracker.data.local.quran.WordByWordTranslationEntity
import com.salattracker.data.local.quran.WordSegmentDao
import com.salattracker.data.local.quran.WordSegmentEntity
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.QuranContentPackType
import com.salattracker.domain.model.TafsirEntry
import com.salattracker.domain.model.TranslatedVerseText
import com.salattracker.domain.model.Verse
import com.salattracker.domain.model.VerseLocation
import com.salattracker.domain.model.WordSegment
import com.salattracker.domain.model.WordTranslation
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranContentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chapterDao: ChapterDao,
    private val verseDao: QuranVerseDao,
    private val wordSegmentDao: WordSegmentDao,
    private val userDataRepository: QuranUserDataRepository,
) : QuranContentRepository {

    // Content packs are opened lazily and kept open for the process lifetime — small, read-only
    // files, so the cost of holding them open is negligible next to re-opening on every query.
    private val translationDbs = ConcurrentHashMap<String, QuranTranslationPackDatabase>()
    private val tafsirDbs = ConcurrentHashMap<String, QuranTafsirPackDatabase>()
    private val wordByWordDbs = ConcurrentHashMap<String, QuranWordByWordPackDatabase>()

    override fun observeChapters(): Flow<List<Chapter>> =
        chapterDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getChapter(number: Int): Chapter? = chapterDao.get(number)?.toDomain()

    override suspend fun getVerses(chapterNumber: Int): List<Verse> =
        verseDao.getForChapter(chapterNumber).map { it.toDomain() }

    override suspend fun getVerse(chapterNumber: Int, verseNumber: Int): Verse? =
        verseDao.get(chapterNumber, verseNumber)?.toDomain()

    override suspend fun getWordSegments(chapterNumber: Int, verseNumber: Int): List<WordSegment> =
        wordSegmentDao.getForVerse(chapterNumber, verseNumber).map { it.toDomain() }

    override suspend fun getWordSegmentsForChapter(chapterNumber: Int): List<WordSegment> =
        wordSegmentDao.getForChapter(chapterNumber).map { it.toDomain() }

    override suspend fun firstVerseOfJuz(juzNumber: Int): VerseLocation? = verseDao.firstOfJuz(juzNumber)?.toLocation()

    override suspend fun firstVerseOfHizb(hizbNumber: Int): VerseLocation? = verseDao.firstOfHizb(hizbNumber)?.toLocation()

    override suspend fun firstVerseOfPage(pageNumber: Int): VerseLocation? = verseDao.firstOfPage(pageNumber)?.toLocation()

    override suspend fun getTranslationText(packId: String, chapterNumber: Int): List<TranslatedVerseText> {
        val db = openTranslationDb(packId) ?: return emptyList()
        return db.translationTextDao().getForChapter(chapterNumber).map { it.toDomain(packId) }
    }

    override suspend fun getTafsirEntries(packId: String, chapterNumber: Int): List<TafsirEntry> {
        val db = openTafsirDb(packId) ?: return emptyList()
        return db.tafsirTextDao().getForChapter(chapterNumber).map { it.toDomain(packId) }
    }

    override suspend fun getWordByWordTranslations(packId: String, chapterNumber: Int, verseNumber: Int): List<WordTranslation> {
        val db = openWordByWordDb(packId) ?: return emptyList()
        return db.wordByWordTranslationDao().getForVerse(chapterNumber, verseNumber).map { it.toDomain(packId) }
    }

    override suspend fun getWordByWordTranslationsForChapter(packId: String, chapterNumber: Int): List<WordTranslation> {
        val db = openWordByWordDb(packId) ?: return emptyList()
        return db.wordByWordTranslationDao().getForChapter(chapterNumber).map { it.toDomain(packId) }
    }

    private suspend fun openTranslationDb(packId: String): QuranTranslationPackDatabase? {
        translationDbs[packId]?.let { return it }
        val localPath = downloadedPackPath(packId, QuranContentPackType.TRANSLATION) ?: return null
        return QuranPackDatabases.openTranslation(context, localPath).also { translationDbs[packId] = it }
    }

    private suspend fun openTafsirDb(packId: String): QuranTafsirPackDatabase? {
        tafsirDbs[packId]?.let { return it }
        val localPath = downloadedPackPath(packId, QuranContentPackType.TAFSIR) ?: return null
        return QuranPackDatabases.openTafsir(context, localPath).also { tafsirDbs[packId] = it }
    }

    private suspend fun openWordByWordDb(packId: String): QuranWordByWordPackDatabase? {
        wordByWordDbs[packId]?.let { return it }
        val localPath = downloadedPackPath(packId, QuranContentPackType.WORD_BY_WORD) ?: return null
        return QuranPackDatabases.openWordByWord(context, localPath).also { wordByWordDbs[packId] = it }
    }

    private suspend fun downloadedPackPath(packId: String, expectedType: QuranContentPackType): String? {
        val pack = userDataRepository.getContentPack(packId) ?: return null
        if (!pack.isDownloaded || pack.type != expectedType) return null
        return pack.localPath
    }
}

private fun ChapterEntity.toDomain() =
    Chapter(number, nameArabic, nameTransliteration, nameTranslation, versesCount, revelationPlace, revelationOrder)

private fun VerseEntity.toDomain() =
    Verse(chapterNumber, verseNumber, textUthmani, juzNumber, hizbNumber, rubNumber, pageNumber, hasSajda)
private fun VerseEntity.toLocation() = VerseLocation(chapterNumber, verseNumber)

private fun WordSegmentEntity.toDomain() = WordSegment(chapterNumber, verseNumber, position, textArabic)

private fun TranslationTextEntity.toDomain(packId: String) = TranslatedVerseText(packId, chapterNumber, verseNumber, text)
private fun TafsirTextEntity.toDomain(packId: String) = TafsirEntry(packId, chapterNumber, fromVerse, toVerse, text)
private fun WordByWordTranslationEntity.toDomain(packId: String) = WordTranslation(packId, chapterNumber, verseNumber, position, text)
