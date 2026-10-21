package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.salattracker.data.local.entity.QuranBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranBookmarkDao {
    @Query("SELECT * FROM quran_bookmarks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<QuranBookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: QuranBookmarkEntity): Long

    @Update
    suspend fun update(bookmark: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
