package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerSlot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QazaV10RulesTest {
    private val trackingStart = LocalDate.of(2026, 10, 20)

    @Test fun `date allocation skips occupied slots without overwriting`() {
        val dates = listOf(trackingStart, trackingStart.plusDays(1))
        val occupied = setOf(PrayerSlot(trackingStart, PrayerType.FAJR))
        val preview = allocateQazaByDates(dates, setOf(PrayerType.FAJR, PrayerType.DHUHR), occupied)
        assertEquals(4, preview.requested)
        assertEquals(3, preview.added)
        assertEquals(1, preview.skipped)
        assertFalse(PrayerSlot(trackingStart, PrayerType.FAJR) in preview.slots)
    }

    @Test fun `count allocation uses real dates before tracking and preserves prayer counts`() {
        val preview = allocateQazaCounts(
            mapOf(PrayerType.FAJR to 2, PrayerType.ISHA to 1), trackingStart, emptySet(),
        )
        assertEquals(3, preview.added)
        assertEquals(listOf(trackingStart.minusDays(1), trackingStart.minusDays(2)), preview.slots.filter { it.prayerType == PrayerType.FAJR }.map { it.date })
        assertEquals(trackingStart.minusDays(1), preview.slots.single { it.prayerType == PrayerType.ISHA }.date)
    }

    @Test fun `count allocation continues backward when a slot is occupied`() {
        val occupied = setOf(PrayerSlot(trackingStart.minusDays(1), PrayerType.FAJR))
        val preview = allocateQazaCounts(mapOf(PrayerType.FAJR to 1), trackingStart, occupied)
        assertEquals(trackingStart.minusDays(2), preview.slots.single().date)
        assertEquals(1, preview.skipped)
    }

    @Test fun `women estimate skips deterministic first dates of every month`() {
        val preview = allocateOlderQaza(1, LocalDate.of(2026, 1, 1), emptySet(), excusedDaysPerMonth = 7)
        assertTrue(preview.slots.isNotEmpty())
        assertTrue(preview.slots.all { it.date.dayOfMonth > 7 })
        assertEquals(0, preview.slots.size % 5)
    }

    @Test fun `thirty year estimate remains bounded and complete`() {
        val preview = allocateOlderQaza(30, trackingStart, emptySet())
        assertTrue(preview.added > 54_000)
        assertEquals(preview.requested, preview.added)
        assertEquals(0, preview.skipped)
    }

    @Test fun `daily goal is capped to available debt`() {
        assertEquals(3, cappedQazaGoal(15, 3))
        assertEquals(10, cappedQazaGoal(10, 40))
    }

    @Test fun `settlement honours oldest and newest order`() {
        val slots = listOf(
            QazaSlot(trackingStart.minusDays(5), PrayerType.FAJR),
            QazaSlot(trackingStart.minusDays(1), PrayerType.FAJR),
        )
        assertEquals(trackingStart.minusDays(5), pickQazaForSettlement(slots, mapOf(PrayerType.FAJR to 1), true).single().date)
        assertEquals(trackingStart.minusDays(1), pickQazaForSettlement(slots, mapOf(PrayerType.FAJR to 1), false).single().date)
    }
}
