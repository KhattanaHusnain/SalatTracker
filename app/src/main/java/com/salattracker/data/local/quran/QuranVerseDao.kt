package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Named "Quran*" (not just "VerseDao") to stay distinct from the existing verse-of-the-day
// VerseDao in data/local/dao/SupportingDaos.kt, an unrelated small cache table in SalatDatabase.
@Dao
interface QuranVerseDao {
    @Query("SELECT * FROM verses WHERE chapter_number = :chapterNumber ORDER BY verse_number ASC")
    suspend fun getForChapter(chapterNumber: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE chapter_number = :chapterNumber AND verse_number = :verseNumber")
    suspend fun get(chapterNumber: Int, verseNumber: Int): VerseEntity?

    // Navigation: a juz/hizb/page's first verse is simply the lowest (chapter, verse) carrying
    // that number — static Mushaf facts, no boundary table needed.
    @Query(
        "SELECT * FROM verses WHERE juz_number = :juzNumber " +
            "ORDER BY chapter_number ASC, verse_number ASC LIMIT 1"
    )
    suspend fun firstOfJuz(juzNumber: Int): VerseEntity?

    /** Every surah that contributes at least one ayah to a juz, in Quran order. */
    @Query(
        "SELECT DISTINCT chapter_number FROM verses WHERE juz_number = :juzNumber " +
            "ORDER BY chapter_number ASC"
    )
    suspend fun chapterNumbersForJuz(juzNumber: Int): List<Int>

    @Query(
        "SELECT * FROM verses WHERE hizb_number = :hizbNumber " +
            "ORDER BY chapter_number ASC, verse_number ASC LIMIT 1"
    )
    suspend fun firstOfHizb(hizbNumber: Int): VerseEntity?

    @Query(
        "SELECT * FROM verses WHERE page_number = :pageNumber " +
            "ORDER BY chapter_number ASC, verse_number ASC LIMIT 1"
    )
    suspend fun firstOfPage(pageNumber: Int): VerseEntity?

    // Bundled-content seeding only — see QuranSeedWorker.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)
}
