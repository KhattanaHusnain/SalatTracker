package com.salattracker.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class TimeFormattingTest {

    @Test
    fun `formats 12 and 24 hour times from the same preference`() {
        val time = LocalTime.of(15, 5)

        assertEquals("03:05 PM", time.formatAppTime(use24HourFormat = false, locale = Locale.US))
        assertEquals("15:05", time.formatAppTime(use24HourFormat = true, locale = Locale.US))
    }
}
