package com.salattracker.domain.usecase

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriDateTest {

    @Test
    fun `conversion succeeds and fields are in range for a modern date`() {
        val hijri = LocalDate.of(2026, 10, 14).toHijriOrNull()
        assertNotNull(hijri)
        assertTrue(hijri!!.day in 1..30)
        assertTrue(hijri.month in 1..12)
        assertTrue(hijri.year in 1400..1500)
    }

    @Test
    fun `consecutive Gregorian days give strictly increasing Hijri dates`() {
        val a = LocalDate.of(2026, 10, 14).toHijriOrNull()!!
        val b = LocalDate.of(2026, 10, 15).toHijriOrNull()!!
        fun key(h: HijriDate) = h.year * 400 + h.month * 31 + h.day
        assertTrue(key(b) > key(a))
    }

    @Test
    fun `a Hijri month rolls from its last day into day 1 of the next`() {
        // Walk forward from a known date until the Hijri day number drops back to 1 — confirms
        // month/day rollover is internally consistent, without hardcoding exact calendar dates.
        var date = LocalDate.of(2026, 1, 1)
        var previous = date.toHijriOrNull()!!
        var sawRollover = false
        repeat(400) {
            date = date.plusDays(1)
            val current = date.toHijriOrNull()!!
            if (current.day == 1 && previous.day > 1) sawRollover = true
            previous = current
        }
        assertTrue(sawRollover)
    }
}
