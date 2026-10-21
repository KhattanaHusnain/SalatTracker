package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salattracker.data.local.entity.QuranContentPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranContentPackDao {
    @Query("SELECT * FROM quran_content_packs ORDER BY type ASC, language ASC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<QuranContentPackEntity>>

    @Query("SELECT * FROM quran_content_packs WHERE is_downloaded = 1 ORDER BY type ASC, language ASC, title COLLATE NOCASE ASC")
    fun observeDownloaded(): Flow<List<QuranContentPackEntity>>

    @Query("SELECT * FROM quran_content_packs WHERE pack_id = :packId")
    suspend fun get(packId: String): QuranContentPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(packs: List<QuranContentPackEntity>)

    @Query("UPDATE quran_content_packs SET is_downloaded = 1, local_path = :localPath, downloaded_at = :downloadedAt WHERE pack_id = :packId")
    suspend fun markDownloaded(packId: String, localPath: String, downloadedAt: Long)

    @Query("UPDATE quran_content_packs SET is_downloaded = 0, local_path = NULL, downloaded_at = NULL WHERE pack_id = :packId")
    suspend fun markRemoved(packId: String)
}
