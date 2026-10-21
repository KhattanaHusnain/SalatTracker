package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.salattracker.data.local.entity.QuranReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranReadHistoryDao {
    @Query("SELECT * FROM quran_read_history ORDER BY read_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuranReadHistoryEntity>>

    // The most recent row doubles as "continue reading" — no separate last-read-position table.
    @Query("SELECT * FROM quran_read_history ORDER BY read_at DESC LIMIT 1")
    fun observeLast(): Flow<QuranReadHistoryEntity?>

    @Insert
    suspend fun insert(entry: QuranReadHistoryEntity): Long

    @Query("DELETE FROM quran_read_history")
    suspend fun clear()
}
