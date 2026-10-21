package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MushafWordDao {
    @Query("SELECT * FROM mushaf_words WHERE page_number = :pageNumber AND edition = :edition ORDER BY order_index ASC")
    suspend fun getWords(pageNumber: Int, edition: Int): List<MushafWordEntity>

    // One query per chapter rather than one per verse — see QuranContentRepositoryImpl's
    // getWordSegmentsForChapter for the same bulk-read convention.
    @Query("SELECT * FROM mushaf_words WHERE chapter_number = :chapterNumber AND edition = :edition ORDER BY order_index ASC")
    suspend fun getWordsForChapter(chapterNumber: Int, edition: Int): List<MushafWordEntity>

    @Query("SELECT COUNT(*) FROM mushaf_words WHERE edition = :edition AND page_number = 1")
    suspend fun countForFirstPage(edition: Int): Int

    // Bundled-content seeding only — see QuranSeedWorker.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<MushafWordEntity>)
}
