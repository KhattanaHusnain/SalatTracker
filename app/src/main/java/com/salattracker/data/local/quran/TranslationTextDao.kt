package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TranslationTextDao {
    @Query("SELECT * FROM translation_texts WHERE chapter_number = :chapterNumber ORDER BY verse_number ASC")
    suspend fun getForChapter(chapterNumber: Int): List<TranslationTextEntity>

    @Insert
    suspend fun insertAll(rows: List<TranslationTextEntity>)
}
