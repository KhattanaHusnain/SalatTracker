package com.salattracker.domain.repository

import kotlinx.coroutines.flow.Flow

data class QazaPreferences(
    val dailyGoal: Int = 5,
    val oldestFirst: Boolean = true,
    val soundEnabled: Boolean = true,
    val hideNavigationBadge: Boolean = false,
    val reducedMotion: Boolean = false,
)

interface QazaPreferencesRepository {
    fun observe(): Flow<QazaPreferences>
    suspend fun get(): QazaPreferences
    suspend fun save(preferences: QazaPreferences)
}
