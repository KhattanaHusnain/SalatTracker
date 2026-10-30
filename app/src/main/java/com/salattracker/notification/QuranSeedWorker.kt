package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.salattracker.data.local.dao.QuranContentPackDao
import com.salattracker.data.local.entity.QuranContentPackEntity
import com.salattracker.data.local.quran.ChapterDao
import com.salattracker.data.local.quran.ChapterEntity
import com.salattracker.data.local.quran.MushafWordDao
import com.salattracker.data.local.quran.MushafWordEntity
import com.salattracker.data.local.quran.QuranPackDatabases
import com.salattracker.data.local.quran.QuranVerseDao
import com.salattracker.data.local.quran.TranslationTextEntity
import com.salattracker.data.local.quran.VerseEntity
import com.salattracker.domain.model.DEFAULT_TRANSLATION_PACK_ID
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.QuranContentPackType
import com.salattracker.domain.model.RevelationPlace
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.InputStreamReader

/**
 * Imports bundled Quran content into QuranCoreDatabase (the chapters, Uthmani verse text, base
 * and Mushaf page/glyph layout in assets/quran) and seeds the default English and Urdu
 * translation packs on first run -- a no-op on every run after the first. The asset JSON is
 * sourced from alquran.cloud; the Arabic ultimately comes from the Tanzil Project, which
 * alquran.cloud serves under terms that explicitly permit storing/bundling it, unlike
 * Quran.com/Quran Foundation's own general content API -- see QURAN_CONTENT_SOURCES.md for the
 * full correction and the licensing basis, including the attribution this owes on the app's
 * About/credits screen. The Mushaf glyph data and its per-page QCF V2 fonts are bundled in an
 * on-demand Play Asset Delivery pack; see QURAN_CONTENT_SOURCES.md.
 */
@HiltWorker
class QuranSeedWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val chapterDao: ChapterDao,
    private val verseDao: QuranVerseDao,
    private val mushafWordDao: MushafWordDao,
    private val contentPackDao: QuranContentPackDao,
) : CoroutineWorker(context, params) {

    private val assets = QuranSeedAssetReader(context)

    override suspend fun doWork(): Result {
        return try {
            seedCoreContentIfNeeded()
            seedMushafWordsIfNeeded()
            seedDefaultTranslationIfNeeded()
            seedDefaultUrduTranslationIfNeeded()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun seedCoreContentIfNeeded() {
        if (chapterDao.get(1) != null) return // already seeded

        val chapters = assets.chapters()
        chapterDao.insertAll(chapters)

        val verses = assets.verses()
        verseDao.insertAll(verses)
    }

    private suspend fun seedMushafWordsIfNeeded() {
        if (mushafWordDao.countForFirstPage(edition = 1) > 0) return // already seeded

        val words = assets.mushafWords()
        mushafWordDao.insertAll(words)
    }

    private suspend fun seedDefaultTranslationIfNeeded() {
        val packId = DEFAULT_TRANSLATION_PACK_ID
        if (contentPackDao.get(packId)?.isDownloaded == true) return

        val packsDir = File(context.filesDir, "quran_packs").apply { mkdirs() }
        val dbFile = File(packsDir, "en_sahih.db")
        val packDb = QuranPackDatabases.openTranslation(context, dbFile.absolutePath)
        try {
            val rows = assets.englishTranslation()
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
            val rows = assets.urduTranslation()
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

    companion object {
        private const val ONE_TIME_WORK_NAME = "quran_seed_worker"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<QuranSeedWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

/** Reads the bundled Quran wire format into the entities consumed by the seed worker. Keeping the
 * JSON boundary in one production component lets optimized instrumentation exercise the exact
 * reflection path without exposing the private wire DTOs or adding release keep rules. */
internal class QuranSeedAssetReader(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    fun chapters(): List<ChapterEntity> =
        read("quran/chapters.json", Array<ChapterAsset>::class.java).map(ChapterAsset::toEntity)

    fun verses(): List<VerseEntity> =
        read("quran/verses.json", Array<VerseAsset>::class.java).map(VerseAsset::toEntity)

    fun englishTranslation(): List<TranslationTextEntity> =
        read("quran/translations/en_sahih.json", Array<TranslationAsset>::class.java)
            .map(TranslationAsset::toEntity)

    fun urduTranslation(): List<TranslationTextEntity> =
        read("quran/translations/ur_jalandhry.json", Array<TranslationAsset>::class.java)
            .map(TranslationAsset::toEntity)

    fun mushafWords(): List<MushafWordEntity> =
        read("quran/mushaf_words.json", Array<MushafWordAsset>::class.java)
            .mapIndexed { index, asset -> asset.toEntity(orderIndex = index) }

    private fun <T> read(path: String, type: Class<Array<T>>): Array<T> =
        context.assets.open(path).use { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { reader -> gson.fromJson(reader, type) }
        }
}

private data class ChapterAsset(
    @SerializedName("number") val number: Int,
    @SerializedName("nameArabic") val nameArabic: String,
    @SerializedName("nameTransliteration") val nameTransliteration: String,
    @SerializedName("nameTranslation") val nameTranslation: String,
    @SerializedName("versesCount") val versesCount: Int,
    @SerializedName("revelationPlace") val revelationPlace: String,
    @SerializedName("revelationOrder") val revelationOrder: Int,
) {
    fun toEntity() = ChapterEntity(
        number, nameArabic, nameTransliteration, nameTranslation, versesCount,
        RevelationPlace.valueOf(revelationPlace), revelationOrder,
    )
}

private data class VerseAsset(
    @SerializedName("chapterNumber") val chapterNumber: Int,
    @SerializedName("verseNumber") val verseNumber: Int,
    @SerializedName("textUthmani") val textUthmani: String,
    @SerializedName("juzNumber") val juzNumber: Int,
    @SerializedName("hizbNumber") val hizbNumber: Int,
    @SerializedName("rubNumber") val rubNumber: Int,
    @SerializedName("pageNumber") val pageNumber: Int,
    @SerializedName("hasSajda") val hasSajda: Boolean,
) {
    fun toEntity() = VerseEntity(
        chapterNumber, verseNumber, textUthmani, juzNumber, hizbNumber, rubNumber, pageNumber, hasSajda,
    )
}

private data class TranslationAsset(
    @SerializedName("chapterNumber") val chapterNumber: Int,
    @SerializedName("verseNumber") val verseNumber: Int,
    @SerializedName("text") val text: String,
) {
    fun toEntity() = TranslationTextEntity(chapterNumber, verseNumber, text)
}

/** The asset is a flat array across all 604 pages, in original API reading order (page 1..604,
 * verses ascending, words ascending within each verse) — a running global index across the whole
 * array is a valid [MushafWordEntity.orderIndex] since it only needs to increase monotonically
 * *within* a page, which this trivially satisfies given the array's own ordering. */
private data class MushafWordAsset(
    @SerializedName("pageNumber") val pageNumber: Int,
    @SerializedName("chapterNumber") val chapterNumber: Int,
    @SerializedName("verseNumber") val verseNumber: Int,
    @SerializedName("position") val position: Int,
    @SerializedName("lineNumber") val lineNumber: Int,
    @SerializedName("glyphCode") val glyphCode: String,
    @SerializedName("isVerseEndMarker") val isVerseEndMarker: Boolean,
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
