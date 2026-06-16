package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.repository.PrayerRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calculates the current and highest prayer streak from Room data.
 *
 * Rules (from PRD):
 *  - CONGREGATION / INDIVIDUAL  → counts toward streak
 *  - QAZA_PENDING / QAZA_COMPLETED → breaks streak
 *  - MENSTRUATION (all 5 prayers) → neutral, day is skipped
 *  - NOT_RECORDED (any prayer)   → breaks streak
 */
class CalculateStreakUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    suspend operator fun invoke(): StreakInfo {
        val (current, highest) = repository.getStreakSnapshot()
        return StreakInfo(currentStreak = current, highestStreak = highest)
    }
}

/**
 * Pure function used by the data layer / worker when persisting a new snapshot.
 * Walks backward from [today], applying the streak rules above.
 */
fun calculateStreakFromEntries(
    entriesByDate: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    today: LocalDate = LocalDate.now(),
): StreakInfo {
    var current = 0
    var highest = 0
    var date    = today

    while (true) {
        val dayEntries = entriesByDate[date] ?: break   // no data → stop

        val statuses = PrayerType.values().map { dayEntries[it] ?: PrayerStatus.NOT_RECORDED }

        when {
            // All five are MENSTRUATION → neutral, skip this day
            statuses.all { it == PrayerStatus.MENSTRUATION } -> { /* skip */ }

            // All prayers count (CONGREGATION or INDIVIDUAL) → extend streak
            statuses.all {
                it == PrayerStatus.CONGREGATION || it == PrayerStatus.INDIVIDUAL
            } -> {
                current++
                if (current > highest) highest = current
            }

            // Anything else (QAZA, NOT_RECORDED, mixed) → streak broken
            else -> break
        }

        date = date.minusDays(1)
    }

    return StreakInfo(currentStreak = current, highestStreak = highest)
}