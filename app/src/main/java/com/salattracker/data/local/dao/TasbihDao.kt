package com.salattracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salattracker.data.local.entity.TasbihSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_sessions WHERE session_id = :sessionId")
    fun observeSession(sessionId: String): Flow<TasbihSessionEntity?>

    @Query("SELECT * FROM tasbih_sessions WHERE session_id = :sessionId")
    suspend fun getSession(sessionId: String): TasbihSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: TasbihSessionEntity)

    @Update
    suspend fun updateSession(session: TasbihSessionEntity)

    @Query("DELETE FROM tasbih_sessions WHERE goal_id IS NOT NULL")
    suspend fun clearLinkedSessions()

    @Query("DELETE FROM tasbih_sessions WHERE goal_id = :goalId")
    suspend fun clearSessionForGoal(goalId: Long)
}
