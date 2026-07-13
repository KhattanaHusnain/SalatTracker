package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Live per-prayer streak snapshot — re-emits whenever any screen changes a prayer's status. */
class CalculateStreakUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    operator fun invoke(): Flow<Map<PrayerType, StreakInfo>> = repository.observeStreaks()
}
