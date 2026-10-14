package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.YearMonth

// ── Shapes ────────────────────────────────────────────────────────────────────

/** One pending (missed, not yet made up) prayer. */
data class QazaSlot(val date: LocalDate, val prayerType: PrayerType)

/** One date's still-pending prayers, newest-insertion-order preserved by the caller's sort. */
data class QazaDay(val date: LocalDate, val prayers: List<PrayerType>)

/** One calendar month's [QazaDay]s, already in the order they were grouped. */
data class QazaMonth(val month: YearMonth, val days: List<QazaDay>)

// ── Grouping ──────────────────────────────────────────────────────────────────

/** Always exactly 5 keys (every [PrayerType]), so a prayer with nothing pending still shows 0
 * rather than being silently dropped from the tiles row. */
fun pendingByPrayer(pending: List<QazaSlot>): Map<PrayerType, Int> {
    val counts = pending.groupingBy { it.prayerType }.eachCount()
    return PrayerType.values().associateWith { counts[it] ?: 0 }
}

/**
 * Groups [pending] into one [QazaDay] per date, optionally narrowed to a single [prayer].
 * Each day's prayers are in [PrayerType] declaration order. Days are ordered newest-first unless
 * [newestFirst] is false.
 */
fun groupPendingByDay(
    pending: List<QazaSlot>,
    prayer: PrayerType? = null,
    newestFirst: Boolean = true,
): List<QazaDay> {
    val filtered = if (prayer != null) pending.filter { it.prayerType == prayer } else pending
    val byDate = filtered.groupBy { it.date }
    val days = byDate.map { (date, slots) ->
        QazaDay(date, slots.map { it.prayerType }.sortedBy { it.ordinal })
    }
    return if (newestFirst) days.sortedByDescending { it.date } else days.sortedBy { it.date }
}

/** Buckets already-grouped [days] by calendar month, newest month first, preserving each day's
 * own order within its month (so pass already-sorted [days] in the order you want preserved). */
fun groupByMonth(days: List<QazaDay>): List<QazaMonth> {
    val byMonth = days.groupBy { YearMonth.from(it.date) }
    return byMonth.entries
        .sortedByDescending { it.key }
        .map { (month, monthDays) -> QazaMonth(month, monthDays) }
}

// ── Quick log: pick the oldest pending slots to mark made up ─────────────────

/**
 * For each prayer type in [counts], picks that many of the OLDEST pending [QazaSlot]s for that
 * prayer — clamped to however many are actually pending, never more. A prayer type absent from
 * [counts] (or mapped to 0) contributes nothing. The result has no guaranteed order.
 */
fun pickOldestPending(pending: List<QazaSlot>, counts: Map<PrayerType, Int>): List<QazaSlot> {
    val byPrayer = pending.groupBy { it.prayerType }
    return counts.flatMap { (type, count) ->
        if (count <= 0) emptyList()
        else byPrayer[type].orEmpty().sortedBy { it.date }.take(count)
    }
}

// ── Mapping from the repository's pending-entries flow ───────────────────────

fun PrayerEntry.toQazaSlot() = QazaSlot(date = date, prayerType = prayerType)
