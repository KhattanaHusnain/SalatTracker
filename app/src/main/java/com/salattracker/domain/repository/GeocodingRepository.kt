package com.salattracker.domain.repository

import com.salattracker.domain.model.CityResult

/**
 * City name <-> coordinates lookups for onboarding's "Enter city instead" / Settings' location
 * search. Backed by the platform's own [android.location.Geocoder] (data/location/
 * GeocodingRepositoryImpl) — no separate API key or network dependency beyond what the device's
 * geocoding service already needs.
 */
interface GeocodingRepository {

    /** Forward geocoding: free-text city/place name to candidate matches. Empty list on no
     * matches, a geocoder service error, or a query that's too short to search. */
    suspend fun searchCities(query: String): List<CityResult>

    /** Reverse geocoding: coordinates (typically a GPS fix) to the place they're in. Null if the
     * service has no match or is unavailable. */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): CityResult?
}
