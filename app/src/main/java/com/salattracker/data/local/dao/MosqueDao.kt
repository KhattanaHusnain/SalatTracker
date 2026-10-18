package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.salattracker.data.local.entity.MosqueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueDao {
    @Query("SELECT * FROM mosques ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<MosqueEntity>>

    @Query("SELECT * FROM mosques ORDER BY id ASC")
    suspend fun getAll(): List<MosqueEntity>

    @Insert
    suspend fun insert(mosque: MosqueEntity): Long

    @Update
    suspend fun update(mosque: MosqueEntity)

    @Query("DELETE FROM mosques WHERE id = :id")
    suspend fun delete(id: Long)
}
