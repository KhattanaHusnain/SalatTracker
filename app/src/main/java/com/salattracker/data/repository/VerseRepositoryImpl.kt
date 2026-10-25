package com.salattracker.data.repository

import com.salattracker.domain.model.DEFAULT_TRANSLATION_PACK_ID
import com.salattracker.domain.model.DEFAULT_URDU_TRANSLATION_PACK_ID
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.SettingsRepository
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

@Singleton
class VerseRepositoryImpl @Inject constructor(
    private val contentRepository: QuranContentRepository,
    private val settingsRepository: SettingsRepository,
) : VerseRepository {

    override suspend fun getVerseOfDay(): VerseInfo? {
        val index = Math.floorMod(java.time.LocalDate.now().toEpochDay(), CURATED_VERSE_KEYS.size.toLong()).toInt()
        val (chapterNumber, verseNumber) = CURATED_VERSE_KEYS[index]
        val chapter = contentRepository.getChapter(chapterNumber) ?: return null
        val verse = contentRepository.getVerse(chapterNumber, verseNumber) ?: return null
        val translationPack = if (settingsRepository.getCurrentPrefs().language == AppLanguage.URDU) {
            DEFAULT_URDU_TRANSLATION_PACK_ID
        } else {
            DEFAULT_TRANSLATION_PACK_ID
        }
        val text = contentRepository
            .getTranslationText(translationPack, chapterNumber)
            .find { it.verseNumber == verseNumber }
            ?.text
            ?: return null
        return VerseInfo(text = text, citation = "${chapter.nameTransliteration} $chapterNumber:$verseNumber", arabic = verse.textUthmani)
    }
}
