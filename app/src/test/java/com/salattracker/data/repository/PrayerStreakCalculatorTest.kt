package com.salattracker.data.repository

import com.salattracker.data.local.entity.PrayerEntryEntity
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerStreakCalculatorTest {

    private val start = LocalDate.of(2026, 10, 1)

    @Test
    fun `highest streak is recomputed from corrected history instead of a prior cache`() {
        val entries = listOf(
            entry(0, PrayerStatus.INDIVIDUAL),
            entry(1, PrayerStatus.INDIVIDUAL),
            entry(2, PrayerStatus.QAZA_PENDING), // correction breaks a formerly four-day run
            entry(3, PrayerStatus.INDIVIDUAL),
        )

        val highest = calculateHighestStreaks(entries, start.plusDays(3))

        assertEquals(2, highest.getValue(PrayerType.FAJR))
    }

    @Test
    fun `menstruation is neutral while missing days still break a run`() {
        val entries = listOf(
            entry(0, PrayerStatus.INDIVIDUAL),
            entry(1, PrayerStatus.MENSTRUATION),
            entry(2, PrayerStatus.INDIVIDUAL),
            entry(4, PrayerStatus.INDIVIDUAL),
        )

        val highest = calculateHighestStreaks(entries, start.plusDays(4))

        assertEquals(2, highest.getValue(PrayerType.FAJR))
    }

    @Test
    fun `retrospective prayed edit does not earn highest streak credit`() {
        val prayerDate = start
        val entry = PrayerEntryEntity(
            userId = LOCAL_USER_ID,
            date = prayerDate,
            prayerType = PrayerType.FAJR,
            status = PrayerStatus.INDIVIDUAL,
            updatedAt = prayerDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        assertEquals(0, calculateHighestStreaks(listOf(entry), prayerDate.plusDays(1)).getValue(PrayerType.FAJR))
    }

    private fun entry(dayOffset: Long, status: PrayerStatus) = PrayerEntryEntity(
        userId = LOCAL_USER_ID,
        date = start.plusDays(dayOffset),
        prayerType = PrayerType.FAJR,
        status = status,
        updatedAt = start.plusDays(dayOffset).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}
