package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerStatus

/** The overall tone one Year-view square shows for a day, derived from whichever of the five
 * prayer statuses were actually recorded that day (there may be fewer than 5 if some are still
 * unrecorded). A pending Qaza always wins, even over an otherwise-complete day, since it's the
 * thing most worth flagging. */
enum class DayTone { ALL_PRAYED, PARTLY, QAZA_PENDING, EXCUSED, NOTHING }

fun dayToneOf(statuses: List<PrayerStatus>): DayTone {
    if (statuses.any { it == PrayerStatus.QAZA_PENDING }) return DayTone.QAZA_PENDING

    val prayedCount = statuses.count {
        it == PrayerStatus.CONGREGATION || it == PrayerStatus.INDIVIDUAL || it == PrayerStatus.QAZA_COMPLETED
    }
    if (prayedCount == 5) return DayTone.ALL_PRAYED
    if (prayedCount > 0) return DayTone.PARTLY

    if (statuses.any { it == PrayerStatus.MENSTRUATION }) return DayTone.EXCUSED
    return DayTone.NOTHING
}

/** How many of [statuses] were actually prayed (congregation or individually) — the "n of 5
 * logged" count shown in the Month view's day panel. Qaza (pending or completed) doesn't count:
 * it tracks what was prayed *on time* that day, same as the existing streak calculation. */
fun completedCount(statuses: List<PrayerStatus>): Int =
    statuses.count { it == PrayerStatus.CONGREGATION || it == PrayerStatus.INDIVIDUAL }
