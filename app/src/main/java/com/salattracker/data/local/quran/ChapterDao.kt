package com.salattracker.data.local.quran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters ORDER BY number ASC")
    fun observeAll(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE number = :number")
    suspend fun get(number: Int): ChapterEntity?

    // Bundled-content seeding only — see QuranSeedWorker.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)
}
