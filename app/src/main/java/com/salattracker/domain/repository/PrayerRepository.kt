package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

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

    /** Calculated on-device (see [com.salattracker.domain.repository.PrayerTimeCalculator]) from
     * the current [UserPrefs] — no network call, no cache. Null only when no location has been
     * set yet (the (0.0, 0.0) default). */
    suspend fun getPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>?

    /** Sunrise closes Fajr's valid prayer window; null while location is unavailable. */
    suspend fun getSunriseTime(date: LocalDate): LocalTime?

    // ── Streak ────────────────────────────────────────────────────────────────

    /** Independent current/highest streak per prayer type — missing entries default to StreakInfo(0, 0). */
    suspend fun getStreakSnapshot(): Map<PrayerType, StreakInfo>

    /** Same data as [getStreakSnapshot], but live — re-emits on any change, from any screen. */
    fun observeStreaks(): Flow<Map<PrayerType, StreakInfo>>

    // ── Qaza ──────────────────────────────────────────────────────────────────

    fun observeQazaBalance(): Flow<Map<PrayerType, Int>>

    fun observePendingQazaEntries(): Flow<List<PrayerEntry>>

    suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType)

    /** All-time count of prayers ever marked made up (status QAZA_COMPLETED). */
    fun observeQazaMadeUpCount(): Flow<Int>

    /** Every prayer made up on or after [since] (inclusive, start of that local day), newest
     * first — the pace window for [com.salattracker.domain.usecase.qazaProgress]. */
    fun observeQazaMadeUpSince(since: LocalDate): Flow<List<PrayerEntry>>

    /** The most recently made-up prayers, newest first, capped at [limit]. */
    fun observeRecentQazaMadeUp(limit: Int): Flow<List<PrayerEntry>>

    // ── Analytics ─────────────────────────────────────────────────────────────

    /**
     * One [StatusCounts] per date that has at least one recorded prayer in [from]..[to] —
     * dates with nothing recorded are omitted rather than returned as all-zero. Every Stats
     * number (completion %, perfect days, qaza recovery, the trend line, by-weekday) is derived
     * from this single source so they can never disagree with each other.
     */
    suspend fun getDailyStatusCounts(from: LocalDate, to: LocalDate): List<DayStatusCounts>

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

/**
 * How a set of prayer-entry rows (a single prayer's history, or a single day's five prayers)
 * broke down by status. [excused] (Menstruation) is neutral — it's left out of [countable] and
 * [completionPercent] the same way the streak calculation treats it, so it never drags either
 * down. The four counts are mutually exclusive and need not sum to any fixed total, since a
 * date range can include dates with nothing recorded at all.
 */
data class StatusCounts(
    val congregation: Int = 0,
    val individual: Int = 0,
    val qazaPending: Int = 0,
    val qazaCompleted: Int = 0,
    val excused: Int = 0,
) {
    val prayed: Int get() = congregation + individual
    val missed: Int get() = qazaPending + qazaCompleted
    val countable: Int get() = prayed + missed

    /** Prayed as a share of countable prayers, 0–100. Null when [countable] is 0 — there's
     * nothing to compute a percentage of, so the caller should show "—", not "0%". */
    val completionPercent: Int? get() = if (countable > 0) (prayed * 100) / countable else null

    operator fun plus(other: StatusCounts) = StatusCounts(
        congregation  = congregation + other.congregation,
        individual    = individual + other.individual,
        qazaPending   = qazaPending + other.qazaPending,
        qazaCompleted = qazaCompleted + other.qazaCompleted,
        excused       = excused + other.excused,
    )
}

/** One date's status counts across all five prayers. */
data class DayStatusCounts(
    val date: LocalDate,
    val counts: StatusCounts,
)

data class PrayerBreakdown(
    val prayerType: PrayerType,
    val counts: StatusCounts,
) {
    val completionRate: Float? get() = counts.completionPercent?.let { it / 100f }
}
