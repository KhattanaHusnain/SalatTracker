package com.salattracker.data.prayertimes

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.PrayerAdjustments
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.usecase.toHijriOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.Madhab as AdhanMadhab

/**
 * The on-device parameters to run for one day's calculation.
 *
 * [maghribAngle], when non-null, means [parameters]'s own Maghrib isn't usable for this method —
 * Tehran and Shia Ithna-Ashari define Maghrib as a sun-depth angle below the horizon rather than
 * "at sunset", and `adhan2`'s [CalculationParameters] has no Maghrib-angle field. The caller
 * (see `AdhanPrayerTimeCalculator`) runs a *second* `PrayerTimes` with `ishaAngle` set to this
 * value and takes *its* Isha as the real Maghrib — that's exactly the moment the sun reaches
 * that depth after sunset, so it's an exact calculation, not an approximation.
 */
data class CalculationSetup(
    val parameters: CalculationParameters,
    val maghribAngle: Double? = null,
)

private const val RAMADAN_HIJRI_MONTH = 9

/**
 * Maps this app's 23 stored [CalculationMethod]s, plus the user's [Madhab] and
 * [HighLatitudeRule], onto `adhan2`'s [CalculationParameters]. Pure — no I/O — so it's unit-
 * tested directly against AlAdhan's own published reference values.
 *
 * Deliberately does **not** apply the user's per-prayer minute corrections
 * ([com.salattracker.domain.repository.UserPrefs.prayerTimeAdjustMinutes]) — those are plain
 * clock-time arithmetic applied once, uniformly, after every prayer time is computed (including
 * the Maghrib-by-angle special case below), in `AdhanPrayerTimeCalculator`. Baking them into
 * `adhan2`'s own `prayerAdjustments` field instead would need the Maghrib-by-angle second run to
 * read the *maghrib* adjustment out of the slot it reads as *isha*, which is exactly the kind of
 * mismatch worth avoiding by keeping this factory's output purely astronomical.
 *
 * Angle/interval values for the 12 methods `adhan2` has no built-in preset for come from
 * AlAdhan's own https://api.aladhan.com/v1/methods (fetched 2026) — re-verify there before
 * changing any of them.
 */
@Singleton
class CalculationParametersFactory @Inject constructor() {

    fun create(
        method: CalculationMethod,
        madhab: Madhab,
        highLatitudeRule: HighLatitudeRule,
        date: LocalDate,
    ): CalculationSetup {
        val base = baseParametersFor(method)
        val maghribAngle = maghribAngleFor(method)

        var params = base.copy(
            madhab = if (madhab == Madhab.HANAFI) AdhanMadhab.HANAFI else AdhanMadhab.SHAFI,
            highLatitudeRule = highLatitudeRule.toAdhanOrNull(),
        )

        // Umm al-Qura's published note: add 30 minutes to Isha during Ramadan.
        if (method == CalculationMethod.UMM_AL_QURA && date.toHijriOrNull()?.month == RAMADAN_HIJRI_MONTH) {
            params = params.copy(methodAdjustments = params.methodAdjustments.copy(isha = params.methodAdjustments.isha + 30))
        }

        return CalculationSetup(params, maghribAngle)
    }

    /** [HighLatitudeRule.AUTOMATIC] maps to null: the caller resolves it per-location via
     * `AdhanHighLatitudeRule.recommendedFor(coordinates)`, since the right rule depends on
     * latitude and this factory doesn't see coordinates. */
    private fun HighLatitudeRule.toAdhanOrNull(): AdhanHighLatitudeRule? = when (this) {
        HighLatitudeRule.AUTOMATIC -> null
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
    }

    private fun baseParametersFor(method: CalculationMethod): CalculationParameters = when (method) {
        // adhan2 presets (11).
        CalculationMethod.MWL -> AdhanMethod.MUSLIM_WORLD_LEAGUE.parameters
        CalculationMethod.ISNA -> AdhanMethod.NORTH_AMERICA.parameters
        CalculationMethod.EGYPT -> AdhanMethod.EGYPTIAN.parameters
        CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA.parameters
        CalculationMethod.KARACHI -> AdhanMethod.KARACHI.parameters
        CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT.parameters
        CalculationMethod.QATAR -> AdhanMethod.QATAR.parameters
        CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE.parameters
        CalculationMethod.TURKEY -> AdhanMethod.TURKEY.parameters
        CalculationMethod.MOONSIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE.parameters
        CalculationMethod.DUBAI -> AdhanMethod.DUBAI.parameters

        // Custom angles — no adhan2 preset exists for these (10).
        CalculationMethod.GULF_REGION -> CalculationParameters(fajrAngle = 19.5, ishaInterval = 90, method = AdhanMethod.OTHER)
        CalculationMethod.FRANCE -> CalculationParameters(fajrAngle = 12.0, ishaAngle = 12.0, method = AdhanMethod.OTHER)
        CalculationMethod.RUSSIA -> CalculationParameters(fajrAngle = 16.0, ishaAngle = 15.0, method = AdhanMethod.OTHER)
        CalculationMethod.JAKIM_MALAYSIA -> CalculationParameters(fajrAngle = 20.0, ishaAngle = 18.0, method = AdhanMethod.OTHER)
        CalculationMethod.TUNISIA -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 18.0, method = AdhanMethod.OTHER)
        CalculationMethod.ALGERIA -> CalculationParameters(fajrAngle = 18.0, ishaAngle = 17.0, method = AdhanMethod.OTHER)
        CalculationMethod.INDONESIA -> CalculationParameters(fajrAngle = 20.0, ishaAngle = 18.0, method = AdhanMethod.OTHER)
        CalculationMethod.MOROCCO -> CalculationParameters(fajrAngle = 19.0, ishaAngle = 17.0, method = AdhanMethod.OTHER)
        CalculationMethod.LISBON -> CalculationParameters(
            fajrAngle = 18.0, ishaInterval = 77, method = AdhanMethod.OTHER,
            methodAdjustments = PrayerAdjustments(maghrib = 3),
        )
        CalculationMethod.JORDAN -> CalculationParameters(
            fajrAngle = 18.0, ishaAngle = 18.0, method = AdhanMethod.OTHER,
            methodAdjustments = PrayerAdjustments(maghrib = 5),
        )

        // Maghrib-by-angle methods (2) — Fajr/Isha angles are exact here; Maghrib is computed
        // separately, see maghribAngleFor.
        CalculationMethod.TEHRAN -> CalculationParameters(fajrAngle = 17.7, ishaAngle = 14.0, method = AdhanMethod.OTHER)
        CalculationMethod.SHIA_ITHNA_ASHARI -> CalculationParameters(fajrAngle = 16.0, ishaAngle = 14.0, method = AdhanMethod.OTHER)
    }

    private fun maghribAngleFor(method: CalculationMethod): Double? = when (method) {
        CalculationMethod.TEHRAN -> 4.5
        CalculationMethod.SHIA_ITHNA_ASHARI -> 4.0
        else -> null
    }
}
