package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.salattracker.data.local.entity.QuranSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranSearchHistoryDao {
    @Query("SELECT * FROM quran_search_history ORDER BY searched_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuranSearchHistoryEntity>>

    @Insert
    suspend fun insert(entry: QuranSearchHistoryEntity): Long

    @Query("DELETE FROM quran_search_history")
    suspend fun clear()
}
