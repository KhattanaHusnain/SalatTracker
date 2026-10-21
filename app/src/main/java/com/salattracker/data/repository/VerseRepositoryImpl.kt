package com.salattracker.data.repository

import com.salattracker.domain.model.DEFAULT_TRANSLATION_PACK_ID
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.VerseRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Curated, verified-relevant-to-prayer verse references. Text and citation come straight from
 * the locally-seeded Quran data (QuranCoreDatabase + the default translation pack, see
 * QuranSeedWorker) — fully offline, no network call or separate cache needed now that the full
 * Quran text lives on-device.
 */
private val CURATED_VERSE_KEYS = listOf(
    2 to 238, 29 to 45, 20 to 14, 4 to 103, 70 to 34, 11 to 114, 2 to 153, 23 to 2,
)

private val FALLBACK_VERSE = VerseInfo(
    text = "Maintain your prayers with care and devotion.",
    citation = "",
)

@Singleton
class VerseRepositoryImpl @Inject constructor(
    private val contentRepository: QuranContentRepository,
) : VerseRepository {

    override suspend fun getRandomVerse(): VerseInfo {
        val (chapterNumber, verseNumber) = CURATED_VERSE_KEYS.random()
        val chapter = contentRepository.getChapter(chapterNumber) ?: return FALLBACK_VERSE
        val text = contentRepository
            .getTranslationText(DEFAULT_TRANSLATION_PACK_ID, chapterNumber)
            .find { it.verseNumber == verseNumber }
            ?.text
            ?: return FALLBACK_VERSE
        return VerseInfo(text = text, citation = "${chapter.nameTransliteration} $chapterNumber:$verseNumber")
    }
}
