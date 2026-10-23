package com.salattracker.notification

import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class QazaAutoMarkDeadlineTest {

    private val date = LocalDate.of(2026, 10, 23)
    private val times = mapOf(
        PrayerType.FAJR to LocalTime.of(5, 0),
        PrayerType.DHUHR to LocalTime.of(12, 20),
        PrayerType.ASR to LocalTime.of(15, 45),
        PrayerType.MAGHRIB to LocalTime.of(18, 5),
        PrayerType.ISHA to LocalTime.of(19, 25),
    )

    @Test
    fun `Fajr deadline is sunrise rather than shortly after Fajr starts`() {
        assertEquals(
            LocalDateTime.of(date, LocalTime.of(6, 20)),
            autoMarkDeadline(PrayerType.FAJR, date, times, LocalTime.of(6, 20), tomorrowTimes()),
        )
    }

    @Test
    fun `Isha deadline is the next day's Fajr`() {
        assertEquals(
            LocalDateTime.of(date.plusDays(1), LocalTime.of(5, 1)),
            autoMarkDeadline(PrayerType.ISHA, date, times, LocalTime.of(6, 20), tomorrowTimes()),
        )
    }

    @Test
    fun `combined Ja'fari pairs stay open until the following distinct prayer`() {
        val combined = times + mapOf(
            PrayerType.ASR to times.getValue(PrayerType.DHUHR),
            PrayerType.ISHA to times.getValue(PrayerType.MAGHRIB),
        )

        assertEquals(LocalDateTime.of(date, times.getValue(PrayerType.MAGHRIB)), autoMarkDeadline(PrayerType.DHUHR, date, combined, LocalTime.of(6, 20), tomorrowTimes()))
        assertEquals(LocalDateTime.of(date.plusDays(1), LocalTime.of(5, 1)), autoMarkDeadline(PrayerType.MAGHRIB, date, combined, LocalTime.of(6, 20), tomorrowTimes()))
    }

    private fun tomorrowTimes() = times + (PrayerType.FAJR to LocalTime.of(5, 1))
}
