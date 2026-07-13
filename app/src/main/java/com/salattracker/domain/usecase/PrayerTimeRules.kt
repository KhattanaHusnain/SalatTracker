package com.salattracker.domain.usecase

import java.time.LocalDate
import java.time.LocalTime

/**
 * A prayer can only be marked once it could plausibly have happened:
 * any day strictly before today, or today once its time has passed.
 * Any date after today is always blocked. An unknown time (prayer times
 * unavailable) defaults to allowed rather than blocking marking entirely.
 */
fun isPrayerMarkable(
    date: LocalDate,
    prayerTime: LocalTime?,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Boolean = when {
    date.isBefore(today) -> true
    date.isAfter(today)  -> false
    else                 -> prayerTime == null || !now.isBefore(prayerTime)
}
