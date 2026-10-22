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

    /** "Karachi, Sindh, Pakistan" including [name] — the single-string form persisted as
     * [com.salattracker.domain.repository.UserPrefs.locationLabel]. */
    val fullLabel: String get() = listOfNotNull(name.takeIf { it.isNotBlank() }, displayLine.takeIf { it.isNotBlank() }).joinToString(", ")
}
