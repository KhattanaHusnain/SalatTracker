package com.salattracker.data.mosque

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueMode
import com.salattracker.domain.repository.MosqueGeofencer
import com.salattracker.notification.MosqueModeReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MosqueGeofencerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: GeofencingClient,
) : MosqueGeofencer {

    // Play services fills in the transition details, so the intent must be mutable on API 31+.
    // The explicit component keeps it from being hijacked.
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, MosqueModeReceiver::class.java).setAction(MosqueModeReceiver.ACTION_GEOFENCE)
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
    }

    // Tracks what's currently registered with Play services *in this process*. register() is
    // called far more often than the mosque list actually changes — app launch, every permission
    // re-check, the daily periodic sync, boot — and a naive remove-then-add on every call tears
    // down and rebuilds the geofence each time. That resets Play services' internal loitering
    // clock to zero, so if register() ever runs more often than the loitering delay, DWELL can
    // never accumulate enough continuous time to fire at all. Skipping the no-op case fixes that.
    // In-memory only is correct: a fresh process should always register once regardless, since the
    // OS may have dropped the geofences (reboot, location toggled off) without us finding out.
    private var lastRegistered: List<Mosque>? = null

    @SuppressLint("MissingPermission")
    override suspend fun register(mosques: List<Mosque>): GeofenceSync {
        if (!hasPermission()) {
            Log.w(TAG, "register: missing permission (fine=${granted(Manifest.permission.ACCESS_FINE_LOCATION)}, " +
                "background=${Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)})")
            return GeofenceSync.MISSING_PERMISSION
        }
        if (mosques == lastRegistered) {
            Log.d(TAG, "register: unchanged, skipping (${mosques.size} geofence(s))")
            return GeofenceSync.OK
        }
        return try {
            client.removeGeofences(pendingIntent).await()
            val geofences = mosques.take(MosqueMode.MAX_MOSQUES).map {
                Geofence.Builder()
                    .setRequestId(it.id.toString())
                    .setCircularRegion(it.latitude, it.longitude, it.radiusMeters.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay(MosqueMode.DWELL_DELAY_MS)
                    .build()
            }
            // DWELL|EXIT initial triggers make Play services report EXIT right away for mosques
            // we're outside of, which ends a visit whose real exit we missed (reboot, location off).
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(geofences)
                .build()
            client.addGeofences(request, pendingIntent).await()
            lastRegistered = mosques
            Log.i(TAG, "register: OK, ${geofences.size} geofence(s): " +
                geofences.joinToString { "#${it.requestId}" })
            GeofenceSync.OK
        } catch (e: SecurityException) {
            lastRegistered = null
            Log.w(TAG, "register: SecurityException", e)
            GeofenceSync.MISSING_PERMISSION
        } catch (e: ApiException) {
            lastRegistered = null
            Log.w(TAG, "register: ApiException statusCode=${e.statusCode} (${GeofenceStatusCodes.getStatusCodeString(e.statusCode)})")
            when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> GeofenceSync.LOCATION_OFF
                GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> GeofenceSync.MISSING_PERMISSION
                else -> GeofenceSync.FAILED
            }
        } catch (e: Exception) {
            lastRegistered = null
            Log.e(TAG, "register: failed", e)
            GeofenceSync.FAILED
        }
    }

    override suspend fun clear() {
        lastRegistered = null
        try {
            client.removeGeofences(pendingIntent).await()
            Log.i(TAG, "clear: OK")
        } catch (e: Exception) {
            // Nothing registered, or Play services unavailable — either way there's nothing to clear.
        }
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    private fun hasPermission(): Boolean {
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        return granted(Manifest.permission.ACCESS_FINE_LOCATION) && background
    }

    private companion object {
        const val TAG = "MosqueMode"
        const val REQUEST_CODE = 8100
    }
}
