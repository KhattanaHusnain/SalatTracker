package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerSlot
import java.time.LocalDate

data class QazaAllocationPreview(
    val slots: List<PrayerSlot>,
    val requested: Int,
    val skipped: Int,
) {
    val added: Int get() = slots.size
}

fun cappedQazaGoal(configuredGoal: Int, outstanding: Int): Int =
    minOf(configuredGoal.coerceAtLeast(0), outstanding.coerceAtLeast(0))

fun allocateQazaByDates(
    dates: Iterable<LocalDate>,
    prayers: Set<PrayerType>,
    occupied: Set<PrayerSlot>,
): QazaAllocationPreview {
    val requestedSlots = dates.distinct().sorted().flatMap { date ->
        PrayerType.values().filter { it in prayers }.map { PrayerSlot(date, it) }
    }
    val available = requestedSlots.filterNot { it in occupied }
    return QazaAllocationPreview(available, requestedSlots.size, requestedSlots.size - available.size)
}

/** Assigns count-only debt to real dates, moving backward from the day before tracking began. */
fun allocateQazaCounts(
    counts: Map<PrayerType, Int>,
    firstRecordedDate: LocalDate,
    occupied: Set<PrayerSlot>,
): QazaAllocationPreview {
    val remaining = PrayerType.values().associateWith { (counts[it] ?: 0).coerceAtLeast(0) }.toMutableMap()
    val requested = remaining.values.sum()
    if (requested == 0) return QazaAllocationPreview(emptyList(), 0, 0)

    val result = ArrayList<PrayerSlot>(requested)
    var skipped = 0
    var date = firstRecordedDate.minusDays(1)
    while (remaining.values.any { it > 0 }) {
        PrayerType.values().forEach { prayer ->
            if (remaining.getValue(prayer) <= 0) return@forEach
            val slot = PrayerSlot(date, prayer)
            if (slot in occupied) skipped++ else {
                result += slot
                remaining[prayer] = remaining.getValue(prayer) - 1
            }
        }
        date = date.minusDays(1)
    }
    return QazaAllocationPreview(result, requested, skipped)
}

/**
 * Expands a years-long estimate without ever materialising rows in Compose. Women can exclude
 * a deterministic approximation: the first [excusedDaysPerMonth] calendar days of each month.
 */
fun allocateOlderQaza(
    years: Int,
    firstRecordedDate: LocalDate,
    occupied: Set<PrayerSlot>,
    excusedDaysPerMonth: Int = 0,
): QazaAllocationPreview {
    val safeYears = years.coerceIn(1, 30)
    val safeExcused = excusedDaysPerMonth.coerceIn(0, 15)
    val from = firstRecordedDate.minusYears(safeYears.toLong())
    val to = firstRecordedDate.minusDays(1)
    val result = ArrayList<PrayerSlot>()
    var requested = 0
    var skipped = 0
    var date = from
    while (!date.isAfter(to)) {
        if (date.dayOfMonth > safeExcused) {
            PrayerType.values().forEach { prayer ->
                requested++
                val slot = PrayerSlot(date, prayer)
                if (slot in occupied) skipped++ else result += slot
            }
        }
        date = date.plusDays(1)
    }
    return QazaAllocationPreview(result, requested, skipped)
}

fun pickQazaForSettlement(
    pending: List<QazaSlot>,
    counts: Map<PrayerType, Int>,
    oldestFirst: Boolean,
): List<QazaSlot> {
    val ordering = if (oldestFirst) compareBy<QazaSlot> { it.date } else compareByDescending { it.date }
    return PrayerType.values().flatMap { prayer ->
        pending.asSequence()
            .filter { it.prayerType == prayer }
            .sortedWith(ordering)
            .take((counts[prayer] ?: 0).coerceAtLeast(0))
            .toList()
    }
}
