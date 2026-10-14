package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class QazaBacklogTest {

    private val d1 = LocalDate.of(2026, 1, 10)
    private val d2 = LocalDate.of(2026, 1, 11)
    private val d3 = LocalDate.of(2026, 2, 1)

    @Test
    fun `pendingByPrayer always has all 5 keys`() {
        val result = pendingByPrayer(listOf(QazaSlot(d1, PrayerType.FAJR)))
        assertEquals(PrayerType.values().toSet(), result.keys)
        assertEquals(1, result[PrayerType.FAJR])
        assertEquals(0, result[PrayerType.DHUHR])
    }

    @Test
    fun `pendingByPrayer counts per prayer`() {
        val pending = listOf(
            QazaSlot(d1, PrayerType.FAJR), QazaSlot(d2, PrayerType.FAJR),
            QazaSlot(d1, PrayerType.ISHA),
        )
        val result = pendingByPrayer(pending)
        assertEquals(2, result[PrayerType.FAJR])
        assertEquals(1, result[PrayerType.ISHA])
        assertEquals(0, result[PrayerType.ASR])
    }

    @Test
    fun `groupPendingByDay groups and sorts prayers within a day by PrayerType order`() {
        val pending = listOf(
            QazaSlot(d1, PrayerType.ISHA), QazaSlot(d1, PrayerType.FAJR), QazaSlot(d1, PrayerType.DHUHR),
        )
        val days = groupPendingByDay(pending)
        assertEquals(1, days.size)
        assertEquals(listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ISHA), days[0].prayers)
    }

    @Test
    fun `groupPendingByDay defaults to newest first`() {
        val pending = listOf(QazaSlot(d1, PrayerType.FAJR), QazaSlot(d2, PrayerType.FAJR))
        val days = groupPendingByDay(pending)
        assertEquals(listOf(d2, d1), days.map { it.date })
    }

    @Test
    fun `groupPendingByDay can sort oldest first`() {
        val pending = listOf(QazaSlot(d1, PrayerType.FAJR), QazaSlot(d2, PrayerType.FAJR))
        val days = groupPendingByDay(pending, newestFirst = false)
        assertEquals(listOf(d1, d2), days.map { it.date })
    }

    @Test
    fun `groupPendingByDay filters to one prayer`() {
        val pending = listOf(QazaSlot(d1, PrayerType.FAJR), QazaSlot(d1, PrayerType.ISHA))
        val days = groupPendingByDay(pending, prayer = PrayerType.FAJR)
        assertEquals(1, days.size)
        assertEquals(listOf(PrayerType.FAJR), days[0].prayers)
    }

    @Test
    fun `groupByMonth buckets days and orders months newest first`() {
        val days = listOf(QazaDay(d3, listOf(PrayerType.FAJR)), QazaDay(d1, listOf(PrayerType.FAJR)), QazaDay(d2, listOf(PrayerType.ISHA)))
        val months = groupByMonth(days)
        assertEquals(listOf(YearMonth.of(2026, 2), YearMonth.of(2026, 1)), months.map { it.month })
        assertEquals(2, months[1].days.size)
    }

    @Test
    fun `pickOldestPending takes the oldest N per prayer`() {
        val pending = listOf(
            QazaSlot(d2, PrayerType.FAJR), QazaSlot(d1, PrayerType.FAJR), QazaSlot(d3, PrayerType.FAJR),
        )
        val picked = pickOldestPending(pending, mapOf(PrayerType.FAJR to 2))
        assertEquals(setOf(d1, d2), picked.map { it.date }.toSet())
    }

    @Test
    fun `pickOldestPending clamps to what exists`() {
        val pending = listOf(QazaSlot(d1, PrayerType.FAJR))
        val picked = pickOldestPending(pending, mapOf(PrayerType.FAJR to 5))
        assertEquals(1, picked.size)
    }

    @Test
    fun `pickOldestPending ignores a prayer type with zero or missing count`() {
        val pending = listOf(QazaSlot(d1, PrayerType.FAJR), QazaSlot(d1, PrayerType.ISHA))
        val picked = pickOldestPending(pending, mapOf(PrayerType.FAJR to 0))
        assertTrue(picked.isEmpty())
    }

    @Test
    fun `pickOldestPending across multiple prayers`() {
        val pending = listOf(
            QazaSlot(d1, PrayerType.FAJR), QazaSlot(d2, PrayerType.FAJR),
            QazaSlot(d1, PrayerType.ISHA),
        )
        val picked = pickOldestPending(pending, mapOf(PrayerType.FAJR to 1, PrayerType.ISHA to 1))
        assertEquals(setOf(PrayerType.FAJR to d1, PrayerType.ISHA to d1), picked.map { it.prayerType to it.date }.toSet())
    }
}
