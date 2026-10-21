package com.salattracker.data.prayertimes

import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.UserPrefs
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-fixture tests: compares [AdhanPrayerTimeCalculator] against AlAdhan's own published
 * reference values (https://api.aladhan.com/v1/timings), fetched for 2026-07-15 before the
 * on-device swap, so the formerly-live API behaviour has a fixed record to check against.
 *
 * Tolerance is +/-3 minutes — `adhan2`'s presets add small built-in adjustments (e.g. MWL's
 * Dhuhr +1 min) that AlAdhan's own numbers already reflect, so this isn't padding for rounding
 * error; a gap bigger than that should be investigated, not waved through by widening it.
 *
 * London/high-latitude locations are deliberately **not** golden-compared here: AlAdhan's
 * default high-latitude handling and `adhan2`'s [HighLatitudeRule.AUTOMATIC] resolve differently
 * at latitudes where an angle is never reached, so the two aren't expected to agree. See the
 * Oslo test below, which only checks the calculator still returns five times.
 */
class AdhanPrayerTimeCalculatorTest {

    private val calculator = AdhanPrayerTimeCalculator(CalculationParametersFactory())
    private val date = LocalDate.of(2026, 7, 15)
    private val toleranceMinutes = 3L

    private fun prefs(methodId: Int, madhab: Madhab = Madhab.STANDARD, highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTOMATIC) =
        UserPrefs(calculationMethodId = methodId, madhab = madhab, highLatitudeRule = highLatitudeRule)

    private fun assertClose(label: String, expected: String, actual: LocalTime?) {
        requireNotNull(actual) { "$label: calculator returned no times" }
        val expectedTime = LocalTime.parse(expected)
        val diff = Math.abs(java.time.temporal.ChronoUnit.MINUTES.between(expectedTime, actual))
        assertTrue("$label: expected ~$expected, got $actual (${diff}min off)", diff <= toleranceMinutes)
    }

    private fun assertGolden(
        city: String,
        lat: Double,
        lon: Double,
        zone: String,
        methodId: Int,
        madhab: Madhab = Madhab.STANDARD,
        expected: Map<PrayerType, String>,
    ) {
        val times = calculator.calculate(date, lat, lon, prefs(methodId, madhab), ZoneId.of(zone))
        requireNotNull(times) { "$city: calculator returned null" }
        expected.forEach { (type, expectedTime) -> assertClose("$city $type", expectedTime, times[type]) }
    }

    @Test
    fun `Karachi with the Karachi method, standard school`() = assertGolden(
        city = "Karachi", lat = 24.8607, lon = 67.0011, zone = "Asia/Karachi",
        methodId = CalculationMethod.KARACHI.id,
        expected = mapOf(
            PrayerType.FAJR to "04:25", PrayerType.DHUHR to "12:38", PrayerType.ASR to "16:02",
            PrayerType.MAGHRIB to "19:24", PrayerType.ISHA to "20:51",
        ),
    )

    @Test
    fun `Karachi with the Karachi method, Hanafi school`() = assertGolden(
        city = "Karachi (Hanafi)", lat = 24.8607, lon = 67.0011, zone = "Asia/Karachi",
        methodId = CalculationMethod.KARACHI.id, madhab = Madhab.HANAFI,
        expected = mapOf(PrayerType.ASR to "17:20"),
    )

    @Test
    fun `Karachi with MWL`() = assertGolden(
        city = "Karachi (MWL)", lat = 24.8607, lon = 67.0011, zone = "Asia/Karachi",
        methodId = CalculationMethod.MWL.id,
        expected = mapOf(
            PrayerType.FAJR to "04:25", PrayerType.DHUHR to "12:38", PrayerType.ASR to "16:02",
            PrayerType.MAGHRIB to "19:24", PrayerType.ISHA to "20:46",
        ),
    )

    @Test
    fun `Makkah with Umm al-Qura`() = assertGolden(
        city = "Makkah", lat = 21.3891, lon = 39.8579, zone = "Asia/Riyadh",
        methodId = CalculationMethod.UMM_AL_QURA.id,
        expected = mapOf(
            PrayerType.FAJR to "04:21", PrayerType.DHUHR to "12:27", PrayerType.ASR to "15:41",
            PrayerType.MAGHRIB to "19:06", PrayerType.ISHA to "20:36",
        ),
    )

    @Test
    fun `Jakarta with the Indonesian method (a custom-angle method, no adhan2 preset)`() = assertGolden(
        city = "Jakarta", lat = -6.2088, lon = 106.8456, zone = "Asia/Jakarta",
        methodId = CalculationMethod.INDONESIA.id,
        expected = mapOf(
            PrayerType.FAJR to "04:42", PrayerType.DHUHR to "11:59", PrayerType.ASR to "15:20",
            PrayerType.MAGHRIB to "17:52", PrayerType.ISHA to "19:06",
        ),
    )

    @Test
    fun `Istanbul with Turkey`() = assertGolden(
        city = "Istanbul", lat = 41.0082, lon = 28.9784, zone = "Europe/Istanbul",
        methodId = CalculationMethod.TURKEY.id,
        expected = mapOf(
            PrayerType.FAJR to "03:43", PrayerType.DHUHR to "13:15", PrayerType.ASR to "17:14",
            PrayerType.MAGHRIB to "20:42", PrayerType.ISHA to "22:28",
        ),
    )

    // Tehran's AlAdhan reference values below are shifted +1h from the calculator's raw output
    // (`aladhanOffsetMinutes`), confirmed by printing every field for both methods: all five
    // prayers were uniformly 60 minutes earlier than AlAdhan's numbers, which only happens from
    // a timezone/DST offset, not a calculation difference (an angle error wouldn't move every
    // prayer by the same round number of minutes). Iran abolished DST in September 2022; the
    // JVM's current tzdata correctly reports a fixed +03:30 for Asia/Tehran with no DST
    // (verified directly), so this looks like AlAdhan's server still applying Iran's old summer
    // DST rule. The correction is applied explicitly here, rather than silently loosening the
    // tolerance, so the actual angle/Maghrib-by-angle math is still being checked to +/-3min.
    private val aladhanOffsetMinutes = 60L

    @Test
    fun `Tehran method's Maghrib-by-angle matches AlAdhan's own Jafari Maghrib`() = assertGolden(
        city = "Tehran", lat = 35.6892, lon = 51.3890, zone = "Asia/Tehran",
        methodId = CalculationMethod.TEHRAN.id,
        expected = mapOf(
            PrayerType.FAJR to "04:17", PrayerType.DHUHR to "13:10", PrayerType.ASR to "16:59",
            PrayerType.MAGHRIB to "20:41", PrayerType.ISHA to "21:39",
        ).mapValues { (_, v) -> LocalTime.parse(v).minusMinutes(aladhanOffsetMinutes).toString() },
    )

    @Test
    fun `Shia Ithna-Ashari's Maghrib-by-angle matches AlAdhan's own Jafari Maghrib`() = assertGolden(
        city = "Tehran (Shia)", lat = 35.6892, lon = 51.3890, zone = "Asia/Tehran",
        methodId = CalculationMethod.SHIA_ITHNA_ASHARI.id,
        expected = mapOf(
            PrayerType.FAJR to "04:28", PrayerType.DHUHR to "13:10", PrayerType.ASR to "16:59",
            PrayerType.MAGHRIB to "20:39", PrayerType.ISHA to "21:39",
        ).mapValues { (_, v) -> LocalTime.parse(v).minusMinutes(aladhanOffsetMinutes).toString() },
    )

    @Test
    fun `Hanafi Asr is later than standard Asr, for every method`() {
        CalculationMethod.values().forEach { method ->
            val standard = calculator.calculate(date, 24.8607, 67.0011, prefs(method.id, Madhab.STANDARD), ZoneId.of("Asia/Karachi"))
            val hanafi = calculator.calculate(date, 24.8607, 67.0011, prefs(method.id, Madhab.HANAFI), ZoneId.of("Asia/Karachi"))
            val standardAsr = standard?.get(PrayerType.ASR)
            val hanafiAsr = hanafi?.get(PrayerType.ASR)
            if (standardAsr != null && hanafiAsr != null) {
                assertTrue("$method: Hanafi Asr ($hanafiAsr) should be later than standard Asr ($standardAsr)", hanafiAsr.isAfter(standardAsr))
            }
        }
    }

    @Test
    fun `Jafari Asr equals Dhuhr, and Isha equals Maghrib`() {
        CalculationMethod.values().forEach { method ->
            val times = calculator.calculate(date, 24.8607, 67.0011, prefs(method.id, Madhab.JAFARI), ZoneId.of("Asia/Karachi"))
            if (times != null) {
                assertEquals("$method: Jafari Asr should equal Dhuhr", times.getValue(PrayerType.DHUHR), times.getValue(PrayerType.ASR))
                assertEquals("$method: Jafari Isha should equal Maghrib", times.getValue(PrayerType.MAGHRIB), times.getValue(PrayerType.ISHA))
            }
        }
    }

    @Test
    fun `Jafari's Asr and Isha still shift with a per-prayer adjustment`() {
        val times = calculator.calculate(
            date, 24.8607, 67.0011,
            prefs(CalculationMethod.MWL.id, Madhab.JAFARI).copy(prayerTimeAdjustMinutes = mapOf(PrayerType.ASR to 5, PrayerType.ISHA to 7)),
            ZoneId.of("Asia/Karachi"),
        )
        requireNotNull(times)
        assertEquals(times.getValue(PrayerType.DHUHR).plusMinutes(5), times.getValue(PrayerType.ASR))
        assertEquals(times.getValue(PrayerType.MAGHRIB).plusMinutes(7), times.getValue(PrayerType.ISHA))
    }

    @Test
    fun `an unset location returns null`() {
        val times = calculator.calculate(date, 0.0, 0.0, prefs(CalculationMethod.MWL.id))
        assertNull(times)
    }

    @Test
    fun `a per-prayer adjustment shifts exactly that prayer's time`() {
        val base = calculator.calculate(date, 24.8607, 67.0011, prefs(CalculationMethod.MWL.id), ZoneId.of("Asia/Karachi"))
        val adjusted = calculator.calculate(
            date, 24.8607, 67.0011,
            prefs(CalculationMethod.MWL.id).copy(prayerTimeAdjustMinutes = mapOf(PrayerType.FAJR to 5)),
            ZoneId.of("Asia/Karachi"),
        )
        assertEquals(base!!.getValue(PrayerType.FAJR).plusMinutes(5), adjusted!!.getValue(PrayerType.FAJR))
        // Nothing else should move.
        assertEquals(base.getValue(PrayerType.DHUHR), adjusted.getValue(PrayerType.DHUHR))
        assertEquals(base.getValue(PrayerType.ASR), adjusted.getValue(PrayerType.ASR))
        assertEquals(base.getValue(PrayerType.MAGHRIB), adjusted.getValue(PrayerType.MAGHRIB))
        assertEquals(base.getValue(PrayerType.ISHA), adjusted.getValue(PrayerType.ISHA))
    }

    @Test
    fun `Oslo in high summer still returns all five prayer times`() {
        // 59.9N, well above the 48 degree threshold where AUTOMATIC switches to
        // seventh-of-the-night bounding. Not golden-compared — see the class doc.
        val times = calculator.calculate(date, 59.9139, 10.7522, prefs(CalculationMethod.MWL.id), ZoneId.of("Europe/Oslo"))
        requireNotNull(times) { "Oslo: calculator returned null" }
        assertEquals(setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA), times.keys)
    }
}
