package com.salattracker.data.prayertimes

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerTimeCalculator
import com.salattracker.domain.repository.UserPrefs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule

/**
 * [PrayerTimeCalculator] backed by the `adhan2` library — replaces the former AlAdhan network
 * call and its Room cache entirely. Pure math (a handful of trig calls), so there's nothing to
 * cache: every call recomputes from scratch, which is cheap enough for Home's once-a-minute
 * ticker and every other caller.
 */
@Singleton
class AdhanPrayerTimeCalculator @Inject constructor(
    private val parametersFactory: CalculationParametersFactory,
) : PrayerTimeCalculator {

    override fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        prefs: UserPrefs,
        zone: ZoneId,
    ): Map<PrayerType, LocalTime>? {
        // (0.0, 0.0) is UserPrefs' unset-location default. Treating it as "unavailable" (rather
        // than computing real times for the Gulf of Guinea) matches what every caller already
        // did for a failed network call — the only way this can now return null.
        if (latitude == 0.0 && longitude == 0.0) return null

        val method = CalculationMethod.fromId(prefs.calculationMethodId)
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)

        val setup = parametersFactory.create(
            method = method,
            madhab = prefs.madhab,
            highLatitudeRule = prefs.highLatitudeRule,
            date = date,
        )
        val parameters = setup.parameters.let {
            // AUTOMATIC resolves to null from the factory (it has no coordinates to decide
            // with) — resolve it here against the real location.
            if (it.highLatitudeRule != null) it
            else it.copy(highLatitudeRule = AdhanHighLatitudeRule.recommendedFor(coordinates))
        }

        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
        val maghrib = setup.maghribAngle?.let { angle ->
            maghribViaIshaAngle(coordinates, dateComponents, parameters, angle)
        } ?: prayerTimes.maghrib

        // Ja'fari has no adhan2 equivalent: Asr is prayed with Dhuhr ("Zuhrain") and Isha with
        // Maghrib ("Maghribain"), so both are simply set equal to the prayer they're combined
        // with — see the Madhab.JAFARI class doc.
        val raw = mapOf(
            PrayerType.FAJR to prayerTimes.fajr,
            PrayerType.DHUHR to prayerTimes.dhuhr,
            PrayerType.ASR to if (prefs.madhab == Madhab.JAFARI) prayerTimes.dhuhr else prayerTimes.asr,
            PrayerType.MAGHRIB to maghrib,
            PrayerType.ISHA to if (prefs.madhab == Madhab.JAFARI) maghrib else prayerTimes.isha,
        )

        return raw.mapValues { (type, instant) ->
            instant.toLocalTime(zone).plusMinutes((prefs.prayerTimeAdjustMinutes[type] ?: 0).toLong())
        }
    }

    override fun calculateSunrise(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        prefs: UserPrefs,
        zone: ZoneId,
    ): LocalTime? {
        if (latitude == 0.0 && longitude == 0.0) return null

        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = parametersFactory.create(
            method = CalculationMethod.fromId(prefs.calculationMethodId),
            madhab = prefs.madhab,
            highLatitudeRule = prefs.highLatitudeRule,
            date = date,
        ).parameters.let {
            if (it.highLatitudeRule != null) it
            else it.copy(highLatitudeRule = AdhanHighLatitudeRule.recommendedFor(coordinates))
        }
        return PrayerTimes(coordinates, dateComponents, parameters).sunrise.toLocalTime(zone)
    }

    /** Tehran/Shia Ithna-Ashari define Maghrib as [angle] degrees of solar depression below the
     * horizon, which `adhan2` has no direct field for. A second run with `ishaAngle = angle`
     * gives exactly that moment as its own Isha — see [CalculationSetup.maghribAngle]. Only the
     * angle changes; the high-latitude rule carries over so the same bounding applies. */
    private fun maghribViaIshaAngle(
        coordinates: Coordinates,
        dateComponents: DateComponents,
        parameters: CalculationParameters,
        angle: Double,
    ): kotlin.time.Instant {
        val maghribParams = CalculationParameters(
            ishaAngle = angle,
            method = AdhanMethod.OTHER,
            highLatitudeRule = parameters.highLatitudeRule,
        )
        return PrayerTimes(coordinates, dateComponents, maghribParams).isha
    }

    private fun kotlin.time.Instant.toLocalTime(zone: ZoneId): LocalTime =
        Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone).toLocalTime()
}
