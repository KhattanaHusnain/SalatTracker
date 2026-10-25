package com.salattracker.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.repository.GeocodingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the platform [Geocoder] (Play Services / the device's own geocoding provider — no
 * separate network dependency or API key). Uses the classic synchronous API across all supported
 * API levels rather than API 33's listener-based overload, since both end up doing the same
 * blocking lookup and the synchronous form needs no version branch.
 */
@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : GeocodingRepository {

    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }

    override suspend fun searchCities(query: String): List<CityResult> {
        if (query.isBlank() || !Geocoder.isPresent()) return emptyList()
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocationName(query, MAX_RESULTS) }
                .getOrNull()
                .orEmpty()
                .mapNotNull { it.toCityResult() }
                .distinctBy { it.name to it.region to it.country }
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): CityResult? {
        if (!Geocoder.isPresent()) return null
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(latitude, longitude, 1) }
                .getOrNull()
                ?.firstOrNull()
                ?.toCityResult()
                // The reverse lookup should describe the exact fix, not a re-centred city match.
                ?.copy(latitude = latitude, longitude = longitude)
        }
    }

    private fun Address.toCityResult(): CityResult? {
        val name = locality ?: subAdminArea ?: adminArea ?: featureName ?: return null
        return CityResult(
            name      = name,
            region    = adminArea?.takeIf { it != name },
            country   = countryName,
            latitude  = latitude,
            longitude = longitude,
            countryCode = countryCode,
        )
    }

    companion object {
        private const val MAX_RESULTS = 5
    }
}
