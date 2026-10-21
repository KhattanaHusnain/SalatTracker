package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TafsirTextDao {
    // A tafsir entry can span a verse range, so a chapter's entries for verse N are every row
    // whose range contains N, not just an exact verse_number match.
    @Query(
        "SELECT * FROM tafsir_texts WHERE chapter_number = :chapterNumber " +
            "ORDER BY from_verse ASC"
    )
    suspend fun getForChapter(chapterNumber: Int): List<TafsirTextEntity>

    @Insert
    suspend fun insertAll(rows: List<TafsirTextEntity>)
}
