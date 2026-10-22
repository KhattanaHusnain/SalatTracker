package com.salattracker.domain.model

/** One city/place match from [com.salattracker.domain.repository.GeocodingRepository]. */
data class CityResult(
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Karachi, Sindh, Pakistan" — drops any part that's blank or missing. */
    val displayLine: String get() = listOfNotNull(region, country).joinToString(", ")
}
