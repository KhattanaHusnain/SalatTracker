package com.salattracker.data.repository

import com.salattracker.data.local.dao.VerseDao
import com.salattracker.data.local.entity.VerseCacheEntity
import com.salattracker.data.remote.api.QuranApiService
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.VerseRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Curated, verified-relevant-to-prayer verse references. Text/translation is
 * always fetched from the Quran.com API rather than hand-typed here, so the
 * actual scripture text comes from a maintained source rather than transcription.
 */
private val CURATED_VERSE_KEYS = listOf(
    "2:238", "29:45", "20:14", "4:103", "70:34", "11:114", "2:153", "23:2",
)

private val SURAH_NAMES = mapOf(
    2  to "Al-Baqarah",
    4  to "An-Nisa",
    11 to "Hud",
    20 to "Ta-Ha",
    23 to "Al-Mu'minun",
    29 to "Al-Ankabut",
    70 to "Al-Ma'arij",
)

private val FALLBACK_VERSE = VerseInfo(
    text = "Maintain your prayers with care and devotion.",
    citation = "",
)

@Singleton
class VerseRepositoryImpl @Inject constructor(
    private val verseDao: VerseDao,
    private val quranApiService: QuranApiService,
) : VerseRepository {

    override suspend fun getRandomVerse(): VerseInfo =
        verseDao.getAll().randomOrNull()?.let { VerseInfo(it.text, it.citation) } ?: FALLBACK_VERSE

    override suspend fun refreshCache() {
        val cachedKeys = verseDao.getCachedKeys().toSet()
        val missing = CURATED_VERSE_KEYS.filter { it !in cachedKeys }

        missing.forEach { verseKey ->
            try {
                val response = quranApiService.getVerse(verseKey)
                val text = response.verse.translations.firstOrNull()?.text?.let(::stripHtml) ?: return@forEach
                verseDao.upsert(
                    VerseCacheEntity(
                        verseKey = verseKey,
                        text     = text,
                        citation = citationFor(verseKey),
                    )
                )
            } catch (e: Exception) {
                // Skip this one — will be retried on the next refresh (periodic worker / next app launch).
            }
        }
    }

    private fun citationFor(verseKey: String): String {
        val surahNumber = verseKey.substringBefore(":").toIntOrNull()
        val surahName = SURAH_NAMES[surahNumber] ?: "Quran"
        return "$surahName $verseKey"
    }

    private fun stripHtml(text: String): String =
        text.replace(Regex("<[^>]*>"), "").trim()
}
