package com.salattracker.domain.usecase

import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.repository.PrayerRepository
import javax.inject.Inject

/**
 * CHANGED: Removed the standalone calculateStreakFromEntries() pure function —
 * that logic now lives in PrayerRepositoryImpl.recalculateAndCacheStreak().
 * The use case simply delegates to repository.getStreakSnapshot() and wraps
 * the result in the domain StreakInfo type.
 */
class CalculateStreakUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    suspend operator fun invoke(): StreakInfo {
        val (current, highest) = repository.getStreakSnapshot()
        return StreakInfo(currentStreak = current, highestStreak = highest)
    }
}