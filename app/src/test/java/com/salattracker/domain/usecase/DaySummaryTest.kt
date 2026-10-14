package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DaySummaryTest {

    @Test
    fun `all five prayed is ALL_PRAYED`() {
        val statuses = listOf(
            PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.CONGREGATION,
            PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_COMPLETED,
        )
        assertEquals(DayTone.ALL_PRAYED, dayToneOf(statuses))
    }

    @Test
    fun `some but not all prayed is PARTLY`() {
        assertEquals(DayTone.PARTLY, dayToneOf(listOf(PrayerStatus.CONGREGATION, PrayerStatus.NOT_RECORDED)))
    }

    @Test
    fun `a pending qaza dominates even when every other prayer was prayed`() {
        val statuses = listOf(
            PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION,
            PrayerStatus.CONGREGATION, PrayerStatus.QAZA_PENDING,
        )
        assertEquals(DayTone.QAZA_PENDING, dayToneOf(statuses))
    }

    @Test
    fun `all excused with nothing prayed is EXCUSED`() {
        assertEquals(DayTone.EXCUSED, dayToneOf(listOf(PrayerStatus.MENSTRUATION, PrayerStatus.MENSTRUATION)))
    }

    @Test
    fun `empty or fully unrecorded is NOTHING`() {
        assertEquals(DayTone.NOTHING, dayToneOf(emptyList()))
        assertEquals(DayTone.NOTHING, dayToneOf(listOf(PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED)))
    }

    @Test
    fun `completedCount only counts congregation and individual, not qaza`() {
        val statuses = listOf(
            PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING,
            PrayerStatus.QAZA_COMPLETED, PrayerStatus.MENSTRUATION,
        )
        assertEquals(2, completedCount(statuses))
    }
}
