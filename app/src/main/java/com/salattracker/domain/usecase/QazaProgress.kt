package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

/** The window the "per day" pace and time-to-clear estimate are averaged over. */
const val PACE_WINDOW_DAYS = 14

/** Below this many made-up prayers in the pace window, the pace is too noisy to show an ETA. */
const val MIN_SAMPLES_FOR_ETA = 3

enum class QazaEtaUnit { DAYS, WEEKS, MONTHS, YEARS }

/** "Weeks to clear at this pace: 6" is [value] = 6, [unit] = WEEKS. */
data class QazaEta(val value: Int, val unit: QazaEtaUnit)

data class QazaProgress(
    val outstanding: Int,
    val madeUpTotal: Int,
    /** Made up ÷ (made up + outstanding), 0-100. Null when both are 0 — nothing to show a % of. */
    val madeUpPercent: Int?,
    val madeUpToday: Int,
    val madeUpThisWeek: Int,
    /** Average made-up prayers per day over [PACE_WINDOW_DAYS]. Only meaningful once
     * [hasEnoughSamplesForEta] is true. */
    val perDay: Double,
    /** False when fewer than [MIN_SAMPLES_FOR_ETA] prayers were made up in the pace window — the
     * caller should show a short nudge instead of [eta]. */
    val hasEnoughSamplesForEta: Boolean,
    /** Null whenever [hasEnoughSamplesForEta] is false, or [outstanding] is 0. */
    val eta: QazaEta?,
)

data class QazaRecovery(val outstandingAtStart: Int, val outstandingAtEnd: Int, val madeUp: Int)

/** Reconstructs recovery from original Qaza dates and each completed entry's update timestamp. */
fun qazaRecovery(entries: List<PrayerEntry>, from: LocalDate, to: LocalDate, zone: ZoneId): QazaRecovery {
    fun endOf(date: LocalDate): Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    fun outstandingAt(date: LocalDate) = entries.count { entry ->
        !entry.date.isAfter(date) && (entry.status == PrayerStatus.QAZA_PENDING ||
            (entry.status == PrayerStatus.QAZA_COMPLETED && entry.updatedAt > endOf(date)))
    }
    val startMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return QazaRecovery(
        outstandingAtStart = outstandingAt(from.minusDays(1)),
        outstandingAtEnd = outstandingAt(to),
        madeUp = entries.count { it.status == PrayerStatus.QAZA_COMPLETED && it.updatedAt in startMillis until endMillis },
    )
}

/**
 * Builds the Qaza tab's progress summary from raw counts.
 *
 * @param outstanding total prayers still pending right now.
 * @param madeUpTotal all-time count of prayers ever marked made up.
 * @param madeUpAtMillis the "made up at" timestamp (epoch millis, i.e. each row's `updated_at`)
 *   of every prayer made up within the last [PACE_WINDOW_DAYS] days — exactly what
 *   `observeQazaMadeUpSince(today - PACE_WINDOW_DAYS + 1)` returns. Entries outside the window
 *   must already be excluded by the caller; this function doesn't re-filter by window, only by
 *   today/this-week within it.
 */
fun qazaProgress(
    outstanding: Int,
    madeUpTotal: Int,
    madeUpAtMillis: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): QazaProgress {
    val madeUpDates = madeUpAtMillis.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    val madeUpToday = madeUpDates.count { it == today }
    val weekStart = today.minusDays(6)
    val madeUpThisWeek = madeUpDates.count { !it.isBefore(weekStart) && !it.isAfter(today) }

    val windowCount = madeUpDates.size
    val perDay = windowCount.toDouble() / PACE_WINDOW_DAYS
    val hasEnoughSamples = windowCount >= MIN_SAMPLES_FOR_ETA

    val eta = if (hasEnoughSamples && outstanding > 0 && perDay > 0) {
        etaFor(ceil(outstanding / perDay).toInt())
    } else {
        null
    }

    val totalEverCountable = madeUpTotal + outstanding
    val percent = if (totalEverCountable > 0) (madeUpTotal * 100) / totalEverCountable else null

    return QazaProgress(
        outstanding = outstanding,
        madeUpTotal = madeUpTotal,
        madeUpPercent = percent,
        madeUpToday = madeUpToday,
        madeUpThisWeek = madeUpThisWeek,
        perDay = perDay,
        hasEnoughSamplesForEta = hasEnoughSamples,
        eta = eta,
    )
}

/** [days] (always ≥ 1) to the coarsest unit that keeps the shown number reasonable: days under
 * two weeks, weeks under ~10 weeks, months under two years, years beyond that. */
private fun etaFor(days: Int): QazaEta = when {
    days < 14  -> QazaEta(days, QazaEtaUnit.DAYS)
    days < 70  -> QazaEta(ceil(days / 7.0).toInt(), QazaEtaUnit.WEEKS)
    days < 730 -> QazaEta(ceil(days / 30.0).toInt(), QazaEtaUnit.MONTHS)
    else       -> QazaEta(ceil(days / 365.0).toInt(), QazaEtaUnit.YEARS)
}
