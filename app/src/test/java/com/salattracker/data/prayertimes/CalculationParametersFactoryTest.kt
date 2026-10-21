package com.salattracker.data.prayertimes

import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.Madhab as AdhanMadhab

class CalculationParametersFactoryTest {

    private val factory = CalculationParametersFactory()
    private val notRamadan: LocalDate = LocalDate.of(2026, 7, 15)

    @Test
    fun `every library-preset method yields its adhan2 preset's angles`() {
        val presets = mapOf(
            CalculationMethod.MWL to Pair(18.0, 17.0),
            CalculationMethod.ISNA to Pair(15.0, 15.0),
            CalculationMethod.EGYPT to Pair(19.5, 17.5),
            CalculationMethod.KARACHI to Pair(18.0, 18.0),
            CalculationMethod.KUWAIT to Pair(18.0, 17.5),
            CalculationMethod.SINGAPORE to Pair(20.0, 18.0),
            CalculationMethod.TURKEY to Pair(18.0, 17.0),
            CalculationMethod.MOONSIGHTING_COMMITTEE to Pair(18.0, 18.0),
            CalculationMethod.DUBAI to Pair(18.2, 18.2),
        )
        presets.forEach { (method, angles) ->
            val setup = factory.create(method, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
            assertEquals("$method fajr angle", angles.first, setup.parameters.fajrAngle, 0.01)
            assertEquals("$method isha angle", angles.second, setup.parameters.ishaAngle, 0.01)
        }
    }

    @Test
    fun `methods with an isha interval instead of an angle`() {
        val intervals = mapOf(
            CalculationMethod.UMM_AL_QURA to 90,
            CalculationMethod.QATAR to 90,
            CalculationMethod.GULF_REGION to 90,
            CalculationMethod.LISBON to 77,
        )
        intervals.forEach { (method, minutes) ->
            val setup = factory.create(method, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
            assertEquals("$method isha interval", minutes, setup.parameters.ishaInterval)
        }
    }

    @Test
    fun `custom-angle methods not covered by an adhan2 preset`() {
        val customAngles = mapOf(
            CalculationMethod.GULF_REGION to Pair(19.5, null),
            CalculationMethod.FRANCE to Pair(12.0, 12.0),
            CalculationMethod.RUSSIA to Pair(16.0, 15.0),
            CalculationMethod.JAKIM_MALAYSIA to Pair(20.0, 18.0),
            CalculationMethod.TUNISIA to Pair(18.0, 18.0),
            CalculationMethod.ALGERIA to Pair(18.0, 17.0),
            CalculationMethod.INDONESIA to Pair(20.0, 18.0),
            CalculationMethod.MOROCCO to Pair(19.0, 17.0),
            CalculationMethod.LISBON to Pair(18.0, null),
            CalculationMethod.JORDAN to Pair(18.0, 18.0),
        )
        customAngles.forEach { (method, angles) ->
            val setup = factory.create(method, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
            assertEquals("$method fajr angle", angles.first, setup.parameters.fajrAngle, 0.01)
            if (angles.second != null) {
                assertEquals("$method isha angle", angles.second!!, setup.parameters.ishaAngle, 0.01)
            }
        }
    }

    @Test
    fun `Lisbon and Jordan add a Maghrib method adjustment in minutes`() {
        val lisbon = factory.create(CalculationMethod.LISBON, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(3, lisbon.parameters.methodAdjustments.maghrib)

        val jordan = factory.create(CalculationMethod.JORDAN, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(5, jordan.parameters.methodAdjustments.maghrib)
    }

    @Test
    fun `Tehran and Shia Ithna-Ashari expose a Maghrib angle instead of adhan2's own Maghrib`() {
        val tehran = factory.create(CalculationMethod.TEHRAN, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(17.7, tehran.parameters.fajrAngle, 0.01)
        assertEquals(14.0, tehran.parameters.ishaAngle, 0.01)
        assertEquals(4.5, tehran.maghribAngle!!, 0.01)

        val shia = factory.create(CalculationMethod.SHIA_ITHNA_ASHARI, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(16.0, shia.parameters.fajrAngle, 0.01)
        assertEquals(14.0, shia.parameters.ishaAngle, 0.01)
        assertEquals(4.0, shia.maghribAngle!!, 0.01)
    }

    @Test
    fun `every other method has no Maghrib angle override`() {
        CalculationMethod.values()
            .filter { it != CalculationMethod.TEHRAN && it != CalculationMethod.SHIA_ITHNA_ASHARI }
            .forEach { method ->
                val setup = factory.create(method, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
                assertNull("$method should not override Maghrib", setup.maghribAngle)
            }
    }

    @Test
    fun `Hanafi maps to adhan2's HANAFI madhab, standard maps to SHAFI`() {
        val hanafi = factory.create(CalculationMethod.MWL, Madhab.HANAFI, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(AdhanMadhab.HANAFI, hanafi.parameters.madhab)

        val standard = factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(AdhanMadhab.SHAFI, standard.parameters.madhab)
    }

    @Test
    fun `Jafari falls into adhan2's SHAFI madhab here, since the real Jafari behavior (Asr equals Dhuhr, Isha equals Maghrib) is applied one layer up in AdhanPrayerTimeCalculator`() {
        val jafari = factory.create(CalculationMethod.MWL, Madhab.JAFARI, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(AdhanMadhab.SHAFI, jafari.parameters.madhab)
    }

    @Test
    fun `AUTOMATIC leaves the high-latitude rule unset for the caller to resolve, others map directly`() {
        val auto = factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertNull(auto.parameters.highLatitudeRule)

        assertEquals(
            AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.MIDDLE_OF_THE_NIGHT, notRamadan).parameters.highLatitudeRule,
        )
        assertEquals(
            AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT,
            factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.SEVENTH_OF_THE_NIGHT, notRamadan).parameters.highLatitudeRule,
        )
        assertEquals(
            AdhanHighLatitudeRule.TWILIGHT_ANGLE,
            factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.TWILIGHT_ANGLE, notRamadan).parameters.highLatitudeRule,
        )
    }

    @Test
    fun `Umm al-Qura adds 30 minutes to Isha only during Ramadan, and only for that method`() {
        val ramadanDate = LocalDate.from(HijrahDate.now().with(ChronoField.MONTH_OF_YEAR, 9).with(ChronoField.DAY_OF_MONTH, 15))

        val ummAlQuraRamadan = factory.create(CalculationMethod.UMM_AL_QURA, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, ramadanDate)
        val ummAlQuraOutsideRamadan = factory.create(CalculationMethod.UMM_AL_QURA, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(30, ummAlQuraRamadan.parameters.methodAdjustments.isha - ummAlQuraOutsideRamadan.parameters.methodAdjustments.isha)

        // A method that also uses ishaInterval (not ishaAngle) must stay unaffected by the rule.
        val mwlRamadan = factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, ramadanDate)
        val mwlOutsideRamadan = factory.create(CalculationMethod.MWL, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan)
        assertEquals(mwlOutsideRamadan.parameters.methodAdjustments.isha, mwlRamadan.parameters.methodAdjustments.isha)
    }

    @Test
    fun `all 23 methods produce a non-null setup`() {
        CalculationMethod.values().forEach { method ->
            assertNotNull(factory.create(method, Madhab.STANDARD, HighLatitudeRule.AUTOMATIC, notRamadan))
        }
    }
}
