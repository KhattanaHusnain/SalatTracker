package com.salattracker.domain.repository

/**
 * Reads the device's current location. An interface (no Android imports — Hilt binds the real
 * FusedLocationProviderClient-backed implementation in data/location/) so ViewModels can be
 * unit-tested with a fake instead of needing a real Google Play Services client.
 *
 * Caller must have already confirmed ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION is granted —
 * this does not request permission itself.
 */
interface LocationProvider {
    /** Null if a fix couldn't be obtained (permission revoked mid-call, location off, timeout). */
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}
