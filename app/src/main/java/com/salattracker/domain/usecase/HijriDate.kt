package com.salattracker.domain.usecase

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** A Hijri calendar date — [month] is 1-based (1 = Muharram .. 12 = Dhu al-Hijjah). */
data class HijriDate(val day: Int, val month: Int, val year: Int)

/**
 * Converts via the platform's built-in Hijrah chronology (Umm al-Qura — the default
 * [java.time.chrono.HijrahChronology] instance). Returns null rather than throwing if the
 * platform's Hijrah data can't represent this date, so the UI can simply omit the label
 * instead of showing something wrong.
 */
fun LocalDate.toHijriOrNull(): HijriDate? = try {
    val hijrah = HijrahDate.from(this)
    HijriDate(
        day   = hijrah.get(ChronoField.DAY_OF_MONTH),
        month = hijrah.get(ChronoField.MONTH_OF_YEAR),
        year  = hijrah.get(ChronoField.YEAR),
    )
} catch (e: Exception) {
    null
}
