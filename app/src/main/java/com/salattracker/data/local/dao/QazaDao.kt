package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salattracker.data.local.entity.QazaActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QazaDao {
    @Query("SELECT * FROM qaza_activity WHERE user_id = :userId ORDER BY recorded_at DESC, id DESC LIMIT 500")
    fun observeActivities(userId: String): Flow<List<QazaActivityEntity>>

    @Query("SELECT * FROM qaza_activity WHERE user_id = :userId ORDER BY recorded_at DESC, id DESC")
    suspend fun getActivities(userId: String): List<QazaActivityEntity>

    @Query("SELECT * FROM qaza_activity WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getActivity(userId: String, id: Long): QazaActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: QazaActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<QazaActivityEntity>): List<Long>

    @Query("DELETE FROM qaza_activity WHERE id IN (:ids)")
    suspend fun deleteActivities(ids: List<Long>)

    @Query("DELETE FROM qaza_activity WHERE user_id = :userId")
    suspend fun clearActivities(userId: String)
}
