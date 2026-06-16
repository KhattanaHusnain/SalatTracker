package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Single repository interface that all ViewModels depend on.
 * The concrete implementation lives in the data layer (Room + Firestore).
 * This interface lives in the domain layer and has zero Android/Room imports.
 */
interface PrayerRepository {

    // ── Today / single day ────────────────────────────────────────────────────

    /** Reactive stream of all 5 prayer entries for [date]. Emits on every DB change. */
    fun observeTodayPrayers(date: LocalDate): Flow<List<PrayerEntry>>

    /** One-shot fetch of all entries for [date] (used for day-detail in Calendar). */
    suspend fun getPrayerEntriesForDate(date: LocalDate): List<PrayerEntry>

    /** Insert or update a single prayer status. Enqueues a sync op automatically. */
    suspend fun upsertPrayerStatus(
        date: LocalDate,
        prayerType: PrayerType,
        status: PrayerStatus,
    )

    // ── Month range ───────────────────────────────────────────────────────────

    /**
     * Reactive stream of all entries between [from] and [to] (inclusive),
     * grouped by date for the Calendar month grid.
     */
    fun observeMonthEntries(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Map<LocalDate, List<PrayerEntry>>>

    // ── Prayer times (AlAdhan cache) ──────────────────────────────────────────

    /**
     * Returns cached prayer times for [date] as a map of PrayerType → LocalTime.
     * Returns null if not yet fetched (UI should show "--:--").
     */
    suspend fun getCachedPrayerTimes(date: LocalDate): Map<PrayerType, LocalTime>?

    // ── Hijri date label ──────────────────────────────────────────────────────

    /** Returns a formatted Hijri date string, e.g. "3 Rajab 1445 AH". */
    suspend fun getHijriLabel(date: LocalDate): String

    // ── Streak ────────────────────────────────────────────────────────────────

    /** Fetches the cached streak snapshot (current + highest). */
    suspend fun getStreakSnapshot(): Pair<Int, Int>

    // ── Qaza ──────────────────────────────────────────────────────────────────

    /**
     * Reactive stream of pending Qaza count per prayer type.
     * Map key = PrayerType, value = count of QAZA_PENDING entries.
     */
    fun observeQazaBalance(): Flow<Map<PrayerType, Int>>

    /**
     * Reactive stream of all QAZA_PENDING entries, ordered by date descending
     * (most recent missed first — matches the "Recent Missed Prayers" list).
     */
    fun observePendingQazaEntries(): Flow<List<PrayerEntry>>

    /**
     * Marks a specific QAZA_PENDING entry as QAZA_COMPLETED and
     * decrements the QazaBalance for that prayer type.
     */
    suspend fun markQazaCompleted(date: LocalDate, prayerType: PrayerType)
}