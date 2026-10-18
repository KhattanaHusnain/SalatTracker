package com.salattracker.domain.model

data class Mosque(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = MosqueMode.DEFAULT_RADIUS_METERS,
)

data class NearbyMosque(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
)

enum class RingerMode { NORMAL, VIBRATE, SILENT }

/**
 * [insideIds] are the saved mosques the phone is currently in. [restoreTo] is set only when this
 * app itself changed the ringer, so we never "restore" something the user set.
 */
data class MosqueModeState(
    val enabled: Boolean = false,
    val insideIds: Set<Long> = emptySet(),
    val restoreTo: RingerMode? = null,
    val activeMosqueName: String? = null,
)

enum class GeofenceSync { OK, MISSING_PERMISSION, LOCATION_OFF, FAILED }

/** What location access the app currently holds; geofences need [FULL] ("allow all the time"). */
enum class LocationAccess { UNKNOWN, NONE, FOREGROUND_ONLY, FULL }

object MosqueMode {
    val RADIUS_OPTIONS_METERS = listOf(50, 100, 150, 200)
    const val DEFAULT_RADIUS_METERS = 100
    /** Play services allows 100 geofences per app. */
    const val MAX_MOSQUES = 100
    const val DWELL_DELAY_MS = 60_000
    const val NEARBY_RADIUS_METERS = 2000
    /** A nearby result this close to a saved mosque counts as already added. */
    const val ALREADY_ADDED_METERS = 50
}
