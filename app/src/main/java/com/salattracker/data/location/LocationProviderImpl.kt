package com.salattracker.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.salattracker.domain.repository.LocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationProviderImpl @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(highAccuracy: Boolean): Pair<Double, Double>? {
        val cancellationTokenSource = CancellationTokenSource()
        val location = try {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.getCurrentLocation(
                    if (highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token,
                ).addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { cont.resume(null) }
                    .addOnCanceledListener { cont.resume(null) }
                cont.invokeOnCancellation { cancellationTokenSource.cancel() }
            }
        } catch (e: SecurityException) {
            null
        }
        return location?.let { it.latitude to it.longitude }
    }
}
