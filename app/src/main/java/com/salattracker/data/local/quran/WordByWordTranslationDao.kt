package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordByWordTranslationDao {
    @Query(
        "SELECT * FROM word_by_word_translations WHERE chapter_number = :chapterNumber AND verse_number = :verseNumber " +
            "ORDER BY position ASC"
    )
    suspend fun getForVerse(chapterNumber: Int, verseNumber: Int): List<WordByWordTranslationEntity>

    @Query("SELECT * FROM word_by_word_translations WHERE chapter_number = :chapterNumber ORDER BY verse_number ASC, position ASC")
    suspend fun getForChapter(chapterNumber: Int): List<WordByWordTranslationEntity>

    @Insert
    suspend fun insertAll(rows: List<WordByWordTranslationEntity>)
}
