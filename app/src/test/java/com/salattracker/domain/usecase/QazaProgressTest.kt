package com.salattracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class QazaProgressTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 1, 15)

    private fun millisOn(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `madeUpToday counts only timestamps on today`() {
        val madeUpAt = listOf(millisOn(today), millisOn(today), millisOn(today.minusDays(1)))
        val progress = qazaProgress(outstanding = 10, madeUpTotal = 20, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertEquals(2, progress.madeUpToday)
    }

    @Test
    fun `madeUpThisWeek counts the trailing 7 days inclusive of today`() {
        val madeUpAt = listOf(
            millisOn(today),               // in window
            millisOn(today.minusDays(6)),  // edge, in window
            millisOn(today.minusDays(7)),  // out of window
        )
        val progress = qazaProgress(outstanding = 10, madeUpTotal = 20, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertEquals(2, progress.madeUpThisWeek)
    }

    @Test
    fun `fewer than MIN_SAMPLES_FOR_ETA samples disables the ETA`() {
        val madeUpAt = listOf(millisOn(today), millisOn(today.minusDays(1)))
        val progress = qazaProgress(outstanding = 100, madeUpTotal = 10, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertFalse(progress.hasEnoughSamplesForEta)
        assertNull(progress.eta)
    }

    @Test
    fun `enough samples with outstanding produces an ETA`() {
        val madeUpAt = List(14) { millisOn(today.minusDays(it.toLong())) } // 1/day for 14 days
        val progress = qazaProgress(outstanding = 13, madeUpTotal = 50, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertTrue(progress.hasEnoughSamplesForEta)
        assertEquals(1.0, progress.perDay, 0.0001)
        assertEquals(QazaEta(13, QazaEtaUnit.DAYS), progress.eta)
    }

    @Test
    fun `eta in days below 14`() {
        val madeUpAt = List(7) { millisOn(today.minusDays(it.toLong())) } // 7 over 14 days -> 0.5/day
        val progress = qazaProgress(outstanding = 5, madeUpTotal = 7, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        // 5 / 0.5 = 10 days
        assertEquals(QazaEta(10, QazaEtaUnit.DAYS), progress.eta)
    }

    @Test
    fun `eta steps up to weeks`() {
        val madeUpAt = List(3) { millisOn(today.minusDays(it.toLong())) } // 3 over 14 days -> perDay ~0.214
        val progress = qazaProgress(outstanding = 10, madeUpTotal = 3, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        // ceil(10 / (3/14)) = ceil(46.67) = 47 days -> weeks = ceil(47/7) = 7
        assertEquals(QazaEtaUnit.WEEKS, progress.eta?.unit)
        assertEquals(7, progress.eta?.value)
    }

    @Test
    fun `eta steps up to months`() {
        val madeUpAt = List(3) { millisOn(today.minusDays(it.toLong())) } // perDay ~0.214
        val progress = qazaProgress(outstanding = 100, madeUpTotal = 3, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        // ceil(100 / (3/14)) = ceil(466.67) = 467 days -> months
        assertEquals(QazaEtaUnit.MONTHS, progress.eta?.unit)
    }

    @Test
    fun `eta steps up to years for a very large backlog`() {
        val madeUpAt = List(3) { millisOn(today.minusDays(it.toLong())) }
        val progress = qazaProgress(outstanding = 10000, madeUpTotal = 3, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertEquals(QazaEtaUnit.YEARS, progress.eta?.unit)
    }

    @Test
    fun `eta is null once outstanding is 0`() {
        val madeUpAt = List(5) { millisOn(today.minusDays(it.toLong())) }
        val progress = qazaProgress(outstanding = 0, madeUpTotal = 50, madeUpAtMillis = madeUpAt, today = today, zone = zone)
        assertNull(progress.eta)
    }

    @Test
    fun `percent is null when nothing has ever been countable`() {
        val progress = qazaProgress(outstanding = 0, madeUpTotal = 0, madeUpAtMillis = emptyList(), today = today, zone = zone)
        assertNull(progress.madeUpPercent)
    }

    @Test
    fun `percent is computed from made up over made up plus outstanding`() {
        val progress = qazaProgress(outstanding = 50, madeUpTotal = 150, madeUpAtMillis = emptyList(), today = today, zone = zone)
        assertEquals(75, progress.madeUpPercent)
    }

    @Test
    fun `percent is 100 once nothing is outstanding but history exists`() {
        val progress = qazaProgress(outstanding = 0, madeUpTotal = 10, madeUpAtMillis = emptyList(), today = today, zone = zone)
        assertEquals(100, progress.madeUpPercent)
    }

    @Test
    fun `a made-up timestamp near local midnight resolves against the given zone`() {
        // 23:30 UTC on the day before `today` is already 09:30 `today` in UTC+10.
        val zonePlus10 = ZoneOffset.ofHours(10)
        val lateNightUtc = today.minusDays(1).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val progress = qazaProgress(outstanding = 1, madeUpTotal = 1, madeUpAtMillis = listOf(lateNightUtc), today = today, zone = zonePlus10)
        assertEquals(1, progress.madeUpToday)
    }
}
