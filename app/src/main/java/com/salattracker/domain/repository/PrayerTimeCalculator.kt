package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Calculates prayer times on the device from coordinates and the user's calculation
 * preferences — no network call, no caching needed (pure math, microseconds per call).
 * Implemented by `data/prayertimes/AdhanPrayerTimeCalculator`, which wraps the `adhan2`
 * library; this interface itself has no library imports, matching [LocationProvider] and
 * [MosqueGeofencer]'s domain-interface/data-impl split.
 */
interface PrayerTimeCalculator {

    /**
     * Returns null when [latitude]/[longitude] are both 0.0 — [UserPrefs]'s unset-location
     * default — so every caller keeps the exact "times unavailable" handling it already had
     * for a failed network call (no location yet is the only way this can fail now).
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        prefs: UserPrefs,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<PrayerType, LocalTime>?

    /** Sunrise is needed to close Fajr's valid prayer window. */
    fun calculateSunrise(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        prefs: UserPrefs,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalTime?
}
