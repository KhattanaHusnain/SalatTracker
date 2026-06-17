package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * CHANGED:
 * - Fixed broken interface body (all methods now inside the interface braces).
 * - Removed cross-layer imports of feature.stats.* types — replaced with domain-
 *   owned types StreakPoint and PrayerBreakdown declared at the bottom of this file.
 * - PeriodStats moved inside this file.
 */
interface PrayerRepository {

    // ── Today / single day ────────────────────────────────────────────────────

    fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>>

    suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry>

    suspend fun upsertPrayerStatus(
        date: LocalDate,
        prayerType: PrayerType,
        status: PrayerStatus,
    )

    // ── Month range ───────────────────────────────────────────────────────────

    fun observeMonthEntries(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Map<LocalDate, List<PrayerEntry>>>

    // ── Prayer times ──────────────────────────────────────────────────────────

    suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>?

    // ── Hijri ─────────────────────────────────────────────────────────────────

    suspend fun getHijriLabel(date: LocalDate): String

    // ── Streak ────────────────────────────────────────────────────────────────

    suspend fun getStreakSnapshot(): Pair<Int, Int>

    // ── Qaza ──────────────────────────────────────────────────────────────────

    fun observeQazaBalance(): Flow<Map<PrayerType, Int>>

    fun observePendingQazaEntries(): Flow<List<PrayerEntry>>

    suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType)

    // ── Analytics ─────────────────────────────────────────────────────────────

    suspend fun getStatsForPeriod(from: LocalDate, to: LocalDate): PeriodStats

    /**
     * Daily completion points for the streak line chart.
     * [periodLabel] controls label format: "week" → "Mon", "month" → "d", "year" → "MMM".
     */
    suspend fun getStreakPoints(
        from: LocalDate,
        to: LocalDate,
        periodLabel: String,       // "week" | "month" | "year"
    ): List<StreakPoint>

    suspend fun getPrayerBreakdown(
        from: LocalDate,
        to: LocalDate,
    ): List<PrayerBreakdown>
}

// ── Domain result types (no feature-layer imports) ────────────────────────────

data class PeriodStats(
    val congregationPct: Int,
    val individualPct: Int,
    val missedPct: Int,
)

data class StreakPoint(
    val label: String,
    val completedCount: Int,
)

data class PrayerBreakdown(
    val prayerType: PrayerType,
    val completionRate: Float,
)