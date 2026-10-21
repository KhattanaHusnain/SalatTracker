package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordSegmentDao {
    @Query(
        "SELECT * FROM word_segments WHERE chapter_number = :chapterNumber AND verse_number = :verseNumber " +
            "ORDER BY position ASC"
    )
    suspend fun getForVerse(chapterNumber: Int, verseNumber: Int): List<WordSegmentEntity>

    @Query("SELECT * FROM word_segments WHERE chapter_number = :chapterNumber ORDER BY verse_number ASC, position ASC")
    suspend fun getForChapter(chapterNumber: Int): List<WordSegmentEntity>

    // Bundled-content seeding only — see QuranSeedWorker.
    @Insert
    suspend fun insertAll(segments: List<WordSegmentEntity>)
}
