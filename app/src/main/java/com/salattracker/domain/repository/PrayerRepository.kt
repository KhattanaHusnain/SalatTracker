package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
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

    /** Sets [status] for every listed (date, prayer type) pair in one write, recalculating streaks once at the end. */
    suspend fun bulkUpsertPrayerStatus(
        entries: List<Pair<LocalDate, PrayerType>>,
        status: PrayerStatus,
    )

    // ── Month range ───────────────────────────────────────────────────────────

    fun observeMonthEntries(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Map<LocalDate, List<PrayerEntry>>>

    // ── Prayer times ──────────────────────────────────────────────────────────

    suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>?

    // ── Streak ────────────────────────────────────────────────────────────────

    /** Independent current/highest streak per prayer type — missing entries default to StreakInfo(0, 0). */
    suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo>

    /** Same data as [getStreakSnapshot], but live — re-emits on any change, from any screen. */
    fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>>

    // ── Qaza ──────────────────────────────────────────────────────────────────

    fun observeQazaBalance(): Flow<Map<PrayerType, Int>>

    fun observePendingQazaEntries(): Flow<List<PrayerEntry>>

    suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType)

    // ── Analytics ─────────────────────────────────────────────────────────────

    suspend fun getStatsForPeriod(from: LocalDate, to: LocalDate): PeriodStats

    /**
     * Percentage-composition points for the status-trend multi-line chart over [from]..[to],
     * where [days] is the range length driving both bucketing and label format:
     *   - days ≤ 40   → one point per day  (label "EEE" when days == 7, else "d")
     *   - 40 < days ≤ 120 → weekly buckets counted back from [to] (label "d MMM")
     *   - days > 120  → monthly buckets     (label "MMM")
     * Each point carries the % of recorded prayers in that bucket that were congregation /
     * individual / qaza (the three sum to ≤ 100; the remainder is un-recorded prayers).
     */
    suspend fun getStatusTrendPoints(
        from: LocalDate,
        to: LocalDate,
        days: Int,
    ): List<StatusTrendPoint>

    suspend fun getPrayerBreakdown(
        from: LocalDate,
        to: LocalDate,
    ): List<PrayerBreakdown>

    /**
     * Recomputes and re-caches the current/highest streak per prayer type from scratch.
     * Exposed for backup restore, which replaces prayer_entries directly via BackupDao
     * and needs the streak cache (and highestStreak carry-forward) recalculated afterward.
     */
    suspend fun recalculateStreaks()
}

// ── Domain result types (no feature-layer imports) ────────────────────────────

data class PeriodStats(
    val congregationPct: Int,
    val individualPct: Int,
    val missedPct: Int,
)

/** One x-axis bucket of the status-trend chart: the % of recorded prayers of each status. */
data class StatusTrendPoint(
    val label: String,
    val congregationPct: Int,
    val individualPct: Int,
    val qazaPct: Int,
)

data class PrayerBreakdown(
    val prayerType: PrayerType,
    val completionRate: Float,
)