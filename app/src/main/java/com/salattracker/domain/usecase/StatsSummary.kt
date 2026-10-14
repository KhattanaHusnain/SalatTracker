package com.salattracker.domain.usecase

import com.salattracker.domain.repository.PrayerBreakdown

/** Congregation + individual as a share of recorded prayers, excluding Menstruation days —
 * those are neutral (excused), the same treatment the streak calculation already gives them,
 * so they shouldn't drag a completion percentage down. Returns 0 when nothing is countable. */
fun completionPercent(congregation: Int, individual: Int, excused: Int, total: Int): Int {
    val countable = total - excused
    if (countable <= 0) return 0
    return ((congregation + individual) * 100) / countable
}

/** The prayer with the lowest completion rate in [breakdown] — the one Stats calls out as
 * needing the most attention. Null when [breakdown] is empty or every rate is already 100%. */
fun weakestPrayer(breakdown: List<PrayerBreakdown>): PrayerBreakdown? {
    val weakest = breakdown.minByOrNull { it.completionRate } ?: return null
    return weakest.takeIf { it.completionRate < 1f }
}
