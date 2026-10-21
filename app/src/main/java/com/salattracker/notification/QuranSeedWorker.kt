package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.salattracker.data.local.dao.QuranContentPackDao
import com.salattracker.data.local.entity.QuranContentPackEntity
import com.salattracker.data.local.quran.ChapterDao
import com.salattracker.data.local.quran.ChapterEntity
import com.salattracker.data.local.quran.MushafWordDao
import com.salattracker.data.local.quran.MushafWordEntity
import com.salattracker.data.local.quran.QuranPackDatabases
import com.salattracker.data.local.quran.QuranVerseDao
import com.salattracker.data.local.quran.TafsirTextEntity
import com.salattracker.data.local.quran.TranslationTextEntity
import com.salattracker.data.local.quran.VerseEntity
import com.salattracker.data.local.quran.WordByWordTranslationEntity
import com.salattracker.data.local.quran.WordSegmentDao
import com.salattracker.data.local.quran.WordSegmentEntity
import com.salattracker.domain.model.DEFAULT_TAFSIR_PACK_ID
import com.salattracker.domain.model.DEFAULT_TRANSLATION_PACK_ID
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.DEFAULT_WORD_BY_WORD_PACK_ID
import com.salattracker.domain.model.QuranContentPackType
import com.salattracker.domain.model.RevelationPlace
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.InputStreamReader

/**
 * Imports bundled Quran content into QuranCoreDatabase (the chapters, Uthmani verse text, base
 * word segmentation, and Mushaf page/glyph layout in assets/quran) and seeds the default
 * translation, tafsir, and word-by-word packs, on first run -- a no-op on every run after the
 * first. The asset JSON is sourced from alquran.cloud (Arabic text, translation, word-by-word)
 * and quranenc.com (tafsir); the Arabic ultimately comes from the Tanzil Project, which
 * alquran.cloud serves under terms that explicitly permit storing/bundling it, unlike
 * Quran.com/Quran Foundation's own general content API -- see QURAN_CONTENT_SOURCES.md for the
 * full correction and the licensing basis, including the attribution this owes on the app's
 * About/credits screen. The Mushaf glyph data and its per-page QCF V2 fonts (assets/quran/
 * mushaf_fonts/v2/) are the one exception sourced directly from Quran Foundation's own API/CDN --
 * its Mushaf Fonts and Images terms specifically permit bundling those (unlike its general
 * content), given a credited, registered developer app; see QURAN_CONTENT_SOURCES.md.
 */
@HiltWorker
class QuranSeedWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val chapterDao: ChapterDao,
    private val verseDao: QuranVerseDao,
    private val wordSegmentDao: WordSegmentDao,
    private val mushafWordDao: MushafWordDao,
    private val contentPackDao: QuranContentPackDao,
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        return try {
            seedCoreContentIfNeeded()
            seedWordSegmentsIfNeeded()
            seedMushafWordsIfNeeded()
            seedDefaultTranslationIfNeeded()
            seedDefaultUrduTranslationIfNeeded()
            seedDefaultTafsirIfNeeded()
            seedDefaultWordByWordIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun seedCoreContentIfNeeded() {
        if (chapterDao.get(1) != null) return // already seeded

        val chapters = readChapterAssets().map { it.toEntity() }
        chapterDao.insertAll(chapters)

        val verses = readVerseAssets().map { it.toEntity() }
        verseDao.insertAll(verses)
    }

    private suspend fun seedWordSegmentsIfNeeded() {
        if (wordSegmentDao.getForVerse(1, 1).isNotEmpty()) return // already seeded

        val segments = readWordSegmentAssets().map { it.toEntity() }
        wordSegmentDao.insertAll(segments)
    }

    private suspend fun seedMushafWordsIfNeeded() {
        if (mushafWordDao.countForFirstPage(edition = 1) > 0) return // already seeded

        val words = readMushafWordAssets().mapIndexed { index, asset -> asset.toEntity(orderIndex = index) }
        mushafWordDao.insertAll(words)
    }

    private suspend fun seedDefaultTranslationIfNeeded() {
        val packId = DEFAULT_TRANSLATION_PACK_ID
        if (contentPackDao.get(packId)?.isDownloaded == true) return

        val packsDir = File(context.filesDir, "quran_packs").apply { mkdirs() }
        val dbFile = File(packsDir, "en_sahih.db")
        val packDb = QuranPackDatabases.openTranslation(context, dbFile.absolutePath)
        try {
            val rows = readTranslationAssets().map { it.toEntity() }
            packDb.translationTextDao().insertAll(rows)
        } finally {
            packDb.close()
        }

        contentPackDao.upsertAll(
            listOf(
                QuranContentPackEntity(
                    packId = packId,
                    type = QuranContentPackType.TRANSLATION,
                    language = "en",
                    title = "Saheeh International",
                    sizeBytes = dbFile.length(),
                    sourceUrl = "https://api.alquran.cloud/v1/quran/en.sahih",
                    version = 1,
                    isDownloaded = true,
                    localPath = dbFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        )
    }

    private suspend fun seedDefaultUrduTranslationIfNeeded() {
        val packId = DEFAULT_URDU_TRANSLATION_PACK_ID
        if (contentPackDao.get(packId)?.isDownloaded == true) return

        val packsDir = File(context.filesDir, "quran_packs").apply { mkdirs() }
        val dbFile = File(packsDir, "ur_jalandhry.db")
        val packDb = QuranPackDatabases.openTranslation(context, dbFile.absolutePath)
        try {
            val rows = readUrduTranslationAssets().map { it.toEntity() }
            packDb.translationTextDao().insertAll(rows)
        } finally {
            packDb.close()
        }

        contentPackDao.upsertAll(
            listOf(
                QuranContentPackEntity(
                    packId = packId,
                    type = QuranContentPackType.TRANSLATION,
                    language = "ur",
                    title = "Fateh Muhammad Jalandhry",
                    sizeBytes = dbFile.length(),
                    sourceUrl = "https://api.alquran.cloud/v1/quran/ur.jalandhry",
                    version = 1,
                    isDownloaded = true,
                    localPath = dbFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        )
    }

    private suspend fun seedDefaultTafsirIfNeeded() {
        val packId = DEFAULT_TAFSIR_PACK_ID
        if (contentPackDao.get(packId)?.isDownloaded == true) return

        val packsDir = File(context.filesDir, "quran_packs").apply { mkdirs() }
        val dbFile = File(packsDir, "ar_muyassar.db")
        val packDb = QuranPackDatabases.openTafsir(context, dbFile.absolutePath)
        try {
            val rows = readTafsirAssets().map { it.toEntity() }
            packDb.tafsirTextDao().insertAll(rows)
        } finally {
            packDb.close()
        }

        contentPackDao.upsertAll(
            listOf(
                QuranContentPackEntity(
                    packId = packId,
                    type = QuranContentPackType.TAFSIR,
                    language = "ar",
                    title = "At-Tafsir Al-Muyassar",
                    sizeBytes = dbFile.length(),
                    sourceUrl = "https://quranenc.com/api/v1/translation/sura/arabic_moyassar",
                    version = 1,
                    isDownloaded = true,
                    localPath = dbFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        )
    }

    private suspend fun seedDefaultWordByWordIfNeeded() {
        val packId = DEFAULT_WORD_BY_WORD_PACK_ID
        if (contentPackDao.get(packId)?.isDownloaded == true) return

        val packsDir = File(context.filesDir, "quran_packs").apply { mkdirs() }
        val dbFile = File(packsDir, "en_wordbyword.db")
        val packDb = QuranPackDatabases.openWordByWord(context, dbFile.absolutePath)
        try {
            val rows = readWordByWordAssets().map { it.toEntity() }
            packDb.wordByWordTranslationDao().insertAll(rows)
        } finally {
            packDb.close()
        }

        contentPackDao.upsertAll(
            listOf(
                QuranContentPackEntity(
                    packId = packId,
                    type = QuranContentPackType.WORD_BY_WORD,
                    language = "en",
                    title = "Word by Word",
                    sizeBytes = dbFile.length(),
                    sourceUrl = "https://api.alquran.cloud/v1/quran/quran-wordbyword",
                    version = 1,
                    isDownloaded = true,
                    localPath = dbFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        )
    }

    private fun readChapterAssets(): Array<ChapterAsset> =
        context.assets.open("quran/chapters.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<ChapterAsset>::class.java) }
        }

    private fun readVerseAssets(): Array<VerseAsset> =
        context.assets.open("quran/verses.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<VerseAsset>::class.java) }
        }

    private fun readTranslationAssets(): Array<TranslationAsset> =
        context.assets.open("quran/translations/en_sahih.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<TranslationAsset>::class.java) }
        }

    private fun readUrduTranslationAssets(): Array<TranslationAsset> =
        context.assets.open("quran/translations/ur_jalandhry.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<TranslationAsset>::class.java) }
        }

    private fun readTafsirAssets(): Array<TafsirAsset> =
        context.assets.open("quran/tafsirs/ar_muyassar.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<TafsirAsset>::class.java) }
        }

    private fun readWordSegmentAssets(): Array<WordSegmentAsset> =
        context.assets.open("quran/word_segments.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<WordSegmentAsset>::class.java) }
        }

    private fun readMushafWordAssets(): Array<MushafWordAsset> =
        context.assets.open("quran/mushaf_words.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<MushafWordAsset>::class.java) }
        }

    private fun readWordByWordAssets(): Array<WordByWordAsset> =
        context.assets.open("quran/wordbyword/ar_en_wordbyword.json").use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, Array<WordByWordAsset>::class.java) }
        }

    companion object {
        private const val ONE_TIME_WORK_NAME = "quran_seed_worker"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<QuranSeedWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

private data class ChapterAsset(
    val number: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameTranslation: String,
    val versesCount: Int,
    val revelationPlace: String,
    val revelationOrder: Int,
) {
    fun toEntity() = ChapterEntity(
        number, nameArabic, nameTransliteration, nameTranslation, versesCount,
        RevelationPlace.valueOf(revelationPlace), revelationOrder,
    )
}

private data class VerseAsset(
    val chapterNumber: Int,
    val verseNumber: Int,
    val textUthmani: String,
    val juzNumber: Int,
    val hizbNumber: Int,
    val rubNumber: Int,
    val pageNumber: Int,
    val hasSajda: Boolean,
) {
    fun toEntity() = VerseEntity(
        chapterNumber, verseNumber, textUthmani, juzNumber, hizbNumber, rubNumber, pageNumber, hasSajda,
    )
}

private data class TranslationAsset(
    val chapterNumber: Int,
    val verseNumber: Int,
    val text: String,
) {
    fun toEntity() = TranslationTextEntity(chapterNumber, verseNumber, text)
}

private data class TafsirAsset(
    val chapterNumber: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val text: String,
) {
    fun toEntity() = TafsirTextEntity(chapterNumber = chapterNumber, fromVerse = fromVerse, toVerse = toVerse, text = text)
}

private data class WordSegmentAsset(
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val textArabic: String,
) {
    fun toEntity() = WordSegmentEntity(chapterNumber, verseNumber, position, textArabic)
}

/** The asset is a flat array across all 604 pages, in original API reading order (page 1..604,
 * verses ascending, words ascending within each verse) — a running global index across the whole
 * array is a valid [MushafWordEntity.orderIndex] since it only needs to increase monotonically
 * *within* a page, which this trivially satisfies given the array's own ordering. */
private data class MushafWordAsset(
    val pageNumber: Int,
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val lineNumber: Int,
    val glyphCode: String,
    val isVerseEndMarker: Boolean,
) {
    fun toEntity(orderIndex: Int) = MushafWordEntity(
        pageNumber = pageNumber,
        edition = 1, // QCF V2 — see MushafEdition.QCF_V2.apiId
        orderIndex = orderIndex,
        chapterNumber = chapterNumber,
        verseNumber = verseNumber,
        position = position,
        lineNumber = lineNumber,
        glyphCode = glyphCode,
        isVerseEndMarker = isVerseEndMarker,
    )
}

private data class WordByWordAsset(
    val chapterNumber: Int,
    val verseNumber: Int,
    val position: Int,
    val text: String,
) {
    fun toEntity() = WordByWordTranslationEntity(chapterNumber, verseNumber, position, text)
}
