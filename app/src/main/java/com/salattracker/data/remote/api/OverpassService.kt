package com.salattracker.data.remote.api

import com.salattracker.domain.model.MosqueMode
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.usecase.distanceMeters
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.Locale
import kotlin.math.roundToInt

// Every field is nullable/defaulted: Gson bypasses constructors, and OSM elements vary
// (nodes carry lat/lon, ways and relations carry a `center`).
data class OverpassResponse(val elements: List<OverpassElement>? = null)

data class OverpassElement(
    val type: String? = null,
    val id: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null,
)

data class OverpassCenter(val lat: Double? = null, val lon: Double? = null)

interface OverpassService {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") query: String): OverpassResponse
}

/** Coordinates are rounded to 3 decimals (about 100 m) so the exact position never leaves the device. */
fun buildNearbyMosquesQuery(latitude: Double, longitude: Double): String {
    val lat = String.format(Locale.US, "%.3f", latitude)
    val lon = String.format(Locale.US, "%.3f", longitude)
    return "[out:json][timeout:25];" +
        "nwr[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:${MosqueMode.NEARBY_RADIUS_METERS},$lat,$lon);" +
        "out center tags;"
}

/** Drops elements without coordinates, sorts nearest first, and collapses a mosque mapped as both a node and a building. */
fun OverpassResponse.toNearbyMosques(originLat: Double, originLon: Double): List<NearbyMosque> {
    val all = elements.orEmpty().mapNotNull { element ->
        val lat = element.lat ?: element.center?.lat ?: return@mapNotNull null
        val lon = element.lon ?: element.center?.lon ?: return@mapNotNull null
        val name = (element.tags?.get("name") ?: element.tags?.get("name:en"))?.trim()?.takeIf { it.isNotEmpty() }
        NearbyMosque(name, lat, lon, distanceMeters(originLat, originLon, lat, lon).roundToInt())
    }.sortedBy { it.distanceMeters }

    val kept = mutableListOf<NearbyMosque>()
    for (candidate in all) {
        val duplicate = candidate.name != null && kept.any {
            it.name.equals(candidate.name, ignoreCase = true) &&
                distanceMeters(it.latitude, it.longitude, candidate.latitude, candidate.longitude) < MosqueMode.ALREADY_ADDED_METERS
        }
        if (!duplicate) kept += candidate
    }
    return kept
}
