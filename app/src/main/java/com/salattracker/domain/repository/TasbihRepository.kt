package com.salattracker.domain.repository

import com.salattracker.domain.model.TasbihIncrementResult
import com.salattracker.domain.model.TasbihPhrase
import com.salattracker.domain.model.TasbihSession
import kotlinx.coroutines.flow.Flow

interface TasbihRepository {
    fun observeSession(goalId: Long?): Flow<TasbihSession?>
    suspend fun ensureSession(goalId: Long?): TasbihSession
    suspend fun increment(goalId: Long?): TasbihIncrementResult
    suspend fun undo(goalId: Long?)
    suspend fun reset(goalId: Long?)
    suspend fun updateConfiguration(goalId: Long?, phrase: TasbihPhrase, target: Int)
}
